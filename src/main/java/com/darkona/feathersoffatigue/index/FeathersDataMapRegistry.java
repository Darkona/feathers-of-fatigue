package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.MountStats;
import com.darkona.feathersoffatigue.api.registry.FeathersDataMaps;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FeathersDataMapRegistry {
    public static final ResourceLocation ARMOR_WEIGHT_FILE = file(FeathersDataMaps.ARMOR_WEIGHT, "item");
    public static final ResourceLocation MOUNT_STATS_FILE = file(FeathersDataMaps.MOUNT_STATS, "entity_type");

    private static final String[] CONDITION_KEYS = {"forge:conditions", "neoforge:conditions"};

    public record ArmorEntry(String key, int weight) {}

    public record MountEntry(String key, MountStats stats) {}

    public record Raw(List<ArmorEntry> armor, List<MountEntry> mounts) {
        public static final Raw EMPTY = new Raw(List.of(), List.of());
    }

    private static volatile Resolved current = new Resolved(Raw.EMPTY);

    private FeathersDataMapRegistry() {}

    @SuppressWarnings("removal")
    private static ResourceLocation file(ResourceLocation map, String registry) {
        return new ResourceLocation(map.getNamespace(), "data_maps/" + registry + "/" + map.getPath() + ".json");
    }

    public static Raw raw() {
        return current.raw;
    }

    public static int armorWeight(Item item) {
        return current.armorWeight(item);
    }

    public static @Nullable MountStats mountStats(EntityType<?> type) {
        return current.mountStats(type);
    }

    public static void onTagsChanged() {
        current.clearCaches();
    }

    public static void accept(Raw raw) {
        current = new Resolved(raw);
        ArmorWeights.invalidate();
        FeathersServiceImpl.invalidateMountTypes();
    }

    public static final class Loader extends SimplePreparableReloadListener<Raw> {
        private final ICondition.IContext conditions;

        public Loader(ICondition.IContext conditions) {
            this.conditions = conditions;
        }

        @Override
        protected Raw prepare(ResourceManager manager, ProfilerFiller profiler) {
            List<ArmorEntry> armor = new ArrayList<>();
            load(manager, ARMOR_WEIGHT_FILE).forEach((key, value) -> {
                JsonElement weight = value.isJsonObject() && value.getAsJsonObject().has("value") ? value.getAsJsonObject().get("value") : value;
                try {
                    armor.add(new ArmorEntry(key, Math.max(0, weight.getAsInt())));
                } catch (RuntimeException e) {
                    Feathers.LOGGER.warn("Armor weight entry '{}' isn't a whole number, ignored.", key);
                }
            });
            List<MountEntry> mounts = new ArrayList<>();
            load(manager, MOUNT_STATS_FILE).forEach((key, value) -> {
                JsonElement stats = value.isJsonObject() && value.getAsJsonObject().has("value") ? value.getAsJsonObject().get("value") : value;
                Optional<MountStats> parsed = MountStats.CODEC.parse(JsonOps.INSTANCE, stats)
                        .resultOrPartial(error -> Feathers.LOGGER.warn("Mount stats entry '{}': {}", key, error));
                parsed.ifPresent(s -> mounts.add(new MountEntry(key, s)));
            });
            return new Raw(armor, mounts);
        }

        @Override
        protected void apply(Raw raw, ResourceManager manager, ProfilerFiller profiler) {
            accept(raw);
        }

        private Map<String, JsonElement> load(ResourceManager manager, ResourceLocation file) {
            Map<String, JsonElement> merged = new LinkedHashMap<>();
            for (Resource resource : manager.getResourceStack(file)) {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (GsonHelper.getAsBoolean(json, "replace", false)) merged.clear();
                    JsonObject values = GsonHelper.getAsJsonObject(json, "values", new JsonObject());
                    for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                        try {
                            if (conditionsMet(entry.getValue())) merged.put(entry.getKey(), entry.getValue());
                        } catch (RuntimeException e) {
                            Feathers.LOGGER.warn("Entry '{}' of {} in {} has invalid conditions, ignored: {}", entry.getKey(), file,
                                    resource.sourcePackId(), e.getMessage());
                        }
                    }
                    if (json.has("remove")) {
                        for (JsonElement removed : GsonHelper.getAsJsonArray(json, "remove")) merged.remove(removed.getAsString());
                    }
                } catch (Exception e) {
                    Feathers.LOGGER.error("Couldn't read {} from {}", file, resource.sourcePackId(), e);
                }
            }
            return merged;
        }

        private boolean conditionsMet(JsonElement value) {
            if (!value.isJsonObject()) return true;
            JsonObject object = value.getAsJsonObject();
            for (String key : CONDITION_KEYS) {
                if (!object.has(key)) continue;
                JsonArray forge = new JsonArray();
                for (JsonElement condition : GsonHelper.getAsJsonArray(object, key)) {
                    JsonObject copy = condition.getAsJsonObject().deepCopy();
                    String type = GsonHelper.getAsString(copy, "type", "");
                    if (type.startsWith("neoforge:")) copy.addProperty("type", "forge:" + type.substring("neoforge:".length()));
                    forge.add(copy);
                }
                if (!CraftingHelper.processConditions(forge, conditions)) return false;
            }
            return true;
        }
    }

    private static final class Resolved {
        private static final Object NO_ENTRY = new Object();

        final Raw raw;
        private final Reference2IntOpenHashMap<Item> armorItems = new Reference2IntOpenHashMap<>();
        private final List<TagKey<Item>> armorTags = new ArrayList<>();
        private final IntArrayList armorTagWeights = new IntArrayList();
        private final Reference2ObjectOpenHashMap<EntityType<?>, MountStats> mountTypes = new Reference2ObjectOpenHashMap<>();
        private final List<TagKey<EntityType<?>>> mountTags = new ArrayList<>();
        private final List<MountStats> mountTagStats = new ArrayList<>();

        private volatile Object[] mountCache = new Object[0];

        Resolved(Raw raw) {
            this.raw = raw;
            armorItems.defaultReturnValue(-1);
            for (ArmorEntry entry : raw.armor()) {
                if (entry.key().startsWith("#")) {
                    ResourceLocation tag = ResourceLocation.tryParse(entry.key().substring(1));
                    if (tag == null) continue;
                    armorTags.add(TagKey.create(Registries.ITEM, tag));
                    armorTagWeights.add(entry.weight());
                } else {
                    ResourceLocation id = ResourceLocation.tryParse(entry.key());
                    if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> armorItems.put(item, entry.weight()));
                }
            }
            for (MountEntry entry : raw.mounts()) {
                if (entry.key().startsWith("#")) {
                    ResourceLocation tag = ResourceLocation.tryParse(entry.key().substring(1));
                    if (tag == null) continue;
                    mountTags.add(TagKey.create(Registries.ENTITY_TYPE, tag));
                    mountTagStats.add(entry.stats());
                } else {
                    ResourceLocation id = ResourceLocation.tryParse(entry.key());
                    if (id != null) BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> mountTypes.put(type, entry.stats()));
                }
            }
        }

        @SuppressWarnings("deprecation")
        int armorWeight(Item item) {
            int weight = armorItems.getInt(item);
            if (weight >= 0) return weight;
            for (int i = armorTags.size() - 1; i >= 0; i--) {
                if (item.builtInRegistryHolder().is(armorTags.get(i))) return armorTagWeights.getInt(i);
            }
            return -1;
        }

        @Nullable MountStats mountStats(EntityType<?> type) {
            int id = BuiltInRegistries.ENTITY_TYPE.getId(type);
            Object[] cache = mountCache;
            if (cache.length == 0) {
                cache = new Object[BuiltInRegistries.ENTITY_TYPE.size()];
                mountCache = cache;
            }
            Object cached = id >= 0 && id < cache.length ? cache[id] : null;
            if (cached == null) {
                MountStats stats = resolveMount(type);
                cached = stats != null ? stats : NO_ENTRY;

                if (id >= 0 && id < cache.length) cache[id] = cached;
            }
            return cached == NO_ENTRY ? null : (MountStats) cached;
        }

        private @Nullable MountStats resolveMount(EntityType<?> type) {
            MountStats stats = mountTypes.get(type);
            if (stats != null) return stats;
            for (int i = mountTags.size() - 1; i >= 0; i--) {
                if (type.is(mountTags.get(i))) return mountTagStats.get(i);
            }
            return null;
        }

        void clearCaches() {
            mountCache = new Object[0];
        }
    }
}
