package com.darkona.feathersoffatigue.compat.thirst;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.compat.ThirstRegen;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import dev.darkona.jdlib.api.compat.Bridges;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class ThirstCompat {

    private static final ThirstRegen.Keys THIRST_KEYS = ThirstRegen.Keys.of("thirst_was_taken");

    public static final boolean LOADED = Bridges.isLoaded("thirst");

    private ThirstCompat() {}

    public static void init() {
        if (LOADED) FeathersAPI.registerRegenFactor(id("thirst_was_taken"), ThirstCompat::regenFactor);
    }

    private static double regenFactor(LivingEntity entity, FeathersView feathers) {
        if (!FeathersCompatConfig.THIRST.get() || !(entity instanceof Player player)) return 0.0;

        int owed = ThirstRegen.owedSinceLastCall(player, feathers, THIRST_KEYS, FeathersCompatConfig.THIRST_PER_FEATHER.get());
        if (owed > 0) ThirstBridge.drain(player, owed);

        return ThirstRegen.factor(ThirstBridge.thirst(player), ThirstBridge.quench(player),
                FeathersCompatConfig.THIRST_REGEN_REDUCTION.get(), FeathersCompatConfig.QUENCH_REGEN_BONUS.get());
    }
}
