package com.darkona.feathersoffatigue.content.weight;

import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.WeightSource;
import com.darkona.feathersoffatigue.api.event.ArmorWeightEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersEnchantments;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.Extensions;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ArmorWeights {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int UNRESOLVED = -1;

    private record TagRule(TagKey<Item> tag, int weight) {}

    private static final Reference2IntOpenHashMap<Item> itemRules = new Reference2IntOpenHashMap<>();
    private static final List<TagRule> tagRules = new ArrayList<>();
    private static final Object2IntOpenHashMap<String> materialPieceRules = new Object2IntOpenHashMap<>();
    private static final Object2IntOpenHashMap<ResourceLocation> materialRules = new Object2IntOpenHashMap<>();
    private static final Reference2IntOpenHashMap<Item> baseWeightCache = new Reference2IntOpenHashMap<>();
    private static boolean rulesLoaded;

    static {
        itemRules.defaultReturnValue(UNRESOLVED);
        materialPieceRules.defaultReturnValue(UNRESOLVED);
        materialRules.defaultReturnValue(UNRESOLVED);
        baseWeightCache.defaultReturnValue(UNRESOLVED);
    }

    private ArmorWeights() {}

    public static synchronized void invalidate() {
        rulesLoaded = false;
        baseWeightCache.clear();
    }

    private static void loadRules() {
        if (rulesLoaded) return;
        itemRules.clear();
        tagRules.clear();
        materialPieceRules.clear();
        materialRules.clear();

        for (String rule : ServerConfig.ARMOR_WEIGHTS.get()) {
            int eq = rule.lastIndexOf('=');
            if (eq <= 0) {
                Feathers.LOGGER.warn("Armor weight rule '{}' has no '=weight' part, ignored.", rule);
                continue;
            }
            String target = rule.substring(0, eq).trim();
            if (target.isEmpty()) {
                Feathers.LOGGER.warn("Armor weight rule '{}' has nothing before '=', ignored.", rule);
                continue;
            }
            int weight;
            try {
                weight = Math.max(0, Integer.parseInt(rule.substring(eq + 1).trim()));
            } catch (NumberFormatException e) {
                Feathers.LOGGER.warn("Armor weight rule '{}' has a weight that isn't a whole number, ignored.", rule);
                continue;
            }

            switch (target.charAt(0)) {
                case '#' -> {
                    ResourceLocation id = ResourceLocation.tryParse(target.substring(1));
                    if (id != null) tagRules.add(new TagRule(TagKey.create(Registries.ITEM, id), weight));
                    else Feathers.LOGGER.warn("Armor weight rule '{}' has an invalid tag, ignored.", rule);
                }
                case '@' -> {
                    String material = target.substring(1);
                    int slash = material.indexOf('/');
                    ResourceLocation id = ResourceLocation.tryParse(slash < 0 ? material : material.substring(0, slash));
                    if (id == null) Feathers.LOGGER.warn("Armor weight rule '{}' has an invalid material, ignored.", rule);
                    else if (slash < 0) materialRules.put(id, weight);
                    else materialPieceRules.put(id + "/" + material.substring(slash + 1), weight);
                }
                default -> {
                    ResourceLocation id = ResourceLocation.tryParse(target);
                    if (id != null && BuiltInRegistries.ITEM.containsKey(id)) itemRules.put(BuiltInRegistries.ITEM.get(id), weight);
                    else Feathers.LOGGER.warn("Armor weight rule '{}' names an unknown item, ignored.", rule);
                }
            }
        }
        rulesLoaded = true;
    }

    public static synchronized int baseWeight(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        int cached = baseWeightCache.getInt(item);
        if (cached != UNRESOLVED) return cached;

        int weight = resolveBaseWeight(stack);
        baseWeightCache.put(item, weight);
        return weight;
    }

    private static int resolveBaseWeight(ItemStack stack) {
        loadRules();
        Item item = stack.getItem();

        int weight = itemRules.getInt(item);
        if (weight != UNRESOLVED) return weight;

        for (TagRule rule : tagRules) {
            if (stack.is(rule.tag())) return rule.weight();
        }

        int mapped = FeathersDataMapRegistry.armorWeight(item);
        if (mapped >= 0) return mapped;

        if (item instanceof ArmorItem armor) {
            weight = materialWeight(ResourceLocation.tryParse(armor.getMaterial().getName()), armor.getType().getName());
            if (weight != UNRESOLVED) return weight;
            return (int) Math.round(armor.getDefense() * ServerConfig.UNLISTED_ARMOR_WEIGHT_PER_DEFENSE.get());
        }
        if (item instanceof HorseArmorItem horseArmor) {
            weight = materialWeight(horseArmorMaterial(horseArmor), "body");
            if (weight != UNRESOLVED) return weight;
            return (int) Math.round(horseArmor.getProtection() * ServerConfig.UNLISTED_ARMOR_WEIGHT_PER_DEFENSE.get());
        }
        return 0;
    }

    private static int materialWeight(@Nullable ResourceLocation material, String piece) {
        if (material == null) return UNRESOLVED;
        int weight = materialPieceRules.getInt(material + "/" + piece);
        return weight != UNRESOLVED ? weight : materialRules.getInt(material);
    }

    private static final String HORSE_ARMOR_TEXTURE = "textures/entity/horse/armor/horse_armor_";

    private static @Nullable ResourceLocation horseArmorMaterial(HorseArmorItem armor) {
        ResourceLocation texture = armor.getTexture();
        String path = texture.getPath();
        if (!path.startsWith(HORSE_ARMOR_TEXTURE) || !path.endsWith(".png")) return null;
        return ResourceLocation.tryBuild(texture.getNamespace(), path.substring(HORSE_ARMOR_TEXTURE.length(), path.length() - ".png".length()));
    }

    public static double pieceWeight(ItemStack stack) {
        int base = baseWeight(stack);
        if (base == 0) return 0;
        EnchantmentLevels levels = enchantmentLevels(stack);
        double lightness = Math.max(0.0, 1.0 - levels.lightweight() * ServerConfig.LIGHTWEIGHT_REDUCTION_PER_LEVEL.get());
        return base * lightness * (1 + levels.heavy());
    }

    private record EnchantmentLevels(@Nullable ListTag enchantments, int size, int lightweight, int heavy) {}

    private static final EnchantmentLevels NO_LEVELS = new EnchantmentLevels(null, 0, 0, 0);

    private static volatile EnchantmentLevels lastLevels = NO_LEVELS;

    private static EnchantmentLevels enchantmentLevels(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEnchanted()) return NO_LEVELS;
        ListTag enchantments = stack.getEnchantmentTags();
        EnchantmentLevels levels = lastLevels;
        if (levels.enchantments() == enchantments && levels.size() == enchantments.size()) return levels;
        int lightweight = 0;
        int heavy = 0;
        for (int i = 0; i < enchantments.size(); i++) {
            CompoundTag entry = enchantments.getCompound(i);
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(entry);
            if (FeathersEnchantments.LIGHTWEIGHT.getId().equals(id)) lightweight = EnchantmentHelper.getEnchantmentLevel(entry);
            else if (FeathersEnchantments.HEAVY.getId().equals(id)) heavy = EnchantmentHelper.getEnchantmentLevel(entry);
        }
        levels = new EnchantmentLevels(enchantments, enchantments.size(), lightweight, heavy);
        lastLevels = levels;
        return levels;
    }

    public static final int HEAD = 0;

    public static final int CHEST = 1;

    public static final int LEGS = 2;

    public static final int FEET = 3;

    public static final int OTHER = 4;

    public static final int PARTS = 5;

    @SuppressWarnings("unused")
    public static int totalWeight(LivingEntity entity) {
        return totalWeight(entity, null);
    }

    public static int totalWeight(LivingEntity entity, @Nullable WeightSplit split) {
        if (split != null) split.clear();
        if (!ServerConfig.ENABLE_ARMOR_WEIGHTS.get()) return 0;

        double head = pieceWeight(entity.getItemBySlot(EquipmentSlot.HEAD));

        double chest = pieceWeight(entity.getItemBySlot(EquipmentSlot.CHEST));
        double legs = pieceWeight(entity.getItemBySlot(EquipmentSlot.LEGS));
        double feet = pieceWeight(entity.getItemBySlot(EquipmentSlot.FEET));
        double raw = head + chest + legs + feet;
        double other = 0;
        if (split != null) {
            split.shares.add(head);
            split.shares.add(chest);
            split.shares.add(legs);
            split.shares.add(feet);
            split.shares.add(0);
        }
        for (Extensions.WeightEntry entry : Extensions.weightSources()) {
            double weight = entry.source().weight(entity);
            if (weight <= 0) continue;
            raw += weight;
            int tint = split != null ? tintOf(entry.source(), entity) : NO_TINT;
            if (tint == NO_TINT) {
                other += weight;
            } else {
                split.shares.add(weight);
                split.sources.add(tint);
                split.sources.add(0);
            }
        }
        if (split != null) split.shares.set(OTHER, other);

        ArmorWeightEvent event = new ArmorWeightEvent(entity, raw);
        MinecraftForge.EVENT_BUS.post(event);
        double weight = event.getWeight();

        AttributeInstance multiplier = entity.getAttribute(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER.get());
        if (multiplier != null) weight *= multiplier.getValue();

        int total = Math.max(0, (int) Math.round(weight));
        if (split != null && total > 0) {
            if (raw <= 0) {
                split.parts[OTHER] = total;
            } else {
                round(split, total, weight / raw);
            }
        }
        return total;
    }

    private static final int NO_TINT = Integer.MIN_VALUE;

    private static int tintOf(WeightSource source, LivingEntity entity) {
        int color = source.color(entity);
        if (color != WeightSource.NO_COLOR) return color & 0xFFFFFF;
        Item item = source.displayItem(entity);
        if (item == null || item == Items.AIR) return NO_TINT;
        return -(BuiltInRegistries.ITEM.getId(item) + 1);
    }

    private static void round(WeightSplit split, int total, double scale) {
        int count = split.shares.size();
        int assigned = 0;
        for (int i = 0; i < count; i++) {
            int floor = (int) Math.floor(split.shares.getDouble(i) * scale);
            setFeathers(split, i, floor);
            assigned += floor;
        }
        while (assigned < total) {
            int best = 0;
            double bestRemainder = -1;
            for (int i = 0; i < count; i++) {
                double remainder = split.shares.getDouble(i) * scale - feathers(split, i);
                if (remainder > bestRemainder) {
                    bestRemainder = remainder;
                    best = i;
                }
            }
            setFeathers(split, best, feathers(split, best) + 1);
            assigned++;
        }
        for (int i = count - 1; i >= 0 && assigned > total; i--) {
            int feathers = feathers(split, i);
            int taken = Math.min(feathers, assigned - total);
            setFeathers(split, i, feathers - taken);
            assigned -= taken;
        }
    }

    private static int feathers(WeightSplit split, int i) {
        return i < PARTS ? split.parts[i] : split.sources.getInt(2 * (i - PARTS) + 1);
    }

    private static void setFeathers(WeightSplit split, int i, int feathers) {
        if (i < PARTS) split.parts[i] = feathers;
        else split.sources.set(2 * (i - PARTS) + 1, feathers);
    }
}
