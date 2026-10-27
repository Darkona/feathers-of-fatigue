package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import dev.darkona.jdlib.api.attribute.HookedMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class FeathersMobEffect extends HookedMobEffect {

    public FeathersMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean canApply(LivingEntity entity) {
        return !(entity instanceof Player player && (player.isCreative() || player.getAbilities().invulnerable));
    }

    public static boolean isProtectedFromHeat(LivingEntity entity) {
        return entity.hasEffect(MobEffects.FIRE_RESISTANCE) || entity.hasEffect(FeathersMobEffects.COOLING);
    }
}
