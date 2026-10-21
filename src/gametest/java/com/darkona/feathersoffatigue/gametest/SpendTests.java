package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.event.StrainEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.saveAndLoad;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;

/**
 * One-off spends: all or nothing, simulation, strain, exhaustion, bonus stamina, exemptions, multipliers.
 * Defaults: 20 max feathers, 6 max strain, strain and exhaustion on.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpendTests {

    private static final ResourceLocation TEST = id("test");

    @GameTest(template = "empty")
    public static void newPlayersStartFull(GameTestHelper helper) {
        FeathersView f = FeathersAPI.get(player(helper));
        helper.assertValueEqual(f.stamina(), Stamina.ofFeathers(20), "stamina");
        helper.assertValueEqual(f.maxFeathers(), 20, "max feathers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spendsAreAllOrNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        SpendResult result = FeathersAPI.spend(player, TEST, Stamina.ofFeathers(25), SpendOptions.DEFAULT.withoutStrain());
        helper.assertValueEqual(result, SpendResult.INSUFFICIENT, "spend beyond the bar without strain");
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "stamina after a refused spend");
        helper.succeed();
    }

    /**
     * Huge amounts saturate instead of wrapping into free negative spends. The command rejects values that do not fit.
     */
    @GameTest(template = "empty")
    public static void hugeAmountsSaturate(GameTestHelper helper) {
        helper.assertValueEqual(Stamina.ofFeathers(3_000_000.0), Integer.MAX_VALUE, "huge feathers");
        helper.assertValueEqual(Stamina.ofFeathers(-3_000_000.0), Integer.MIN_VALUE, "huge negative feathers");
        helper.assertValueEqual(Stamina.ofFeathers(Double.POSITIVE_INFINITY), Integer.MAX_VALUE, "infinite feathers");
        helper.assertValueEqual(Stamina.ofFeathers(Double.NaN), 0, "NaN feathers");
        helper.assertValueEqual(Stamina.ofFeathers(3_000_000), Integer.MAX_VALUE, "huge whole feathers");
        helper.assertValueEqual(Stamina.ofFeathers(-3_000_000), Integer.MIN_VALUE, "huge negative whole feathers");
        helper.assertValueEqual(Stamina.toFeathersCeil(Integer.MIN_VALUE), -2_147_483, "ceiling of the lowest stamina");
        helper.assertValueEqual(Stamina.toFeathersCeil(1), 1, "ceiling of a sliver");

        ServerPlayer player = player(helper);
        helper.assertValueEqual(FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3_000_000.0)), SpendResult.INSUFFICIENT, "a huge spend");
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "stamina after a huge spend");

        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        var source = server.createCommandSourceStack();
        helper.assertTrue(Commands.getParseException(dispatcher.parse("feathers spend @s 3000000", source)) != null, "the command refuses 3000000 feathers");
        helper.assertTrue(Commands.getParseException(dispatcher.parse("feathers set @s 3000000", source)) != null, "and setting them");
        helper.assertTrue(Commands.getParseException(dispatcher.parse("feathers spend @s 2000000", source)) == null, "the command takes 2000000 feathers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overspendingGoesIntoStrain(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertValueEqual(FeathersAPI.spend(player, TEST, Stamina.ofFeathers(25)), SpendResult.OK, "spend into strain");
        FeathersView f = FeathersAPI.get(player);
        helper.assertValueEqual(f.stamina(), 0, "stamina");
        helper.assertValueEqual(f.strain(), Stamina.ofFeathers(5), "strain");
        helper.assertFalse(f.exhausted(), "one feather of strain room is left");
        helper.succeed();
    }

    /**
     * A gift while strained pays the strain back first, as regeneration does: stamina and strain never sit side by
     * side. Clearing the last of it posts the event, like the tick's own recovery.
     */
    @GameTest(template = "empty")
    public static void gainsPayStrainBackFirst(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(24));
        boolean[] cleared = {false};
        Consumer<StrainEvent.Cleared> listener = event -> cleared[0] |= event.getEntity() == player;
        NeoForge.EVENT_BUS.addListener(StrainEvent.Cleared.class, listener);
        try {
            helper.assertValueEqual(FeathersAPI.gain(player, TEST, Stamina.ofFeathers(3)), Stamina.ofFeathers(3), "stamina used by a gift under strain");
            FeathersView f = FeathersAPI.get(player);
            helper.assertValueEqual(f.strain(), Stamina.ofFeathers(1), "strain left after the gift");
            helper.assertValueEqual(f.stamina(), 0, "no stamina while strain is left");
            helper.assertFalse(cleared[0], "strain not cleared yet");

            helper.assertValueEqual(FeathersAPI.gain(player, TEST, Stamina.ofFeathers(5)), Stamina.ofFeathers(5), "stamina used by a gift past the strain");
            helper.assertValueEqual(f.strain(), 0, "strain paid back");
            helper.assertValueEqual(f.stamina(), Stamina.ofFeathers(4), "the rest of the gift");
            helper.assertTrue(cleared[0], "clearing the strain posts its event");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    /**
     * Setting the feathers of a strained player (the API, {@code /feathers set}) clears the strain, with its event:
     * stamina and strain never sit side by side. Setting them to zero keeps it.
     */
    @GameTest(template = "empty")
    public static void settingFeathersClearsStrain(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(24));
        boolean[] cleared = {false};
        Consumer<StrainEvent.Cleared> listener = event -> cleared[0] |= event.getEntity() == player;
        NeoForge.EVENT_BUS.addListener(StrainEvent.Cleared.class, listener);
        try {
            FeathersAPI.setStamina(player, 0);
            helper.assertValueEqual(FeathersAPI.get(player).strain(), Stamina.ofFeathers(4), "strain after setting no feathers");
            helper.assertFalse(cleared[0], "strain not cleared yet");

            var server = helper.getLevel().getServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), "feathers set @s 10");
            FeathersView f = FeathersAPI.get(player);
            helper.assertValueEqual(f.stamina(), Stamina.ofFeathers(10), "stamina set by the command");
            helper.assertValueEqual(f.strain(), 0, "strain after setting feathers");
            helper.assertTrue(cleared[0], "clearing the strain posts its event");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    /**
     * Strain turned off in the config while a player is strained: the strain goes on the next tick, with its event,
     * and the strained effect is not put on the player on the way out.
     */
    @GameTest(template = "empty")
    public static void strainTurnedOffClearsStrainWithItsEvent(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(24));
        boolean[] cleared = {false};
        Consumer<StrainEvent.Cleared> listener = event -> cleared[0] |= event.getEntity() == player;
        NeoForge.EVENT_BUS.addListener(StrainEvent.Cleared.class, listener);
        boolean strain = FeathersServerConfig.ENABLE_STRAIN.get();
        try {
            FeathersServerConfig.ENABLE_STRAIN.set(false);
            tick(player, 1);
            helper.assertValueEqual(FeathersAPI.get(player).strain(), 0, "strain with strain turned off");
            helper.assertTrue(cleared[0], "dropping the strain posts its event");
            helper.assertFalse(player.hasEffect(FeathersMobEffects.STRAINED), "no strained effect without strain");
        } finally {
            FeathersServerConfig.ENABLE_STRAIN.set(strain);
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void simulatingChangesNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(FeathersAPI.canSpend(player, TEST, Stamina.ofFeathers(10)), "can spend 10");
        helper.assertFalse(FeathersAPI.canSpend(player, TEST, Stamina.ofFeathers(27)), "can't spend past strain");
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "stamina after simulating");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spendingEverythingExhausts(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(26));
        helper.assertTrue(FeathersAPI.get(player).exhausted(), "exhausted with no feathers and no strain room");
        helper.assertValueEqual(FeathersAPI.spend(player, TEST, 1), SpendResult.EXHAUSTED, "spending while exhausted");
        helper.assertValueEqual(FeathersAPI.spend(player, TEST, 0, SpendOptions.DEFAULT.ignoringExhaustion()), SpendResult.OK, "ignoring exhaustion");

        FeathersAPI.reset(player);
        helper.assertFalse(FeathersAPI.get(player).exhausted(), "reset ends exhaustion");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bonusStaminaIsSpentFirstAndSaved(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.addBonusStamina(player, TEST, 3000, -1);
        FeathersAPI.spend(player, TEST, 2000);

        FeathersView f = FeathersAPI.get(player);
        helper.assertValueEqual(f.bonusStamina(), 1000, "bonus left");
        helper.assertValueEqual(f.stamina(), Stamina.ofFeathers(20), "regular stamina untouched");
        helper.assertValueEqual(saveAndLoad(helper, player).bonusStamina(), 1000, "bonus after a save");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativePlayersAreExempt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setGameMode(GameType.CREATIVE);
        helper.assertValueEqual(FeathersAPI.spend(player, TEST, Stamina.ofFeathers(5)), SpendResult.EXEMPT, "creative spend");
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "creative stamina");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heatDoublesCosts(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(FeathersMobEffects.HOT, 200));
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3));
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(14), "stamina after a doubled cost");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void costsFollowUsageChanges(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(1));
        player.addEffect(new MobEffectInstance(FeathersMobEffects.HOT, 200));
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3));
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(13), "a cost doubled by heat that came after the first spend");
        helper.succeed();
    }

    /**
     * The client prices a predicted spend like the server: the usage multiplier (Heat doubles it) and the stamina
     * modifiers, against the view it has.
     */
    @GameTest(template = "empty")
    public static void clientPricingMatchesTheServer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(FeathersMobEffects.HOT, 200));
        int priced = FeathersServiceImpl.effectiveCost(player, view(false), player.getAttribute(FeathersAttributes.USAGE_MULTIPLIER), TEST, Stamina.ofFeathers(3));
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3));
        helper.assertValueEqual(priced, Stamina.ofFeathers(20) - FeathersAPI.get(player).stamina(), "the client's price against the server's");
        helper.assertValueEqual(priced, Stamina.ofFeathers(6), "a cost doubled by heat");
        helper.succeed();
    }

    /** A view as the client or a mount's rider sees it: 3 feathers, 2 of strain room out of 4, exhausted or not. */
    private static FeathersView view(boolean exhausted) {
        return new FeathersView() {
            public boolean hasFeathers() { return true; }
            public int stamina() { return Stamina.ofFeathers(3); }
            public int maxStamina() { return Stamina.ofFeathers(20); }
            public int availableStamina() { return Stamina.ofFeathers(3); }
            public int strain() { return Stamina.ofFeathers(2); }
            public int maxStrain() { return Stamina.ofFeathers(4); }
            public boolean exhausted() { return exhausted; }
        };
    }

    @GameTest(template = "empty")
    public static void viewChecksFollowTheOptions(GameTestHelper helper) {
        boolean strain = FeathersServerConfig.ENABLE_STRAIN.get();
        try {
            FeathersServerConfig.ENABLE_STRAIN.set(true);
            helper.assertValueEqual(FeathersServiceImpl.simulateAgainst(view(false), Stamina.ofFeathers(5), true, false), SpendResult.OK, "stamina plus strain room");
            helper.assertValueEqual(FeathersServiceImpl.simulateAgainst(view(false), Stamina.ofFeathers(6), true, false), SpendResult.INSUFFICIENT, "past the strain room");
            helper.assertValueEqual(FeathersServiceImpl.simulateAgainst(view(false), Stamina.ofFeathers(4), false, false), SpendResult.INSUFFICIENT, "no strain for this spend");
            helper.assertValueEqual(FeathersServiceImpl.simulateAgainst(view(true), Stamina.ofFeathers(1), true, false), SpendResult.EXHAUSTED, "exhausted");
            helper.assertValueEqual(FeathersServiceImpl.simulateAgainst(view(true), Stamina.ofFeathers(1), true, true), SpendResult.OK, "exhaustion ignored");
            FeathersServerConfig.ENABLE_STRAIN.set(false);
            helper.assertValueEqual(FeathersServiceImpl.simulateAgainst(view(false), Stamina.ofFeathers(4), true, false), SpendResult.INSUFFICIENT, "strain off on the server");
        } finally {
            FeathersServerConfig.ENABLE_STRAIN.set(strain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fractionalCostsWork(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, 250);
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20) - 250, "a quarter feather");
        helper.succeed();
    }
}
