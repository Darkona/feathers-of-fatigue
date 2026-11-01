package com.darkona.feathersoffatigue.content.feathers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.Extensions;
import com.darkona.feathersoffatigue.content.feathers.FeathersMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class ClimateEffects {
    public static final int INTERVAL = 40;

    private ClimateEffects() {}

    public static void registerBuiltIn() {
        Extensions.addClimate(id("vanilla"), 0, ClimateEffects::vanilla);
    }

    public static Climate evaluate(LivingEntity entity) {
        if (ServerConfig.FATIGUE_FROM_BURNING.get() && (entity.isOnFire() || entity.isInLava())) return Climate.SCORCHING;
        if (ServerConfig.FATIGUE_FROM_NETHER.get() && entity.level().dimension() == Level.NETHER) return Climate.SCORCHING;

        for (Extensions.ClimateEntry entry : Extensions.climates()) {
            Climate climate = entry.provider().getClimate(entity);
            if (climate != null) return climate;
        }
        return Climate.NEUTRAL;
    }

    public static void apply(LivingEntity entity, Climate climate) {
        removeIfRefused(entity, FeathersMobEffects.HOT);
        removeIfRefused(entity, FeathersMobEffects.FATIGUE);
        removeIfRefused(entity, FeathersMobEffects.COLD);

        if (climate != Climate.COLD && climate != Climate.NEUTRAL) entity.removeEffect(FeathersMobEffects.COLD.get());
        if (climate == Climate.COLD) entity.removeEffect(FeathersMobEffects.HOT.get());

        if (ServerConfig.ENABLE_COLD.get()) update(entity, FeathersMobEffects.COLD, climate == Climate.COLD);
        if (ServerConfig.ENABLE_HEAT.get()) update(entity, FeathersMobEffects.HOT, climate.isHot());
        if (ServerConfig.ENABLE_FATIGUE.get()) update(entity, FeathersMobEffects.FATIGUE, climate == Climate.SCORCHING);
    }

    private static void update(LivingEntity entity, RegistryObject<MobEffect> holder, boolean active) {
        MobEffect effect = holder.get();
        MobEffectInstance current = entity.getEffect(effect);
        boolean permanent = current != null && current.isInfiniteDuration();

        if (active) {
            if (!permanent && canApply(entity, effect)) {
                entity.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            }
        } else if (permanent) {
            entity.removeEffect(effect);
            int linger = ServerConfig.EFFECT_LINGER.get();
            if (linger > 0) entity.addEffect(new MobEffectInstance(effect, linger, 0, false, true));
        }
    }

    private static void removeIfRefused(LivingEntity entity, RegistryObject<MobEffect> holder) {
        MobEffect effect = holder.get();
        if (entity.hasEffect(effect) && !canApply(entity, effect)) entity.removeEffect(effect);
    }

    private static boolean canApply(LivingEntity entity, MobEffect effect) {
        return !(effect instanceof FeathersMobEffect feathersEffect) || feathersEffect.canApply(entity);
    }

    private static Climate vanilla(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (entity.isFreezing() || isColdWeather(level, pos)) return Climate.COLD;

        if (entity.wasOnFire || entity.isOnFire() || entity.isInLava()) return Climate.HOT;
        if (level.dimension() == Level.NETHER) return Climate.HOT;
        if (entity.isInPowderSnow || entity.isInWaterOrRain()) return Climate.NEUTRAL;
        if (!level.isDay() || !level.canSeeSky(pos)) return Climate.NEUTRAL;

        return level.getBiome(pos).value().getModifiedClimateSettings().temperature() >= ServerConfig.HOT_TEMPERATURE.get()
                ? Climate.HOT : Climate.NEUTRAL;
    }

    private static boolean isColdWeather(Level level, BlockPos pos) {
        if (!level.isRaining() || !level.canSeeSky(pos)) return false;
        Biome biome = level.getBiome(pos).value();
        if (biome.getPrecipitationAt(pos) == Biome.Precipitation.NONE) return false;
        return biome.coldEnoughToSnow(pos) || biome.getModifiedClimateSettings().temperature() < ServerConfig.COLD_TEMPERATURE.get();
    }
}
