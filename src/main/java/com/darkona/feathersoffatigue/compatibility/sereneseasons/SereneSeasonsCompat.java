package com.darkona.feathersoffatigue.compatibility.sereneseasons;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import dev.darkona.jdlib.api.compat.Bridges;
import dev.darkona.jdlib.api.compat.climate.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class SereneSeasonsCompat {

    public static final boolean LOADED = Bridges.isLoaded("sereneseasons");

    public static final int CLIMATE_PRIORITY = 50;

    private SereneSeasonsCompat() {}

    public static void init() {
        if (LOADED) FeathersAPI.registerClimateProvider(id("serene_seasons"), CLIMATE_PRIORITY, SereneSeasonsCompat::climate);
    }

    private static Climate climate(LivingEntity entity) {
        if (!FeathersCompatConfig.SEASONS.get()) return null;
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        if (!level.canSeeSky(pos) || Seasons.isTropical(level.getBiome(pos))) return null;

        return switch (Seasons.current(level)) {
            case WINTER -> FeathersCompatConfig.SEASONS_WINTER_COLD.get()
                    && level.getBiome(pos).value().getModifiedClimateSettings().temperature() < FeathersCompatConfig.SEASONS_WINTER_MAX_TEMPERATURE.get()
                    ? Climate.COLD : null;
            case SUMMER -> FeathersCompatConfig.SEASONS_SUMMER_HEAT.get() && level.isDay() && !entity.isInWaterOrRain()
                    && level.getBiome(pos).value().getModifiedClimateSettings().temperature() >= FeathersCompatConfig.SEASONS_SUMMER_MIN_TEMPERATURE.get()
                    ? Climate.HOT : null;
            case SPRING, AUTUMN, TROPICAL_DRY, TROPICAL_WET -> null;
        };
    }
}
