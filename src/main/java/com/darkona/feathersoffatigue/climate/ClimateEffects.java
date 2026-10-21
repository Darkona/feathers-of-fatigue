package com.darkona.feathersoffatigue.climate;

import com.darkona.feathersoffatigue.api.ClimateProvider;
import net.minecraft.resources.ResourceLocation;
import dev.darkona.jdlib.core.registry.PriorityList;
import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.core.Extensions;
import com.darkona.feathersoffatigue.effect.FeathersMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class ClimateEffects {

    public static final int INTERVAL = 40;

    private ClimateEffects() {}

    public static void registerBuiltIn() {
        Extensions.addClimate(id("vanilla"), 0, VanillaClimate::climate);
    }

    public static Climate evaluate(LivingEntity entity) {
        if (FeathersServerConfig.FATIGUE_FROM_BURNING.get() && (entity.isOnFire() || entity.isInLava())) return Climate.SCORCHING;
        if (FeathersServerConfig.FATIGUE_FROM_NETHER.get() && entity.level().dimension() == Level.NETHER) return Climate.SCORCHING;

        PriorityList.Snapshot<ResourceLocation, ClimateProvider> providers = Extensions.climates();
        for (int i = 0; i < providers.size(); i++) {
            Climate climate = providers.value(i).getClimate(entity);
            if (climate != null) return climate;
        }
        return Climate.NEUTRAL;
    }

    public static void apply(LivingEntity entity, Climate climate) {
        removeIfRefused(entity, FeathersMobEffects.HOT);
        removeIfRefused(entity, FeathersMobEffects.FATIGUE);
        removeIfRefused(entity, FeathersMobEffects.COLD);

        if (climate != Climate.COLD && climate != Climate.NEUTRAL) entity.removeEffect(FeathersMobEffects.COLD);
        if (climate == Climate.COLD) entity.removeEffect(FeathersMobEffects.HOT);

        if (FeathersServerConfig.ENABLE_COLD.get()) update(entity, FeathersMobEffects.COLD, climate == Climate.COLD);
        if (FeathersServerConfig.ENABLE_HEAT.get()) update(entity, FeathersMobEffects.HOT, climate.isHot());
        if (FeathersServerConfig.ENABLE_FATIGUE.get()) update(entity, FeathersMobEffects.FATIGUE, climate == Climate.SCORCHING);
    }

    private static void update(LivingEntity entity, Holder<MobEffect> effect, boolean active) {
        MobEffectInstance current = entity.getEffect(effect);
        boolean permanent = current != null && current.isInfiniteDuration();

        if (active) {
            if (!permanent && canApply(entity, effect)) {
                entity.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            }
        } else if (permanent) {
            entity.removeEffect(effect);
            int linger = FeathersServerConfig.EFFECT_LINGER.get();
            if (linger > 0) entity.addEffect(new MobEffectInstance(effect, linger, 0, false, true));
        }
    }

    private static void removeIfRefused(LivingEntity entity, Holder<MobEffect> effect) {
        if (entity.hasEffect(effect) && !canApply(entity, effect)) entity.removeEffect(effect);
    }

    private static boolean canApply(LivingEntity entity, Holder<MobEffect> effect) {
        return !(effect.value() instanceof FeathersMobEffect feathersEffect) || feathersEffect.canApply(entity);
    }
}
