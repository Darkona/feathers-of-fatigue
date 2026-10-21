package com.darkona.feathersoffatigue.climate;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class VanillaClimate {

    private VanillaClimate() {}

    public static Climate climate(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (entity.isFreezing() || isColdWeather(level, pos)) return Climate.COLD;

        if (entity.wasOnFire || entity.isOnFire() || entity.isInLava()) return Climate.HOT;
        if (level.dimension() == Level.NETHER) return Climate.HOT;
        if (entity.isInPowderSnow || entity.isInWaterOrRain()) return Climate.NEUTRAL;
        if (!level.isDay() || !level.canSeeSky(pos)) return Climate.NEUTRAL;

        return level.getBiome(pos).value().getModifiedClimateSettings().temperature() >= FeathersServerConfig.HOT_TEMPERATURE.get()
                ? Climate.HOT : Climate.NEUTRAL;
    }

    private static boolean isColdWeather(Level level, BlockPos pos) {
        if (!level.isRaining() || !level.canSeeSky(pos)) return false;
        Biome biome = level.getBiome(pos).value();
        if (biome.getPrecipitationAt(pos) == Biome.Precipitation.NONE) return false;
        return biome.coldEnoughToSnow(pos) || biome.getModifiedClimateSettings().temperature() < FeathersServerConfig.COLD_TEMPERATURE.get();
    }
}
