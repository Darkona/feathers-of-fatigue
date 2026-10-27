package com.darkona.feathersoffatigue.compat.dropletsofthirst;

import com.darkona.dropletsofthirst.api.DropletsView;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.compat.ThirstRegen;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import dev.darkona.jdlib.api.compat.Bridges;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class DropletsOfThirstCompat {

    private static final ThirstRegen.Keys KEYS = ThirstRegen.Keys.of("droplets_of_thirst");

    public static final boolean LOADED = Bridges.isLoaded("droplets_of_thirst");

    private DropletsOfThirstCompat() {}

    public static void init() {
        if (LOADED) FeathersAPI.registerRegenFactor(id("droplets_of_thirst"), DropletsOfThirstCompat::regenFactor);
    }

    private static double regenFactor(LivingEntity entity, FeathersView feathers) {
        if (!FeathersCompatConfig.DROPLETS_OF_THIRST.get() || !(entity instanceof Player player)) return 0.0;

        DropletsView thirst = DropletsOfThirstBridge.view(player);
        if (!thirst.isEnabled()) return 0.0;

        int owed = ThirstRegen.owedSinceLastCall(player, feathers, KEYS, FeathersCompatConfig.DROPLETS_OF_THIRST_THIRST_PER_FEATHER.get());
        if (owed > 0 && !player.level().isClientSide()) DropletsOfThirstBridge.drain(player, owed);

        return ThirstRegen.factor(thirst.thirst(), thirst.quenched(),
                FeathersCompatConfig.DROPLETS_OF_THIRST_REGEN_REDUCTION.get(), FeathersCompatConfig.DROPLETS_OF_THIRST_QUENCH_BONUS.get());
    }
}
