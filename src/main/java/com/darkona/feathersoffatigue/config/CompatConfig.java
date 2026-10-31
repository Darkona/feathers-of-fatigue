package com.darkona.feathersoffatigue.config;

import com.darkona.feathersoffatigue.config.CompatConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class CompatConfig {

    public static final ModConfigSpec SPEC;

    public static final BooleanValue COLD_SWEAT;
    public static final BooleanValue COLD_SWEAT_COLD;
    public static final BooleanValue COLD_SWEAT_HEAT;
    public static final IntValue COLD_SWEAT_COLD_THRESHOLD;
    public static final IntValue COLD_SWEAT_HOT_THRESHOLD;
    public static final IntValue COLD_SWEAT_SEVERE_THRESHOLD;

    public static final BooleanValue THIRST;
    public static final DoubleValue THIRST_REGEN_REDUCTION;
    public static final DoubleValue QUENCH_REGEN_BONUS;
    public static final DoubleValue THIRST_PER_FEATHER;

    public static final BooleanValue DROPLETS_OF_THIRST;
    public static final DoubleValue DROPLETS_OF_THIRST_REGEN_REDUCTION;
    public static final DoubleValue DROPLETS_OF_THIRST_QUENCH_BONUS;
    public static final DoubleValue DROPLETS_OF_THIRST_THIRST_PER_FEATHER;

    public static final BooleanValue TAN;
    public static final BooleanValue TAN_TEMPERATURE;
    public static final BooleanValue TAN_COLD_IS_COLD;
    public static final BooleanValue TAN_WARM_IS_HOT;
    public static final DoubleValue TAN_SEVERE_HYPERTHERMIA;
    public static final BooleanValue TAN_THIRST;
    public static final DoubleValue TAN_THIRST_REDUCTION;
    public static final DoubleValue TAN_HYDRATION_BONUS;
    public static final DoubleValue TAN_THIRST_EXHAUSTION;

    public static final BooleanValue LSO;
    public static final BooleanValue LSO_TEMPERATURE;
    public static final BooleanValue LSO_THIRST;
    public static final DoubleValue LSO_THIRST_REDUCTION;
    public static final DoubleValue LSO_HYDRATION_BONUS;
    public static final DoubleValue LSO_THIRST_EXHAUSTION;

    public static final BooleanValue SEASONS;
    public static final BooleanValue SEASONS_WINTER_COLD;
    public static final DoubleValue SEASONS_WINTER_MAX_TEMPERATURE;
    public static final BooleanValue SEASONS_SUMMER_HEAT;
    public static final DoubleValue SEASONS_SUMMER_MIN_TEMPERATURE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Cold Sweat: body temperature decides Cold, Heat and Fatigue instead of biomes.").push("cold_sweat");
        COLD_SWEAT = builder.comment("Use Cold Sweat when it is installed.")
                .define("enabled", true);
        COLD_SWEAT_COLD = builder.comment("Low body temperature applies Cold.")
                .define("cold_from_body_temperature", true);
        COLD_SWEAT_HEAT = builder.comment("High body temperature applies Heat, and severe heat Fatigue.")
                .define("heat_from_body_temperature", true);
        COLD_SWEAT_COLD_THRESHOLD = builder.comment("Body temperature at or below which Cold applies. Body temperature spans -150 to 150;",
                        "Cold Sweat's own freezing starts around -45.")
                .defineInRange("cold_threshold", -50, -150, 150);
        COLD_SWEAT_HOT_THRESHOLD = builder.comment("Body temperature at or above which Heat applies. Overheating starts around 45.")
                .defineInRange("hot_threshold", 50, -150, 150);
        COLD_SWEAT_SEVERE_THRESHOLD = builder.comment("Body temperature at or above which the heat is severe and Fatigue applies too.")
                .defineInRange("severe_hot_threshold", 100, -150, 150);
        builder.pop();

        builder.comment("Thirst Was Taken: thirst slows regeneration, being quenched speeds it up.").push("thirst_was_taken");
        THIRST = builder.comment("Use Thirst Was Taken when it is installed.")
                .define("enabled", true);
        THIRST_REGEN_REDUCTION = builder.comment("Feathers per second lost per missing thirst point (20 points = full).")
                .defineInRange("regen_reduction_per_thirst_point", 0.02, 0.0, 20.0);
        QUENCH_REGEN_BONUS = builder.comment("Feathers per second gained per point of quench (thirst saturation).")
                .defineInRange("regen_bonus_per_quench_point", 0.02, 0.0, 20.0);
        THIRST_PER_FEATHER = builder.comment("Thirst points each regenerated feather costs. 0 = regenerating costs no thirst.")
                .defineInRange("thirst_per_regenerated_feather", 0.0, 0.0, 20.0);
        builder.pop();

        builder.comment("Droplets of Thirst (the continuation of Thirst Was Taken): thirst slows regeneration, being quenched speeds it up.",
                "Ignored for players whose thirst is off.").push("droplets_of_thirst");
        DROPLETS_OF_THIRST = builder.comment("Use Droplets of Thirst when it is installed.")
                .define("enabled", true);
        DROPLETS_OF_THIRST_REGEN_REDUCTION = builder.comment("Feathers per second lost per missing thirst point (20 points = full).")
                .defineInRange("regen_reduction_per_thirst_point", 0.02, 0.0, 20.0);
        DROPLETS_OF_THIRST_QUENCH_BONUS = builder.comment("Feathers per second gained per point of quenched (thirst saturation).")
                .defineInRange("regen_bonus_per_quench_point", 0.02, 0.0, 20.0);
        DROPLETS_OF_THIRST_THIRST_PER_FEATHER = builder.comment("Extra thirst points each regenerated feather costs.",
                        "Droplets of Thirst already drains thirst with the food spent on regeneration, unless its mode is OWN.")
                .defineInRange("thirst_per_regenerated_feather", 0.0, 0.0, 20.0);
        builder.pop();

        builder.comment("Tough As Nails: its temperature drives Cold, Heat and Fatigue; its thirst drives regeneration.").push("tough_as_nails");
        TAN = builder.comment("Use Tough As Nails when it is installed.")
                .define("enabled", true);
        TAN_TEMPERATURE = builder.comment("Use its temperature (when its temperature is on).")
                .define("temperature", true);
        TAN_COLD_IS_COLD = builder.comment("COLD applies the Cold effect; off: only ICY does.")
                .define("cold_level_applies_cold", true);
        TAN_WARM_IS_HOT = builder.comment("WARM applies Heat too; off: only HOT does.")
                .define("warm_level_applies_heat", false);
        TAN_SEVERE_HYPERTHERMIA = builder.comment("Hyperthermia progress (0 to 1) at which the heat is severe and Fatigue applies.")
                .defineInRange("severe_hyperthermia", 0.5, 0.0, 1.0);
        TAN_THIRST = builder.comment("Use its thirst (when its thirst is on).")
                .define("thirst", true);
        TAN_THIRST_REDUCTION = builder.comment("Feathers per second lost per missing thirst point (20 = full).")
                .defineInRange("regen_reduction_per_thirst_point", 0.02, 0.0, 20.0);
        TAN_HYDRATION_BONUS = builder.comment("Feathers per second gained per point of hydration.")
                .defineInRange("regen_bonus_per_hydration_point", 0.02, 0.0, 20.0);
        TAN_THIRST_EXHAUSTION = builder.comment("Thirst exhaustion per regenerated feather (4.0 = one thirst point). 0 = free.")
                .defineInRange("thirst_exhaustion_per_regenerated_feather", 0.0, 0.0, 40.0);
        builder.pop();

        builder.comment("Legendary Survival Overhaul: body temperature drives Cold, Heat and Fatigue; hydration drives regeneration.")
                .push("legendary_survival_overhaul");
        LSO = builder.comment("Use Legendary Survival Overhaul when it is installed.")
                .define("enabled", true);
        LSO_TEMPERATURE = builder.comment("Use its body temperature.")
                .define("temperature", true);
        LSO_THIRST = builder.comment("Use its hydration.")
                .define("thirst", true);
        LSO_THIRST_REDUCTION = builder.comment("Feathers per second lost per missing hydration point (20 = full).")
                .defineInRange("regen_reduction_per_thirst_point", 0.02, 0.0, 20.0);
        LSO_HYDRATION_BONUS = builder.comment("Feathers per second gained per point of hydration saturation.")
                .defineInRange("regen_bonus_per_saturation_point", 0.02, 0.0, 20.0);
        LSO_THIRST_EXHAUSTION = builder.comment("Thirst exhaustion per regenerated feather. 0 = free.")
                .defineInRange("thirst_exhaustion_per_regenerated_feather", 0.0, 0.0, 40.0);
        builder.pop();

        builder.comment("Serene Seasons: winter outdoors is cold, a summer day in the sun is hot. Ignored while a body-temperature",
                "mod (Cold Sweat, Tough As Nails, Legendary Survival Overhaul) is in charge: they already count seasons.").push("serene_seasons");
        SEASONS = builder.comment("Use Serene Seasons when it is installed.")
                .define("enabled", true);
        SEASONS_WINTER_COLD = builder.comment("Being outdoors in winter applies Cold.")
                .define("winter_cold", true);
        SEASONS_WINTER_MAX_TEMPERATURE = builder.comment("Only in biomes cooler than this (deserts and jungles stay warm). Plains are 0.8.")
                .defineInRange("winter_cold_below_temperature", 1.0, -2.0, 4.0);
        SEASONS_SUMMER_HEAT = builder.comment("A summer day under the sun applies Heat in warm biomes.")
                .define("summer_heat", true);
        SEASONS_SUMMER_MIN_TEMPERATURE = builder.comment("Biome temperature from which summer sun applies Heat (lower than the normal hot_temperature).")
                .defineInRange("summer_heat_from_temperature", 0.8, -2.0, 4.0);
        builder.pop();

        SPEC = builder.build();
    }

    private CompatConfig() {}
}
