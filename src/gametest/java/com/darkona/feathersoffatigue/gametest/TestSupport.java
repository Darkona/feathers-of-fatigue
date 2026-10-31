package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.config.CompatConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.darkona.feathersoffatigue.content.feathers.StaminaView;
import dev.darkona.jdlib.api.stat.ResourceCodecs;
import dev.darkona.jdlib.core.stat.Resource;
import dev.darkona.jdlib.testkit.TestPlayers;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

final class TestSupport {

    private TestSupport() {}

    private static final List<ModConfigSpec.BooleanValue> COMPATS = List.of(CompatConfig.COLD_SWEAT, CompatConfig.THIRST,
            CompatConfig.DROPLETS_OF_THIRST, CompatConfig.TAN, CompatConfig.LSO, CompatConfig.SEASONS);

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
        Resource<ResourceLocation> saved = FeathersServiceImpl.stamina(player);
        Tag tag = ResourceCodecs.SAVED.encodeStart(NbtOps.INSTANCE, saved).getOrThrow();
        Resource<ResourceLocation> loaded = ResourceCodecs.SAVED.parse(NbtOps.INSTANCE, tag).getOrThrow();
        return new StaminaView() {
            @Override
            protected Resource<ResourceLocation> resource() {
                return loaded;
            }
        };
    }
}
