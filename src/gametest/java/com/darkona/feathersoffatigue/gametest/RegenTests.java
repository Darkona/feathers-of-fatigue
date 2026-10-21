package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.DrainOptions;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;

/**
 * Regeneration, its pauses, and continuous drains. Default regeneration: 0.4 feathers/s = 20 stamina a tick.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class RegenTests {

    private static final ResourceLocation TEST = id("test");
    private static final ResourceLocation OTHER = id("other");

    private static ServerPlayer emptyPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.setStamina(player, 0);
        return player;
    }

    /**
     * The game keeps attribute bases when a player dies and respawns, and so do the maximum and regeneration set by
     * {@code /feathers max} and {@code /feathers regen}: the respawned player does not get the config's back.
     */
    @GameTest(template = "empty")
    public static void commandBasesSurviveDeath(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(dead, helper.getLevel()));
        FeathersAPI.setMaxFeathers(dead, 30);
        FeathersAPI.setBaseRegenPerSecond(dead, 1.5);

        // PlayerList.respawn: restoreFrom copies the bases, posts the clone event, then the new player joins the level.
        ServerPlayer respawned = player(helper);
        respawned.getAttributes().assignBaseValues(dead.getAttributes());
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(respawned, dead, true));
        NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(respawned, helper.getLevel()));

        helper.assertValueEqual(respawned.getAttributeBaseValue(FeathersAttributes.MAX_FEATHERS), 30.0, "max feathers after respawning");
        helper.assertValueEqual(respawned.getAttributeBaseValue(FeathersAttributes.FEATHERS_PER_SECOND), 1.5, "regeneration after respawning");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void regenerationRefills(GameTestHelper helper) {
        ServerPlayer player = emptyPlayer(helper);
        tick(player, 50);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 1000, "one feather after 50 ticks at 0.4 f/s");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void regenerationFollowsRateChanges(GameTestHelper helper) {
        ServerPlayer player = emptyPlayer(helper);
        tick(player, 50);
        FeathersAPI.setBaseRegenPerSecond(player, 1.0);
        tick(player, 20);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 2000, "one feather at 0.4 f/s, then one more at 1 f/s");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hugeRegenDelaysStopAtTheCap(GameTestHelper helper) {
        ServerPlayer player = emptyPlayer(helper);
        FeathersAPI.setStamina(player, 5000);
        FeathersAPI.spend(player, TEST, 1000);
        FeathersAPI.spend(player, TEST, 1000, SpendOptions.DEFAULT.withRegenDelay(Integer.MAX_VALUE));
        helper.assertValueEqual(FeathersAPI.get(player).regenDelay(), FeathersServerConfig.MAX_COOLDOWN.get() * 20, "the delay stops at max_cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void verySlowRegenerationStillAddsUp(GameTestHelper helper) {
        ServerPlayer player = emptyPlayer(helper);
        FeathersAPI.setBaseRegenPerSecond(player, 0.01);
        tick(player, 100);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 50, "0.01 f/s for 5 s: fractions carry over");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spendingPausesRegeneration(GameTestHelper helper) {
        ServerPlayer player = emptyPlayer(helper);
        FeathersAPI.setStamina(player, 5000);
        FeathersAPI.spend(player, TEST, 1000, SpendOptions.DEFAULT.withRegenDelay(20));
        tick(player, 20);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 4000, "no regeneration during the delay");
        tick(player, 5);
        helper.assertTrue(FeathersAPI.get(player).stamina() > 4000, "regeneration resumes after the delay");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void regenerationPaysStrainBackFirst(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(21), SpendOptions.DEFAULT.withRegenDelay(0));
        tick(player, 25);
        // 20 on the first tick, then 5 a tick once the strained effect cuts regeneration by 75%.
        helper.assertValueEqual(FeathersAPI.get(player).strain(), 1000 - 20 - 24 * 5, "strain after 25 ticks of payback");
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 0, "no stamina while strained");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void regenerationBlocksAreKeptPerSource(GameTestHelper helper) {
        ServerPlayer player = emptyPlayer(helper);
        FeathersAPI.blockRegen(player, TEST, -1);
        FeathersAPI.blockRegen(player, OTHER, -1);
        FeathersAPI.unblockRegen(player, TEST);
        tick(player, 10);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 0, "still blocked by the other source");
        FeathersAPI.unblockRegen(player, OTHER);
        tick(player, 10);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 200, "regenerating once both are gone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drainsPayEveryTickAndStopWhenEmpty(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.setStamina(player, 500);
        FeathersAPI.startDrain(player, TEST, 100, DrainOptions.DEFAULT.withoutStrain().withTimeout(0));
        tick(player, 3);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 200, "after three ticks of 100");
        tick(player, 2);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 0, "drained");
        tick(player, 1);
        helper.assertFalse(FeathersAPI.isDraining(player, TEST), "the drain stops when it can't pay");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void entriesAreRemovedPerSource(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.startDrain(player, TEST, 10, DrainOptions.DEFAULT.withTimeout(0));
        FeathersAPI.startDrain(player, OTHER, 10, DrainOptions.DEFAULT.withTimeout(0));
        FeathersAPI.stopDrain(player, TEST);
        helper.assertFalse(FeathersAPI.isDraining(player, TEST), "the stopped drain is gone");
        helper.assertTrue(FeathersAPI.isDraining(player, OTHER), "the other drain goes on");

        FeathersAPI.addBonusStamina(player, TEST, 1000, -1);
        FeathersAPI.addBonusStamina(player, OTHER, 2000, -1);
        FeathersAPI.removeBonusStamina(player, TEST);
        helper.assertValueEqual(FeathersAPI.get(player).bonusStamina(), 2000, "only the other bonus is left");

        FeathersAPI.setRestBonus(player, TEST, 4.0, -1);
        FeathersAPI.setRestBonus(player, OTHER, 3.0, -1);
        FeathersAPI.removeRestBonus(player, TEST);
        tick(player, 1);
        helper.assertValueEqual(FeathersAPI.get(player).restMultiplier(), 3.0, "only the other rest bonus is left");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fractionalDrainsCarryOver(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.startDrain(player, TEST, 12.5, DrainOptions.DEFAULT.withTimeout(0));
        tick(player, 8);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20) - 100, "8 ticks of 12.5");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void forgottenDrainsTimeOut(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.startDrain(player, TEST, 10);
        helper.runAfterDelay(10, () -> {
            tick(player, 1);
            helper.assertFalse(FeathersAPI.isDraining(player, TEST), "a drain nobody refreshes times out");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void freeActionsCanStillPauseRegeneration(GameTestHelper helper) {
        ServerPlayer player = emptyPlayer(helper);
        FeathersAPI.spend(player, TEST, 0, SpendOptions.DEFAULT.withRegenDelay(20));
        tick(player, 20);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), 0, "a free action with a delay pauses regeneration");
        helper.succeed();
    }
}
