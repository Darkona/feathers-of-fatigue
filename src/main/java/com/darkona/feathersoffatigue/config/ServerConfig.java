package com.darkona.feathersoffatigue.config;

import com.darkona.feathersoffatigue.config.ServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

import java.util.List;

public final class ServerConfig {

    public static final ModConfigSpec SPEC;

    public static final IntValue MAX_FEATHERS;
    public static final DoubleValue REGEN_FEATHERS_PER_SECOND;
    public static final BooleanValue SLEEPING_ALWAYS_RESTORES_FEATHERS;
    public static final IntValue DEFAULT_USAGE_COOLDOWN;
    public static final IntValue MAX_COOLDOWN;
    public static final BooleanValue REGEN_USES_HUNGER;
    public static final DoubleValue HUNGER_PER_FEATHER;
    public static final DoubleValue SATURATION_REGEN_BONUS;
    public static final DoubleValue HUNGER_REGEN_PENALTY;

    public static final BooleanValue ENABLE_STRAIN;
    public static final IntValue MAX_STRAIN;
    public static final BooleanValue ENABLE_EXHAUSTION;
    public static final DoubleValue EXHAUSTION_RECOVERY;

    public static final BooleanValue ENABLE_COLD;
    public static final DoubleValue COLD_TEMPERATURE;
    public static final BooleanValue ENABLE_HEAT;
    public static final DoubleValue HOT_TEMPERATURE;
    public static final BooleanValue ENABLE_FATIGUE;
    public static final BooleanValue FATIGUE_FROM_NETHER;
    public static final BooleanValue FATIGUE_FROM_BURNING;
    public static final IntValue EFFECT_LINGER;
    public static final BooleanValue ENABLE_ENDURANCE;
    public static final BooleanValue ENABLE_MOMENTUM;
    public static final BooleanValue ENABLE_ENERGIZED;
    public static final BooleanValue ENABLE_COOLING;

    public static final BooleanValue ENABLE_REST;
    public static final IntValue REST_STILL_TICKS;
    public static final DoubleValue REST_STILL_MULTIPLIER;
    public static final DoubleValue REST_CROUCHING_MULTIPLIER;
    public static final DoubleValue REST_SITTING_MULTIPLIER;
    public static final BooleanValue REST_BOOSTS_REGEN;

    public static final BooleanValue ENABLE_ARMOR_WEIGHTS;
    public static final ConfigValue<List<? extends String>> ARMOR_WEIGHTS;
    public static final DoubleValue UNLISTED_ARMOR_WEIGHT_PER_DEFENSE;
    public static final DoubleValue LIGHTWEIGHT_REDUCTION_PER_LEVEL;

    public static final BooleanValue ENABLE_BASIC_EXERTION;
    public static final DoubleValue SPRINT_FEATHERS_PER_SECOND;
    public static final DoubleValue JUMP_FEATHERS;

    public static final BooleanValue ENABLE_MOUNTS;
    public static final IntValue MOUNT_MIN_FEATHERS;
    public static final IntValue MOUNT_MAX_FEATHERS;
    public static final DoubleValue MOUNT_REGEN;
    public static final DoubleValue MOUNT_GALLOP_FEATHERS_PER_SECOND;
    public static final DoubleValue MOUNT_GALLOP_SPEED;
    public static final DoubleValue MOUNT_JUMP_FEATHERS;
    public static final DoubleValue MOUNT_EXHAUSTED_SLOWDOWN;

    public static final BooleanValue DEBUG_MODE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");

        MAX_FEATHERS = builder
                .comment("Feathers a player has. Two feathers make one icon, like hearts: 20 is a full row.",
                        "This is the base of the feathers_of_fatigue:max_feathers attribute; effects and items modify it.")
                .defineInRange("max_feathers", 20, 0, 1000);

        REGEN_FEATHERS_PER_SECOND = builder
                .comment("Feathers regenerated per second. Base of the feathers_of_fatigue:feathers_per_second attribute;",
                        "Cold halves it, Energized doubles it. Any value works, however small: fractions carry over.")
                .defineInRange("regen_feathers_per_second", 0.4, -40.0, 40.0);

