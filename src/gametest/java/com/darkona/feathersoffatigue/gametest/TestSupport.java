package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import com.darkona.feathersoffatigue.core.FeathersData;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import com.darkona.feathersoffatigue.core.FeathersTicker;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Creates players for stamina tests. Vanilla's mock server player reports Creative mode, which bypasses stamina.
 * These tests use NeoForge fake players in Survival mode and tick them directly.
 */
final class TestSupport {

    private TestSupport() {}

    /** Compatibility switches that keep optional thirst or temperature mods from changing ordinary tests. */
    private static final List<ModConfigSpec.BooleanValue> COMPATS = List.of(FeathersCompatConfig.COLD_SWEAT, FeathersCompatConfig.THIRST,
            FeathersCompatConfig.DROPLETS_OF_THIRST, FeathersCompatConfig.TAN, FeathersCompatConfig.LSO, FeathersCompatConfig.SEASONS);

    static {
        COMPATS.forEach(value -> value.set(false));
    }

    /**
     * Runs {@code test} with every compat switched on.
     */
    static void withCompat(Runnable test) {
        COMPATS.forEach(value -> value.set(true));
        try {
            test.run();
        } finally {
            COMPATS.forEach(value -> value.set(false));
        }
    }

    static ServerPlayer player(GameTestHelper helper) {
        clearNoon(helper.getLevel());
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "feathers-test"));
        player.moveTo(helper.absoluteVec(Vec3.ZERO));
        FeathersAPI.get(player);
        return player;
    }

    /**
     * Noon under a clear sky, whenever the batch happens to run: the vanilla climate and Serene Seasons read the sun,
     * and rain on a cold biome chills.
     */
    static void clearNoon(ServerLevel level) {
        level.setDayTime(6000);
        level.setWeatherParameters(24000, 0, false, false);
        level.setRainLevel(0);
        level.setThunderLevel(0);
        level.updateSkyBrightness();
    }

    static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            FeathersTicker.tick(player);
            player.tickCount++;
        }
    }

    /**
     * The player's feathers as they would come back from a save.
     */
    static FeathersView saveAndLoad(GameTestHelper helper, ServerPlayer player) {
        CompoundTag tag = FeathersServiceImpl.data(player).serializeNBT(helper.getLevel().registryAccess());
        FeathersData loaded = new FeathersData();
        loaded.deserializeNBT(helper.getLevel().registryAccess(), tag);
        return loaded;
    }
}
