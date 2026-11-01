package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.content.feathers.SyncedFeathers;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import com.darkona.feathersoffatigue.network.SyncPayload;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.event.StrainEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.assertValueEqual;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.saveAndLoad;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;

@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class SpendTests {
    private static final ResourceLocation TEST = id("test");

    @GameTest(template = "empty")
    public static void newPlayersStartFull(GameTestHelper helper) {
        FeathersView f = FeathersAPI.get(player(helper));
        assertValueEqual(helper, f.stamina(), Stamina.ofFeathers(20), "stamina");
        assertValueEqual(helper, f.maxFeathers(), 20, "max feathers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spendsAreAllOrNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        SpendResult result = FeathersAPI.spend(player, TEST, Stamina.ofFeathers(25), SpendOptions.DEFAULT.withoutStrain());
        assertValueEqual(helper, result, SpendResult.INSUFFICIENT, "spend beyond the bar without strain");
        assertValueEqual(helper, FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "stamina after a refused spend");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hugeAmountsSaturate(GameTestHelper helper) {
        assertValueEqual(helper, Stamina.ofFeathers(3_000_000.0), Integer.MAX_VALUE, "huge feathers");
        assertValueEqual(helper, Stamina.ofFeathers(-3_000_000.0), Integer.MIN_VALUE, "huge negative feathers");
        assertValueEqual(helper, Stamina.ofFeathers(Double.POSITIVE_INFINITY), Integer.MAX_VALUE, "infinite feathers");
        assertValueEqual(helper, Stamina.ofFeathers(Double.NaN), 0, "NaN feathers");
        assertValueEqual(helper, Stamina.ofFeathers(3_000_000), Integer.MAX_VALUE, "huge whole feathers");
        assertValueEqual(helper, Stamina.ofFeathers(-3_000_000), Integer.MIN_VALUE, "huge negative whole feathers");
        assertValueEqual(helper, Stamina.toFeathersCeil(Integer.MIN_VALUE), -2_147_483, "ceiling of the lowest stamina");
        assertValueEqual(helper, Stamina.toFeathersCeil(1), 1, "ceiling of a sliver");

        ServerPlayer player = player(helper);
        assertValueEqual(helper, FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3_000_000.0)), SpendResult.INSUFFICIENT, "a huge spend");
        assertValueEqual(helper, FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "stamina after a huge spend");

        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack source = server.createCommandSourceStack();

        helper.assertTrue(server.getCommands().getDispatcher().parse("feathers spend @s 3000000", source).getReader().canRead(), "the command refuses 3000000 feathers");
        helper.assertTrue(server.getCommands().getDispatcher().parse("feathers set @s 3000000", source).getReader().canRead(), "and setting them");
        helper.assertFalse(server.getCommands().getDispatcher().parse("feathers spend @s 2000000", source).getReader().canRead(), "the command takes 2000000 feathers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overspendingGoesIntoStrain(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        assertValueEqual(helper, FeathersAPI.spend(player, TEST, Stamina.ofFeathers(25)), SpendResult.OK, "spend into strain");
        FeathersView f = FeathersAPI.get(player);
        assertValueEqual(helper, f.stamina(), 0, "stamina");
        assertValueEqual(helper, f.strain(), Stamina.ofFeathers(5), "strain");
        helper.assertFalse(f.exhausted(), "one feather of strain room is left");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gainsPayStrainBackFirst(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(24));
        boolean[] cleared = {false};
        Consumer<StrainEvent.Cleared> listener = event -> cleared[0] |= event.getEntity() == player;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, StrainEvent.Cleared.class, listener);
        try {
            assertValueEqual(helper, FeathersAPI.gain(player, TEST, Stamina.ofFeathers(3)), Stamina.ofFeathers(3), "stamina used by a gift under strain");
            FeathersView f = FeathersAPI.get(player);
            assertValueEqual(helper, f.strain(), Stamina.ofFeathers(1), "strain left after the gift");
            assertValueEqual(helper, f.stamina(), 0, "no stamina while strain is left");
            helper.assertFalse(cleared[0], "strain not cleared yet");

            assertValueEqual(helper, FeathersAPI.gain(player, TEST, Stamina.ofFeathers(5)), Stamina.ofFeathers(5), "stamina used by a gift past the strain");
            assertValueEqual(helper, f.strain(), 0, "strain paid back");
            assertValueEqual(helper, f.stamina(), Stamina.ofFeathers(4), "the rest of the gift");
            helper.assertTrue(cleared[0], "clearing the strain posts its event");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void settingFeathersClearsStrain(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(24));
        boolean[] cleared = {false};
        Consumer<StrainEvent.Cleared> listener = event -> cleared[0] |= event.getEntity() == player;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, StrainEvent.Cleared.class, listener);
        try {
            FeathersAPI.setStamina(player, 0);
            assertValueEqual(helper, FeathersAPI.get(player).strain(), Stamina.ofFeathers(4), "strain after setting no feathers");
            helper.assertFalse(cleared[0], "strain not cleared yet");

            var server = helper.getLevel().getServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player).withSuppressedOutput(), "feathers set @s 10");
            FeathersView f = FeathersAPI.get(player);
            assertValueEqual(helper, f.stamina(), Stamina.ofFeathers(10), "stamina set by the command");
            assertValueEqual(helper, f.strain(), 0, "strain after setting feathers");
            helper.assertTrue(cleared[0], "clearing the strain posts its event");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strainTurnedOffClearsStrainWithItsEvent(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(24));
        boolean[] cleared = {false};
        Consumer<StrainEvent.Cleared> listener = event -> cleared[0] |= event.getEntity() == player;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, StrainEvent.Cleared.class, listener);
        boolean strain = ServerConfig.ENABLE_STRAIN.get();
        try {
            ServerConfig.ENABLE_STRAIN.set(false);
            tick(player, 1);
            assertValueEqual(helper, FeathersAPI.get(player).strain(), 0, "strain with strain turned off");
            helper.assertTrue(cleared[0], "dropping the strain posts its event");
            helper.assertFalse(player.hasEffect(FeathersMobEffects.STRAINED.get()), "no strained effect without strain");
        } finally {
            ServerConfig.ENABLE_STRAIN.set(strain);
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void simulatingChangesNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(FeathersAPI.canSpend(player, TEST, Stamina.ofFeathers(10)), "can spend 10");
        helper.assertFalse(FeathersAPI.canSpend(player, TEST, Stamina.ofFeathers(27)), "can't spend past strain");
        assertValueEqual(helper, FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "stamina after simulating");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spendingEverythingExhausts(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(26));
        helper.assertTrue(FeathersAPI.get(player).exhausted(), "exhausted with no feathers and no strain room");
        assertValueEqual(helper, FeathersAPI.spend(player, TEST, 1), SpendResult.EXHAUSTED, "spending while exhausted");
        assertValueEqual(helper, FeathersAPI.spend(player, TEST, 0, SpendOptions.DEFAULT.ignoringExhaustion()), SpendResult.OK, "ignoring exhaustion");

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
        assertValueEqual(helper, f.bonusStamina(), 1000, "bonus left");
        assertValueEqual(helper, f.stamina(), Stamina.ofFeathers(20), "regular stamina untouched");
        assertValueEqual(helper, saveAndLoad(helper, player).bonusStamina(), 1000, "bonus after a save");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativePlayersAreExempt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setGameMode(GameType.CREATIVE);
        assertValueEqual(helper, FeathersAPI.spend(player, TEST, Stamina.ofFeathers(5)), SpendResult.EXEMPT, "creative spend");
        assertValueEqual(helper, FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "creative stamina");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heatDoublesCosts(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(FeathersMobEffects.HOT.get(), 200));
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3));
        assertValueEqual(helper, FeathersAPI.get(player).stamina(), Stamina.ofFeathers(14), "stamina after a doubled cost");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void costsFollowUsageChanges(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(1));
        player.addEffect(new MobEffectInstance(FeathersMobEffects.HOT.get(), 200));
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3));
        assertValueEqual(helper, FeathersAPI.get(player).stamina(), Stamina.ofFeathers(13), "a cost doubled by heat that came after the first spend");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clientPricingMatchesTheServer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addEffect(new MobEffectInstance(FeathersMobEffects.HOT.get(), 200));
        int priced = FeathersServiceImpl.effectiveCost(player, view(false), player.getAttribute(FeathersAttributes.USAGE_MULTIPLIER.get()), TEST, Stamina.ofFeathers(3));
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(3));
        assertValueEqual(helper, priced, Stamina.ofFeathers(20) - FeathersAPI.get(player).stamina(), "the client's price against the server's");
        assertValueEqual(helper, priced, Stamina.ofFeathers(6), "a cost doubled by heat");
        helper.succeed();
    }

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

    private static SyncedFeathers stamina(boolean exhausted) {
        SyncedFeathers feathers = new SyncedFeathers();
        feathers.apply(new SyncPayload(1, Stamina.ofFeathers(3), Stamina.ofFeathers(20), Stamina.ofFeathers(2), Stamina.ofFeathers(4), 0, 0, 0,
                exhausted, 0.0, RestState.NONE, new int[ArmorWeights.PARTS]));
        return feathers;
    }

    @GameTest(template = "empty")
    public static void checksFollowTheOptions(GameTestHelper helper) {
        boolean strain = ServerConfig.ENABLE_STRAIN.get();
        try {
            ServerConfig.ENABLE_STRAIN.set(true);
            assertValueEqual(helper, stamina(false).check(Stamina.ofFeathers(5), true, false), SpendResult.OK, "stamina plus strain room");
            assertValueEqual(helper, stamina(false).check(Stamina.ofFeathers(6), true, false), SpendResult.INSUFFICIENT, "past the strain room");
            assertValueEqual(helper, stamina(false).check(Stamina.ofFeathers(4), false, false), SpendResult.INSUFFICIENT, "no strain for this spend");
            assertValueEqual(helper, stamina(true).check(Stamina.ofFeathers(1), true, false), SpendResult.EXHAUSTED, "exhausted");
            assertValueEqual(helper, stamina(true).check(Stamina.ofFeathers(1), true, true), SpendResult.OK, "exhaustion ignored");
            ServerConfig.ENABLE_STRAIN.set(false);
            assertValueEqual(helper, stamina(false).check(Stamina.ofFeathers(4), true, false), SpendResult.INSUFFICIENT, "strain off on the server");
        } finally {
            ServerConfig.ENABLE_STRAIN.set(strain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fractionalCostsWork(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersAPI.spend(player, TEST, 250);
        assertValueEqual(helper, FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20) - 250, "a quarter feather");
        helper.succeed();
    }
}
