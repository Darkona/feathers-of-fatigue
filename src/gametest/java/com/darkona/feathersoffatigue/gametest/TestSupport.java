package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import dev.darkona.jdlib.testkit.TestPlayers;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

final class TestSupport {

    private TestSupport() {}

    private static final List<ModConfigSpec.BooleanValue> COMPATS = List.of(FeathersCompatConfig.COLD_SWEAT, FeathersCompatConfig.THIRST,
            FeathersCompatConfig.DROPLETS_OF_THIRST, FeathersCompatConfig.TAN, FeathersCompatConfig.LSO, FeathersCompatConfig.SEASONS);

    static {
        COMPATS.forEach(value -> value.set(false));
    }

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
        ServerPlayer player = TestPlayers.survival(helper, "feathers-test");
        FeathersAPI.get(player);
        return player;
    }

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

    static FeathersView saveAndLoad(GameTestHelper helper, ServerPlayer player) {
        CompoundTag tag = FeathersServiceImpl.data(player).serializeNBT(helper.getLevel().registryAccess());
        FeathersData loaded = new FeathersData();
        loaded.deserializeNBT(helper.getLevel().registryAccess(), tag);
        return loaded;
    }
}