        SLEEPING_ALWAYS_RESTORES_FEATHERS = builder
                .comment("Sleeping through the night restores all feathers and clears strain and exhaustion.")
                .define("sleeping_restores_all_feathers", true);

        DEFAULT_USAGE_COOLDOWN = builder
                .comment("Ticks without regeneration after spending feathers, for spends that don't set their own.")
                .defineInRange("default_usage_cooldown_ticks", 30, 0, 1200);

        MAX_COOLDOWN = builder
                .comment("Cooldowns from consecutive spends add up to at most this many seconds.")
                .defineInRange("max_cooldown_seconds", 5, 0, 600);

        REGEN_USES_HUNGER = builder
                .comment("Regenerating feathers costs food, like healing: no regeneration at 6 hunger points or less.")
                .define("regen_uses_hunger", true);

        HUNGER_PER_FEATHER = builder
                .comment("Food exhaustion per regenerated feather when regen_uses_hunger is on. 4.0 exhaustion = one hunger point.")
                .defineInRange("hunger_exhaustion_per_feather", 0.3, 0.0, 40.0);

        SATURATION_REGEN_BONUS = builder
                .comment("Regeneration multiplier at maximum saturation: 1.5 regenerates 50% faster.",
                        "It grows in a straight line with saturation, on top of the food bar. 1.0 turns it off.")
                .defineInRange("saturation_regen_bonus", 1.5, 1.0, 10.0);

        HUNGER_REGEN_PENALTY = builder
                .comment("Regeneration multiplier at 6 hunger points or less: 0.5 regenerates at half speed, 0.0 stops it.",
                        "It rises in a straight line to normal speed at a full food bar. 1.0 turns it off.",
                        "With regen_uses_hunger on, regeneration stops at 6 hunger points or less anyway.")
                .defineInRange("hunger_regen_penalty", 0.5, 0.0, 1.0);

        builder.pop();

        builder.push("exhaustion_and_strain");

        ENABLE_STRAIN = builder
                .comment("Strain: when feathers run out, keep exerting by overspending into red 'negative' feathers, up to",
                        "max_strained_feathers. Regeneration pays the strain back first, slowly; resting speeds it up.",
                        "Mods can still ask for a spend that never strains.")
                .define("strain_enabled", true);

        MAX_STRAIN = builder
                .comment("How far into strain a player can go, in feathers. Base of the feathers_of_fatigue:max_strain attribute.")
                .defineInRange("max_strained_feathers", 6, 1, 1000);

        ENABLE_EXHAUSTION = builder
                .comment("Exhaustion: once a player has nothing left to spend (no feathers, and no strain room when strain is",
                        "on), they are exhausted and can't exert again until they recover exhaustion_recovery of the bar.",
                        "Off: they can spend again as soon as anything regenerates.")
                .define("exhaustion_enabled", true);

        EXHAUSTION_RECOVERY = builder
                .comment("Share of the maximum feathers to regain, with no strain left, before exhaustion ends.")
                .defineInRange("exhaustion_recovery", 0.3, 0.0, 1.0);

        builder.pop();

        builder.push("effects");

        ENABLE_COLD = builder
                .comment("Cold effect: halves regeneration (level II stops it). Applied in cold, snowy or freezing places.")
                .define("effect_cold_enabled", true);

        COLD_TEMPERATURE = builder
                .comment("Biome temperature below which rain or snow applies Cold. Cold biomes go from 0.05 down to -0.7.")
                .defineInRange("cold_temperature", -0.3, -2.0, 2.0);

        ENABLE_HEAT = builder
                .comment("Heat effect: doubles costs. First heat tier: a hot biome under the sun, or Cold Sweat's hot_threshold.",
                        "Fire Resistance and the Cooling effect prevent it.")
                .define("effect_hot_enabled", true);

