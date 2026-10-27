package com.darkona.feathersoffatigue.content.weight;

import dev.darkona.jdlib.core.registry.PriorityList;
import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.WeightSource;
import com.darkona.feathersoffatigue.api.event.ArmorWeightEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersDataMaps;
import com.darkona.feathersoffatigue.api.registry.FeathersEnchantments;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.content.feathers.Extensions;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.NeoForge;
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

        for (String rule : FeathersServerConfig.ARMOR_WEIGHTS.get()) {
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

        Integer mapped = stack.getItemHolder().getData(FeathersDataMaps.ARMOR_WEIGHT);
        if (mapped != null) return mapped;

        if (item instanceof ArmorItem armor) {
            ResourceLocation material = armor.getMaterial().unwrapKey().map(ResourceKey::location).orElse(null);
            if (material != null) {
                weight = materialPieceRules.getInt(material + "/" + armor.getType().getName());
                if (weight != UNRESOLVED) return weight;
                weight = materialRules.getInt(material);
                if (weight != UNRESOLVED) return weight;
            }
            return (int) Math.round(armor.getDefense() * FeathersServerConfig.UNLISTED_ARMOR_WEIGHT_PER_DEFENSE.get());
        }
        return 0;
    }

    public static double pieceWeight(ItemStack stack) {
        int base = baseWeight(stack);
        if (base == 0) return 0;
        EnchantmentLevels levels = enchantmentLevels(stack.getTagEnchantments());
        double lightness = Math.max(0.0, 1.0 - levels.lightweight() * FeathersServerConfig.LIGHTWEIGHT_REDUCTION_PER_LEVEL.get());
        return base * lightness * (1 + levels.heavy());
    }

    private record EnchantmentLevels(ItemEnchantments enchantments, int lightweight, int heavy) {}

    private static final EnchantmentLevels NO_LEVELS = new EnchantmentLevels(ItemEnchantments.EMPTY, 0, 0);
    private static volatile EnchantmentLevels lastLevels = NO_LEVELS;

    private static EnchantmentLevels enchantmentLevels(ItemEnchantments enchantments) {
        if (enchantments.isEmpty()) return NO_LEVELS;
        EnchantmentLevels levels = lastLevels;
        if (levels.enchantments() == enchantments) return levels;
        int lightweight = 0;
        int heavy = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchantments.entrySet()) {
            if (e.getKey().is(FeathersEnchantments.LIGHTWEIGHT)) lightweight = e.getIntValue();
            else if (e.getKey().is(FeathersEnchantments.HEAVY)) heavy = e.getIntValue();
        }
        levels = new EnchantmentLevels(enchantments, lightweight, heavy);
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
        if (!FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.get()) return 0;

        double head = pieceWeight(entity.getItemBySlot(EquipmentSlot.HEAD));
        double chest = pieceWeight(entity.getItemBySlot(EquipmentSlot.CHEST));
        if (!(entity instanceof Player)) chest += pieceWeight(entity.getItemBySlot(EquipmentSlot.BODY));
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
        PriorityList.Snapshot<ResourceLocation, WeightSource> sources = Extensions.weightSources();
        for (int i = 0; i < sources.size(); i++) {
            WeightSource source = sources.value(i);
            double weight = source.weight(entity);
            if (weight <= 0) continue;
            raw += weight;
            int tint = split != null ? tintOf(source, entity) : NO_TINT;
            if (tint == NO_TINT) {
                other += weight;
            } else {
                split.shares.add(weight);
                split.sources.add(tint);
                split.sources.add(0);
            }
        }
        if (split != null) split.shares.set(OTHER, other);

        double weight = NeoForge.EVENT_BUS.post(new ArmorWeightEvent(entity, raw)).getWeight();

        AttributeInstance multiplier = entity.getAttribute(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER);
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
