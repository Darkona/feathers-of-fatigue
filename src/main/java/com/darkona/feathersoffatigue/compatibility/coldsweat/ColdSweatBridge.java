package com.darkona.feathersoffatigue.compatibility.coldsweat;

import com.momosoftworks.coldsweat.core.init.ModEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

final class ColdSweatBridge {

    private ColdSweatBridge() {}

    static boolean canApplyCold(LivingEntity entity) {
        return !(entity.hasEffect(ModEffects.GRACE) || entity.hasEffect(ModEffects.ICE_RESISTANCE));
    }

    static boolean canApplyHeat(LivingEntity entity) {
        return !(entity.hasEffect(ModEffects.GRACE) || entity.hasEffect(MobEffects.FIRE_RESISTANCE));
    }
}
