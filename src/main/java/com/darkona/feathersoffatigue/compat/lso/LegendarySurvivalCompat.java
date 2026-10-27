package com.darkona.feathersoffatigue.compat.lso;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.compat.ThirstRegen;
import com.darkona.feathersoffatigue.config.CompatConfig;
import dev.darkona.jdlib.api.compat.Bridges;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class LegendarySurvivalCompat {

    private static final ThirstRegen.Keys LSO_KEYS = ThirstRegen.Keys.of("legendary_survival");

    public static final boolean LOADED = Bridges.isLoaded("legendarysurvivaloverhaul");

    public static final int CLIMATE_PRIORITY = 80;

    private LegendarySurvivalCompat() {}

    public static void init() {
        if (!LOADED) return;
        FeathersAPI.registerClimateProvider(id("legendary_survival_overhaul"), CLIMATE_PRIORITY, LegendarySurvivalCompat::climate);
        FeathersAPI.registerRegenFactor(id("legendary_survival_overhaul"), LegendarySurvivalCompat::regenFactor);
    }

    private static Climate climate(LivingEntity entity) {
        if (!CompatConfig.LSO.get() || !CompatConfig.LSO_TEMPERATURE.get() || !(entity instanceof Player player)) return null;
        return switch (LegendarySurvivalBridge.temperature(player)) {
            case FROSTBITE, COLD -> Climate.COLD;
            case HOT -> Climate.HOT;
            case HEAT_STROKE -> Climate.SCORCHING;
            case NORMAL -> Climate.NEUTRAL;
        };
    }

    private static double regenFactor(LivingEntity entity, FeathersView feathers) {
        if (!CompatConfig.LSO.get() || !CompatConfig.LSO_THIRST.get() || !(entity instanceof Player player)) return 0.0;

        float exhaustion = ThirstRegen.exhaustionSinceLastCall(player, feathers, LSO_KEYS, CompatConfig.LSO_THIRST_EXHAUSTION.get());
        if (exhaustion > 0) LegendarySurvivalBridge.addThirstExhaustion(player, exhaustion);

        return ThirstRegen.factor(LegendarySurvivalBridge.hydration(player), LegendarySurvivalBridge.saturation(player),
                CompatConfig.LSO_THIRST_REDUCTION.get(), CompatConfig.LSO_HYDRATION_BONUS.get());
    }
}
