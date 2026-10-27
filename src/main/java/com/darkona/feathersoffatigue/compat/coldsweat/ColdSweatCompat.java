package com.darkona.feathersoffatigue.compat.coldsweat;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.config.CompatConfig;
import dev.darkona.jdlib.api.compat.Bridges;
import dev.darkona.jdlib.api.compat.climate.BodyTemperature;
import net.minecraft.world.entity.LivingEntity;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class ColdSweatCompat {

    public static final boolean LOADED = Bridges.isLoaded("cold_sweat");

    public static final int CLIMATE_PRIORITY = 100;

    private ColdSweatCompat() {}

    public static boolean isEnabled() {
        return LOADED && CompatConfig.COLD_SWEAT.get();
    }

    public static void init() {
        if (!LOADED) return;
        FeathersAPI.registerClimateProvider(id("cold_sweat"), CLIMATE_PRIORITY, ColdSweatCompat::climate);
    }

    private static Climate climate(LivingEntity entity) {
        if (!isEnabled()) return null;
        double body = BodyTemperature.of(entity, 0.0);
        if (CompatConfig.COLD_SWEAT_HEAT.get()) {
            if (body >= CompatConfig.COLD_SWEAT_SEVERE_THRESHOLD.get()) return Climate.SCORCHING;
            if (body >= CompatConfig.COLD_SWEAT_HOT_THRESHOLD.get()) return Climate.HOT;
        }
        if (CompatConfig.COLD_SWEAT_COLD.get() && body <= CompatConfig.COLD_SWEAT_COLD_THRESHOLD.get()) {
            return Climate.COLD;
        }
        return Climate.NEUTRAL;
    }

    public static boolean canApplyCold(LivingEntity entity) {
        return !isEnabled() || ColdSweatBridge.canApplyCold(entity);
    }

    public static boolean canApplyHeat(LivingEntity entity) {
        return !isEnabled() || ColdSweatBridge.canApplyHeat(entity);
    }
}
