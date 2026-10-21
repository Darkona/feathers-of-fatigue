package com.darkona.feathersoffatigue.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class FeathersClientConfig {

    public static final ModConfigSpec SPEC;

    public static final BooleanValue FADE_WHEN_FULL;
    public static final IntValue FADE_COOLDOWN;
    public static final IntValue FADE_IN_DURATION;
    public static final IntValue FADE_OUT_DURATION;
    public static final BooleanValue REGEN_EFFECT;
    public static final BooleanValue FROST_SOUND;
    public static final BooleanValue DISPLAY_WEIGHTS;
    public static final BooleanValue VISUAL_WEIGHTS;
    public static final BooleanValue AFFECTED_BY_RIGHT_HEIGHT;
    public static final IntValue X_OFFSET;
    public static final IntValue Y_OFFSET;
    public static final EnumValue<FeatherColor> FEATHER_COLOR;
    public static final BooleanValue WAVE_WHEN_ENERGIZED;
    public static final BooleanValue SHAKE_WHEN_LOW;
    public static final IntValue LOW_FEATHERS;
    public static final EnumValue<StrainAnimation> STRAIN_ANIMATION;

    public enum FeatherColor {
        GREEN,
        BLUE,
        WHITE
    }

    public enum StrainAnimation {
        NONE,
        SHAKE,
        PULSE
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("hud");

        FADE_WHEN_FULL = builder.comment("Fade the feathers out while they are full.")
                .define("fade_when_full", false);

        FADE_COOLDOWN = builder.comment("Ticks full before the feathers fade.")
                .defineInRange("fade_cooldown_ticks", 60, 0, 1200);

        FADE_IN_DURATION = builder.comment("Ticks the fade-in takes.")
                .defineInRange("fade_in_ticks", 40, 1, 1200);

        FADE_OUT_DURATION = builder.comment("Ticks the fade-out takes.")
                .defineInRange("fade_out_ticks", 40, 1, 1200);

        REGEN_EFFECT = builder.comment("Flash the feathers when one regenerates.")
                .define("regen_flash", false);

        AFFECTED_BY_RIGHT_HEIGHT = builder.comment("Stack the feathers with the other bars on the right (food, air, thirst...).",
                        "Off: always draw them right above the food bar and let other bars sort themselves out.")
                .define("stack_with_right_bars", true);

        X_OFFSET = builder.comment("Horizontal offset of the feathers, in pixels.")
                .defineInRange("x_offset", 0, -1000, 1000);

        Y_OFFSET = builder.comment("Vertical offset of the feathers, in pixels. Negative moves them up.")
                .defineInRange("y_offset", 0, -1000, 1000);

        FEATHER_COLOR = builder.comment("Color of your feathers: GREEN, BLUE (Elenai's original) or WHITE (like a chicken's).",
                        "Mounts' feathers take their own color, and armor weight its armor's.")
                .defineEnum("feather_color", FeatherColor.GREEN);

        builder.pop();

        builder.push("animations");

        WAVE_WHEN_ENERGIZED = builder.comment("A wave runs along the feathers while Energized, like hearts under Regeneration.")
                .define("wave_when_energized", true);

        SHAKE_WHEN_LOW = builder.comment("The feathers shake when few are left, like hearts at low health.")
                .define("shake_when_low", true);

        LOW_FEATHERS = builder.comment("How many usable feathers count as few, for shake_when_low.")
                .defineInRange("low_feathers", 2, 0, 20);

        STRAIN_ANIMATION = builder.comment("How the feathers move while strained: NONE, SHAKE or PULSE (brighten and dim).")
                .defineEnum("strain_animation", StrainAnimation.PULSE);

        builder.pop();

        builder.push("feedback");

        FROST_SOUND = builder.comment("Play a sound when the Cold effect freezes the feathers.")
                .define("cold_sound", true);

        DISPLAY_WEIGHTS = builder.comment("Show armor weight in item tooltips.")
                .define("weight_in_tooltips", true);

        VISUAL_WEIGHTS = builder.comment("Show tooltip weights as feather icons (true) or as text (false).")
                .define("weight_as_icons", false);

        builder.pop();

        SPEC = builder.build();
    }

    private FeathersClientConfig() {}
}