        HOT_TEMPERATURE = builder
                .comment("Biome temperature from which being under the sun applies Heat. The hottest biomes reach 2.0.")
                .defineInRange("hot_temperature", 1.8, -2.0, 4.0);

        ENABLE_FATIGUE = builder
                .comment("Fatigue effect: 4 fewer max feathers per level. Second heat tier, on top of Heat: the Nether, burning,",
                        "lava, or Cold Sweat's severe_hot_threshold. Other mods may apply it too. Fire Resistance and Cooling prevent it.")
                .define("effect_fatigue_enabled", true);

        FATIGUE_FROM_NETHER = builder
                .comment("Being in the Nether counts as severe heat.")
                .define("fatigue_from_nether", true);

        FATIGUE_FROM_BURNING = builder
                .comment("Being on fire or in lava counts as severe heat.")
                .define("fatigue_from_burning", true);

        EFFECT_LINGER = builder
                .comment("Ticks that Cold, Heat and Fatigue last after leaving what caused them. 0 disables lingering.")
                .defineInRange("effect_lingering_ticks", 60, 0, 12000);

        ENABLE_ENDURANCE = builder
                .comment("Endurance effect: golden feathers spent before regular ones; the effect ends when they run out.")
                .define("effect_endurance_enabled", true);

        ENABLE_MOMENTUM = builder
                .comment("Momentum effect: halves costs.")
                .define("effect_momentum_enabled", true);

        ENABLE_ENERGIZED = builder
                .comment("Energized effect: doubles regeneration.")
                .define("effect_energized_enabled", true);

        ENABLE_COOLING = builder
                .comment("Cooling effect: protects from Heat and Fatigue.")
                .define("effect_cooling_enabled", true);

        builder.pop();

        builder.push("resting");

        ENABLE_REST = builder
                .comment("Resting speeds up paying back strain: standing still, crouching still, or sitting (riding a boat,",
                        "a mount, or a seat from another mod). Mods can add rest bonuses through the API; the best one applies.")
                .define("rest_enabled", true);

        REST_STILL_TICKS = builder
                .comment("Ticks without moving before standing still counts as resting. 20 ticks = 1 second.")
                .defineInRange("rest_still_ticks", 40, 1, 1200);

        REST_STILL_MULTIPLIER = builder
                .comment("Strain recovery multiplier while standing still.")
                .defineInRange("rest_still_multiplier", 1.5, 1.0, 20.0);

        REST_CROUCHING_MULTIPLIER = builder
                .comment("Strain recovery multiplier while crouching still.")
                .defineInRange("rest_crouching_multiplier", 2.5, 1.0, 20.0);

        REST_SITTING_MULTIPLIER = builder
                .comment("Strain recovery multiplier while sitting.")
                .defineInRange("rest_sitting_multiplier", 2.0, 1.0, 20.0);

        REST_BOOSTS_REGEN = builder
                .comment("Resting also speeds up normal regeneration, not only strain recovery.")
                .define("rest_boosts_regen", false);

        builder.pop();

        builder.push("armor_weights");

        ENABLE_ARMOR_WEIGHTS = builder
                .comment("Worn armor has weight: each point makes one feather unusable (shown grey on the HUD).")
                .define("armor_weights_enabled", false);

        ARMOR_WEIGHTS = builder
                .comment("Weight rules, as 'target=weight'. The most specific matching rule wins:",
                        "  minecraft:iron_chestplate=3      one item",
                        "  #mymod:heavy_armor=6            every item in an item tag",
                        "  (the feathers_of_fatigue:armor_weight data map, which mods and datapacks can ship, comes here)",
                        "  @minecraft:iron/chestplate=3    one piece of an armor material (helmet, chestplate, leggings, boots, body for horse armor)",
                        "  @minecraft:iron=2               every piece of an armor material",
                        "Armor that matches nothing weighs its defense points times unlisted_armor_weight_per_defense.")
                .defineListAllowEmpty("armor_weights", List.of(
                        "@minecraft:leather=1",
                        "@minecraft:chainmail=1",
                        "@minecraft:turtle=1",
                        "@minecraft:gold=2",
                        "@minecraft:iron=2",
                        "@minecraft:diamond=3",
                        "@minecraft:netherite=4",
                        "@minecraft:leather/body=1",
                        "@minecraft:gold/body=1",
                        "@minecraft:iron/body=2",
                        "@minecraft:diamond/body=2"), () -> "", o -> o instanceof String);

