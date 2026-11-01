package com.darkona.feathersoffatigue.compat.coldsweat;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.core.init.EffectInit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

final class ColdSweatBridge {
    private ColdSweatBridge() {}

    static double bodyTemperature(LivingEntity entity) {
        return Temperature.get(entity, Temperature.Trait.BODY);
    }

    static boolean canApplyCold(LivingEntity entity) {
        return !(entity.hasEffect(EffectInit.GRACE.get()) || entity.hasEffect(EffectInit.ICE_RESISTANCE.get()));
    }

    static boolean canApplyHeat(LivingEntity entity) {
        return !(entity.hasEffect(EffectInit.GRACE.get()) || entity.hasEffect(MobEffects.FIRE_RESISTANCE));
    }
}
