package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.client.FeatherAnimation;
import com.darkona.feathersoffatigue.api.client.FeatherAnimations;
import com.darkona.feathersoffatigue.api.client.FeatherStyle;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.ClientConfig;
import com.darkona.feathersoffatigue.config.ClientConfig.StrainAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import static com.darkona.feathersoffatigue.api.client.FeatherStyles.*;
import static com.darkona.feathersoffatigue.api.client.FeatherVariants.*;
import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class BuiltInFeatherStyles {

    public static final ResourceLocation STATUS_PROVIDER = id("status");

    private BuiltInFeatherStyles() {}

    public static void register() {
        registerVariant(FEATHER, SHEET, 1);
        registerVariant(CRYSTAL, SHEET, 2);
        registerVariant(GLINT, SHEET, 3);
        registerVariant(STRAINED, SHEET, 4);
        registerVariant(PLAIN, SHEET, 5);
        registerOverlay(FROST, SHEET, 6);
        registerOverlay(HEAT, SHEET, 7);

        registerStyle(GREEN, FeatherStyle.opaque(0x00B53A, 0x000000));
        registerStyle(BLUE, FeatherStyle.opaque(0x22A5F0, 0x000000));
        registerStyle(WHITE, FeatherStyle.opaque(0xF2F2F2, 0x3C3C3C));
        registerStyle(COLD, FeatherStyle.opaque(0x7DEFFF, 0x000000).withVariant(CRYSTAL).withOverlay(FROST, 0xFF8DC8FE, 0xFFFFFFFF));
        registerStyle(HOT, FeatherStyle.opaque(0xFF870C, 0x000000));
        registerStyle(ENERGIZED, FeatherStyle.opaque(0xFBEE2B, 0x000000).withVariant(GLINT));
        registerStyle(MOMENTUM, FeatherStyle.opaque(0x00B192, 0x000000).withVariant(CRYSTAL));
        registerStyle(STRAIN, FeatherStyle.opaque(0xFF0E0C, 0x940000).withVariant(STRAINED));
        registerStyle(ENDURANCE, FeatherStyle.opaque(0xD4AF37, 0x000000));
        registerStyle(ARMOR, new FeatherStyle(0xFFB8B9C4, FeatherStyle.NO_BORDER).withVariant(PLAIN));
        registerStyle(EMPTY, FeatherStyle.opaque(0x282828, 0x000000));
        registerStyle(EXHAUSTED, FeatherStyle.opaque(0x281616, 0x000000));
        registerStyleProvider(STATUS_PROVIDER, STATUS_PRIORITY, (player, feathers) -> status(player));
    }

    private static @Nullable ResourceLocation status(Player player) {
        if (player.hasEffect(FeathersMobEffects.COLD)) return COLD;
        if (player.hasEffect(FeathersMobEffects.HOT)) return HOT;
        if (player.hasEffect(FeathersMobEffects.ENERGIZED)) return ENERGIZED;
        if (player.hasEffect(FeathersMobEffects.MOMENTUM)) return MOMENTUM;
        return null;
    }

    public static void registerAnimations() {
        FeatherAnimations.registerProvider(STATUS_PROVIDER, FeatherAnimations.STATUS_PRIORITY, (entity, feathers) -> pick(entity, feathers,
                ClientConfig.STRAIN_ANIMATION.get(), ClientConfig.SHAKE_WHEN_LOW.get(), ClientConfig.LOW_FEATHERS.get(),
                ClientConfig.WAVE_WHEN_ENERGIZED.get()));
    }

    public static @Nullable FeatherAnimation pick(LivingEntity entity, FeathersView feathers, StrainAnimation strain, boolean shakeWhenLow,
                                                  int lowFeathers, boolean waveWhenEnergized) {
        if (feathers.strained() && strain != StrainAnimation.NONE) return strain == StrainAnimation.SHAKE ? FeatherAnimation.SHAKE : FeatherAnimation.PULSE;
        if (shakeWhenLow && feathers.maxFeathers() > lowFeathers && Stamina.toFeathers(feathers.availableStamina()) <= lowFeathers) {
            return FeatherAnimation.SHAKE;
        }
        if (waveWhenEnergized && FeathersAPI.isEnergized(entity)) return FeatherAnimation.WAVE;
        return null;
    }
}