        UNLISTED_ARMOR_WEIGHT_PER_DEFENSE = builder
                .comment("Weight of armor no rule or data map covers, per point of defense. 0 makes it weightless.")
                .defineInRange("unlisted_armor_weight_per_defense", 0.5, 0.0, 10.0);

        LIGHTWEIGHT_REDUCTION_PER_LEVEL = builder
                .comment("Share of a piece's weight each level of Lightweight removes: 0.25 leaves 25% at level III.")
                .defineInRange("lightweight_reduction_per_level", 0.25, 0.0, 1.0);

        builder.pop();

        builder.push("basic_exertion");

        ENABLE_BASIC_EXERTION = builder
                .comment("Sprinting and jumping cost feathers, so Feathers of Fatigue does something on its own.",
                        "Always off when another mod takes over player actions through the API.")
                .define("basic_exertion_enabled", true);

        SPRINT_FEATHERS_PER_SECOND = builder
                .comment("Feathers per second while sprinting. Regeneration pauses while sprinting.")
                .defineInRange("sprint_feathers_per_second", 1.0, 0.0, 40.0);

        JUMP_FEATHERS = builder
                .comment("Feathers per jump.")
                .defineInRange("jump_feathers", 0.5, 0.0, 40.0);

        builder.pop();

        builder.push("mounts");

        ENABLE_MOUNTS = builder
                .comment("Horses, donkeys, mules and camels have feathers too: galloping and jumping tire them, rest restores them.",
                        "Their feathers show above yours while you ride. An exhausted mount slows down.")
                .define("mounts_enabled", true);

        MOUNT_MIN_FEATHERS = builder
                .comment("Each mount is born with its own stamina, a hidden trait like its speed or health: somewhere",
                        "between these two, usually near the middle. Foals take after their parents.")
                .defineInRange("mount_feathers_min", 14, 1, 1000);

        MOUNT_MAX_FEATHERS = builder
                .comment("The most feathers a mount can be born with.")
                .defineInRange("mount_feathers_max", 30, 1, 1000);

        MOUNT_REGEN = builder
                .comment("Feathers a mount regenerates per second.")
                .defineInRange("mount_regen_feathers_per_second", 0.5, 0.0, 40.0);

        MOUNT_GALLOP_FEATHERS_PER_SECOND = builder
                .comment("Feathers per second while galloping (ridden faster than mount_gallop_speed). Animals tire slowly:",
                        "at 0.1, a 20-feather horse gallops for over three minutes.")
                .defineInRange("mount_gallop_feathers_per_second", 0.1, 0.0, 40.0);

        MOUNT_GALLOP_SPEED = builder
                .comment("Horizontal speed, in blocks per tick, from which a ridden mount counts as galloping. A walk is about 0.1.")
                .defineInRange("mount_gallop_speed", 0.25, 0.0, 10.0);

        MOUNT_JUMP_FEATHERS = builder
                .comment("Feathers for a fully charged jump (or a camel's dash); weaker jumps cost less.")
                .defineInRange("mount_jump_feathers", 0.5, 0.0, 40.0);

        MOUNT_EXHAUSTED_SLOWDOWN = builder
                .comment("How much an exhausted mount slows down: 0.5 = half speed until it recovers.")
                .defineInRange("mount_exhausted_slowdown", 0.5, 0.0, 0.95);

        builder.pop();

        builder.push("debugging");

        DEBUG_MODE = builder
                .comment("Shows a debug overlay with the feathers state and what spent them, and logs spends.")
                .define("debug_mode", false);

        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {}
}
