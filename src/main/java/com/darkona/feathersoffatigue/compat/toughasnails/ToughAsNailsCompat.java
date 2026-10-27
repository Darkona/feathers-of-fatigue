package com.darkona.feathersoffatigue.compat.toughasnails;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.compat.ThirstRegen;
import com.darkona.feathersoffatigue.config.CompatConfig;
import dev.darkona.jdlib.api.compat.Bridges;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class ToughAsNailsCompat {

    private static final ThirstRegen.Keys TAN_KEYS = ThirstRegen.Keys.of("tough_as_nails");

    public static final boolean LOADED = Bridges.isLoaded("toughasnails");

    public static final int CLIMATE_PRIORITY = 90;

    private ToughAsNailsCompat() {}

    public static void init() {
        if (!LOADED) return;
        FeathersAPI.registerClimateProvider(id("tough_as_nails"), CLIMATE_PRIORITY, ToughAsNailsCompat::climate);
        FeathersAPI.registerRegenFactor(id("tough_as_nails"), ToughAsNailsCompat::regenFactor);
    }

    private static Climate climate(LivingEntity entity) {
        if (!CompatConfig.TAN.get() || !CompatConfig.TAN_TEMPERATURE.get() || !(entity instanceof Player player)
                || !ToughAsNailsBridge.temperatureEnabled()) return null;

        if (ToughAsNailsBridge.hyperthermia(player) >= CompatConfig.TAN_SEVERE_HYPERTHERMIA.get()) return Climate.SCORCHING;
        return switch (ToughAsNailsBridge.temperature(player)) {
            case ICY -> Climate.COLD;
            case COLD -> CompatConfig.TAN_COLD_IS_COLD.get() ? Climate.COLD : Climate.NEUTRAL;
            case WARM -> CompatConfig.TAN_WARM_IS_HOT.get() ? Climate.HOT : Climate.NEUTRAL;
            case HOT -> Climate.HOT;
            case NEUTRAL -> Climate.NEUTRAL;
        };
    }

    private static double regenFactor(LivingEntity entity, FeathersView feathers) {
        if (!CompatConfig.TAN.get() || !CompatConfig.TAN_THIRST.get() || !(entity instanceof Player player)
                || !ToughAsNailsBridge.thirstEnabled()) return 0.0;

        float exhaustion = ThirstRegen.exhaustionSinceLastCall(player, feathers, TAN_KEYS, CompatConfig.TAN_THIRST_EXHAUSTION.get());
        if (exhaustion > 0) ToughAsNailsBridge.addThirstExhaustion(player, exhaustion);

        return ThirstRegen.factor(ToughAsNailsBridge.thirst(player), ToughAsNailsBridge.hydration(player),
                CompatConfig.TAN_THIRST_REDUCTION.get(), CompatConfig.TAN_HYDRATION_BONUS.get());
    }
}
