package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.ClimateProvider;
import com.darkona.feathersoffatigue.api.RegenFactor;
import com.darkona.feathersoffatigue.api.StaminaModifier;
import com.darkona.feathersoffatigue.api.WeightSource;
import dev.darkona.jdlib.core.registry.PriorityList;
import dev.darkona.jdlib.core.registry.PriorityList.Snapshot;
import net.minecraft.resources.ResourceLocation;

public final class Extensions {

    private static final PriorityList<ResourceLocation, ClimateProvider> CLIMATES =
            new PriorityList<>(PriorityList.Order.HIGHEST_FIRST, PriorityList.Ties.REGISTRATION);
    private static final PriorityList<ResourceLocation, RegenFactor> REGEN_FACTORS =
            new PriorityList<>(PriorityList.Order.LOWEST_FIRST, PriorityList.Ties.REGISTRATION);
    private static final PriorityList<ResourceLocation, WeightSource> WEIGHT_SOURCES =
            new PriorityList<>(PriorityList.Order.LOWEST_FIRST, PriorityList.Ties.REGISTRATION);
    private static final PriorityList<ResourceLocation, StaminaModifier> MODIFIERS =
            new PriorityList<>(PriorityList.Order.LOWEST_FIRST, PriorityList.Ties.REGISTRATION);

    private Extensions() {}

    public static Snapshot<ResourceLocation, ClimateProvider> climates() {
        return CLIMATES.snapshot();
    }

    public static Snapshot<ResourceLocation, RegenFactor> regenFactors() {
        return REGEN_FACTORS.snapshot();
    }

    public static Snapshot<ResourceLocation, WeightSource> weightSources() {
        return WEIGHT_SOURCES.snapshot();
    }

    public static Snapshot<ResourceLocation, StaminaModifier> modifiers() {
        return MODIFIERS.snapshot();
    }

    public static void addClimate(ResourceLocation id, int priority, ClimateProvider provider) {
        CLIMATES.put(id, priority, provider);
    }

    public static void addRegenFactor(ResourceLocation id, RegenFactor factor) {
        REGEN_FACTORS.put(id, 0, factor);
    }

    public static void addWeightSource(ResourceLocation id, WeightSource source) {
        WEIGHT_SOURCES.put(id, 0, source);
    }

    public static void addModifier(ResourceLocation id, int ordinal, StaminaModifier modifier) {
        MODIFIERS.put(id, ordinal, modifier);
    }
}
