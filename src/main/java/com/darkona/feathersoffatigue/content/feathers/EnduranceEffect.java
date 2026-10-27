package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public class EnduranceEffect extends FeathersMobEffect {

    public static final ResourceLocation BONUS = id("endurance_effect");

    public EnduranceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFF00);
    }

    private static int bonus(MobEffectInstance instance) {
        return Stamina.ofFeathers((instance.getAmplifier() + 1) * 8);
    }

    @Override
    public boolean canApply(LivingEntity entity) {
        return super.canApply(entity) && ServerConfig.ENABLE_ENDURANCE.get();
    }

    @Override
    public void onApplied(LivingEntity entity, MobEffectInstance instance, @Nullable MobEffectInstance previous) {
        if (!FeathersAPI.hasFeathers(entity)) return;
        int full = bonus(instance);
        int left = FeathersServiceImpl.data(entity).bonusStamina(BONUS);
        int amount = previous != null && left > 0 ? Math.min(full, left + Math.max(0, full - bonus(previous))) : full;
        FeathersAPI.addBonusStamina(entity, BONUS, amount, instance.isInfiniteDuration() ? -1 : instance.getDuration());
    }

    @Override
    public void onEnded(LivingEntity entity, MobEffectInstance instance) {
        FeathersAPI.removeBonusStamina(entity, BONUS);
    }
}
