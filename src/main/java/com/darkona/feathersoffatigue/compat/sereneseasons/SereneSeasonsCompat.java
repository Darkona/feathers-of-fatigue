package com.darkona.feathersoffatigue.compat.sereneseasons;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class SereneSeasonsCompat {
    public static final boolean LOADED = ModList.get().isLoaded("sereneseasons");

    public static final int CLIMATE_PRIORITY = 50;

    private SereneSeasonsCompat() {}

    public static void init() {
        if (LOADED) FeathersAPI.registerClimateProvider(id("serene_seasons"), CLIMATE_PRIORITY, SereneSeasonsCompat::climate);
    }

    private static Climate climate(LivingEntity entity) {
        if (!CompatConfig.SEASONS.get()) return null;
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        if (!level.canSeeSky(pos) || SereneSeasonsBridge.isTropical(level, pos)) return null;

        return switch (SereneSeasonsBridge.season(level)) {
            case WINTER -> CompatConfig.SEASONS_WINTER_COLD.get()
                    && level.getBiome(pos).value().getModifiedClimateSettings().temperature() < CompatConfig.SEASONS_WINTER_MAX_TEMPERATURE.get()
                    ? Climate.COLD : null;
            case SUMMER -> CompatConfig.SEASONS_SUMMER_HEAT.get() && level.isDay() && !entity.isInWaterOrRain()
                    && level.getBiome(pos).value().getModifiedClimateSettings().temperature() >= CompatConfig.SEASONS_SUMMER_MIN_TEMPERATURE.get()
                    ? Climate.HOT : null;
            case SPRING, AUTUMN -> null;
        };
    }
}
