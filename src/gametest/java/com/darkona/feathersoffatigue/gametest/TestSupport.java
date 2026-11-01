package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.config.CompatConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

final class TestSupport {
    private TestSupport() {}

    private static final List<ForgeConfigSpec.BooleanValue> COMPATS = List.of(CompatConfig.COLD_SWEAT, CompatConfig.THIRST,
            CompatConfig.DROPLETS_OF_THIRST, CompatConfig.TAN, CompatConfig.LSO, CompatConfig.SEASONS);

    static {
        stopWatchingConfigFiles();
        COMPATS.forEach(value -> value.set(false));
    }

    private static void stopWatchingConfigFiles() {
        for (ModConfig config : ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER)) {
            if (!FeathersIds.MOD_ID.equals(config.getModId())) continue;
            try {
                FileWatcher.defaultInstance().setWatch(config.getFullPath(), () -> {});
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
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
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "feathers-test"));
        player.moveTo(helper.absoluteVec(Vec3.ZERO));
        player.getFoodData().setSaturation(0f);
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
        CompoundTag tag = FeathersServiceImpl.data(player).serializeNBT();
        FeathersData loaded = new FeathersData();
        loaded.deserializeNBT(tag);
        return loaded;
    }

    static <N> void assertValueEqual(GameTestHelper helper, N actual, N expected, String name) {
        if (!actual.equals(expected)) {
            throw new GameTestAssertException("Expected " + name + " to be " + expected + ", but was " + actual);
        }
    }
}
