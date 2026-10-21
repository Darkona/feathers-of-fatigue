package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;

/**
 * Resting speeds up paying strain back, and so do API rest bonuses. A night slept through restores everything.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class RestTests {

    private static ServerPlayer strainedPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, id("test"), Stamina.ofFeathers(24), SpendOptions.DEFAULT.withRegenDelay(0));
        return player;
    }

    @GameTest(template = "empty")
    public static void crouchingStillRecoversStrainFaster(GameTestHelper helper) {
        ServerPlayer active = strainedPlayer(helper);
        ServerPlayer resting = strainedPlayer(helper);
        resting.setShiftKeyDown(true);
        resting.setPose(Pose.CROUCHING);

        tick(active, 80);
        tick(resting, 80);

        helper.assertValueEqual(FeathersAPI.get(resting).restState(), RestState.CROUCHING, "rest state");
        int activeStrain = FeathersAPI.get(active).strain();
        int restingStrain = FeathersAPI.get(resting).strain();
        helper.assertTrue(restingStrain < activeStrain, "crouching still should recover faster: " + restingStrain + " vs " + activeStrain);
        helper.succeed();
    }

    /**
     * A player that slept long enough (the level wakes sleepers only then) and lies in bed: sleeping is what the
     * wake-up event checks, not the bed.
     */
    private static Player sleeper(GameTestHelper helper) {
        Player player = new Player(helper.getLevel(), BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "feathers-sleeper")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public boolean isSleepingLongEnough() {
                return true;
            }
        };
        FeathersAPI.spend(player, id("test"), Stamina.ofFeathers(10));
        return player;
    }

    private static void wakeUp(Player player, boolean wakeImmediately, boolean updateLevel) {
        NeoForge.EVENT_BUS.post(new PlayerWakeUpEvent(player, wakeImmediately, updateLevel));
    }

    /**
     * Only a night slept through restores the feathers: "Leave Bed" at night, after the five seconds the level counts
     * as sleeping, does not (in multiplayer the other players may be up, and the night goes on). A sleep mod that skips
     * the night itself and wakes the players with {@code stopSleeping()} does restore them, by day.
     */
    @GameTest(template = "empty")
    public static void onlyANightSleptThroughRestoresFeathers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        try {
            level.setDayTime(18000);
            level.updateSkyBrightness();
            Player player = sleeper(helper);
            wakeUp(player, false, true);
            helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(10), "feathers after leaving the bed at night");
            wakeUp(player, true, false);
            helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(10), "feathers after disconnecting at night");
            wakeUp(player, false, false);
            helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "feathers after the level skipped the night");

            player = sleeper(helper);
            player.stopSleeping();
            helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(10), "feathers after being woken at once at night");

            level.setDayTime(6000);
            level.updateSkyBrightness();
            player = sleeper(helper);
            wakeUp(player, false, true);
            helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "feathers after the day broke over the bed");
            player = sleeper(helper);
            player.stopSleeping();
            helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "feathers after a sleep mod woke the player by day");
            player = sleeper(helper);
            wakeUp(player, true, false);
            helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(10), "feathers after disconnecting by day");
        } finally {
            TestSupport.clearNoon(level);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void restBonusFromApiApplies(GameTestHelper helper) {
        ServerPlayer normal = strainedPlayer(helper);
        ServerPlayer soaking = strainedPlayer(helper);
        FeathersAPI.setRestBonus(soaking, id("test_hot_spring"), 4.0, 100);

        tick(normal, 20);
        tick(soaking, 20);

        helper.assertValueEqual(FeathersAPI.get(soaking).restMultiplier(), 4.0, "rest multiplier");
        int normalPaid = Stamina.ofFeathers(4) - FeathersAPI.get(normal).strain();
        int soakingPaid = Stamina.ofFeathers(4) - FeathersAPI.get(soaking).strain();
        helper.assertTrue(soakingPaid >= 3.5 * normalPaid, "a hot spring pays back about 4x: " + soakingPaid + " vs " + normalPaid);
        helper.succeed();
    }
}
