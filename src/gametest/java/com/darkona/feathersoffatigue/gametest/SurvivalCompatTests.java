package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.climate.ClimateEffects;
import com.darkona.feathersoffatigue.compatibility.dropletsofthirst.DropletsOfThirstCompat;
import com.darkona.feathersoffatigue.compatibility.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.compatibility.lso.LegendarySurvivalCompat;
import com.darkona.feathersoffatigue.compatibility.sereneseasons.SereneSeasonsCompat;
import com.darkona.feathersoffatigue.compatibility.thirst.ThirstCompat;
import com.darkona.feathersoffatigue.compatibility.toughasnails.ToughAsNailsCompat;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import com.darkona.feathersoffatigue.gametest.scenario.DropletsOfThirstScenario;
import com.darkona.feathersoffatigue.gametest.scenario.ColdSweatScenario;
import com.darkona.feathersoffatigue.gametest.scenario.LegendarySurvivalScenario;
import com.darkona.feathersoffatigue.gametest.scenario.SereneSeasonsScenario;
import com.darkona.feathersoffatigue.gametest.scenario.ThirstScenario;
import com.darkona.feathersoffatigue.gametest.scenario.ToughAsNailsScenario;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;
import static com.darkona.feathersoffatigue.gametest.TestSupport.withCompat;

/**
 * Drives each survival mod into an extreme with its own API and checks the feathers react: cold, scorching heat,
 * thirst. Each test passes without action when its optional mod is absent ({@code -Pcompat=...}).
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class SurvivalCompatTests {

    /** Applies the climate the providers report and returns it. */
    private static Climate applyClimate(ServerPlayer player) {
        Climate climate = ClimateEffects.evaluate(player);
        ClimateEffects.apply(player, climate);
        return climate;
    }

    private static void assertCold(GameTestHelper helper, ServerPlayer player, String mod) {
        helper.assertValueEqual(applyClimate(player), Climate.COLD, mod + " climate");
        helper.assertTrue(player.hasEffect(FeathersMobEffects.COLD), mod + ": the Cold effect");
    }

    private static void assertScorching(GameTestHelper helper, ServerPlayer player, String mod) {
        helper.assertValueEqual(applyClimate(player), Climate.SCORCHING, mod + " climate");
        helper.assertTrue(player.hasEffect(FeathersMobEffects.HOT), mod + ": the Heat effect");
        helper.assertTrue(player.hasEffect(FeathersMobEffects.FATIGUE), mod + ": the Fatigue effect");
    }

    /** Advances past the 20-tick thirst update interval and reads the resulting regeneration. */
    private static double regenAfterFactors(ServerPlayer player) {
        player.tickCount = 0;
        tick(player, 1);
        return FeathersAPI.getRegenPerSecond(player);
    }

    @GameTest(template = "empty")
    public static void coldSweatBodyTemperature(GameTestHelper helper) {
        if (ColdSweatCompat.LOADED) withCompat(() -> {
            ServerPlayer player = player(helper);
            ColdSweatScenario.bodyTemperature(player, -120);
            assertCold(helper, player, "Cold Sweat freezing");
            ColdSweatScenario.bodyTemperature(player, 120);
            assertScorching(helper, player, "Cold Sweat overheating");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void thirstWasTakenSlowsRegeneration(GameTestHelper helper) {
        if (ThirstCompat.LOADED) withCompat(() -> {
            ServerPlayer player = player(helper);
            ThirstScenario.thirst(player, 0, 0);
            helper.assertTrue(regenAfterFactors(player) < 0.05, "parched: regeneration nearly stops, was " + FeathersAPI.getRegenPerSecond(player));
            ThirstScenario.thirst(player, 20, 10);
            helper.assertTrue(regenAfterFactors(player) > 0.4, "quenched: regeneration above base, was " + FeathersAPI.getRegenPerSecond(player));
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dropletsOfThirstSlowsRegeneration(GameTestHelper helper) {
        if (DropletsOfThirstCompat.LOADED) withCompat(() -> {
            ServerPlayer player = player(helper);
            DropletsOfThirstScenario.thirst(player, 0, 0);
            helper.assertTrue(regenAfterFactors(player) < 0.05, "parched: regeneration nearly stops, was " + FeathersAPI.getRegenPerSecond(player));
            DropletsOfThirstScenario.thirst(player, 20, 10);
            helper.assertTrue(regenAfterFactors(player) > 0.4, "quenched: regeneration above base, was " + FeathersAPI.getRegenPerSecond(player));
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dropletsOfThirstRegenerationCostsThirst(GameTestHelper helper) {
        if (DropletsOfThirstCompat.LOADED) withCompat(() -> {
            FeathersCompatConfig.DROPLETS_OF_THIRST_THIRST_PER_FEATHER.set(1.0);
            try {
                ServerPlayer player = player(helper);
                DropletsOfThirstScenario.thirst(player, 20, 0);
                FeathersAPI.setStamina(player, 0);
                tick(player, 200);
                int thirst = DropletsOfThirstScenario.thirst(player);
                helper.assertTrue(thirst <= 18, "one thirst point per regenerated feather: thirst should drop, was " + thirst);
            } finally {
                FeathersCompatConfig.DROPLETS_OF_THIRST_THIRST_PER_FEATHER.set(0.0);
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void toughAsNailsTemperatureAndThirst(GameTestHelper helper) {
        if (ToughAsNailsCompat.LOADED) withCompat(() -> {
            ServerPlayer player = player(helper);
            if (ToughAsNailsScenario.temperatureEnabled()) {
                ToughAsNailsScenario.icy(player);
                assertCold(helper, player, "Tough As Nails icy");
                ToughAsNailsScenario.heatstroke(player);
                assertScorching(helper, player, "Tough As Nails hyperthermia");
            }
            if (ToughAsNailsScenario.thirstEnabled()) {
                ToughAsNailsScenario.parched(player);
                helper.assertTrue(regenAfterFactors(player) < 0.05, "parched: regeneration nearly stops, was " + FeathersAPI.getRegenPerSecond(player));
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legendarySurvivalTemperatureAndThirst(GameTestHelper helper) {
        if (LegendarySurvivalCompat.LOADED) withCompat(() -> {
            ServerPlayer player = player(helper);
            LegendarySurvivalScenario.frostbite(player);
            assertCold(helper, player, "LSO frostbite");
            LegendarySurvivalScenario.heatstroke(player);
            assertScorching(helper, player, "LSO heat stroke");
            LegendarySurvivalScenario.parched(player);
            helper.assertTrue(regenAfterFactors(player) < 0.05, "parched: regeneration nearly stops, was " + FeathersAPI.getRegenPerSecond(player));
        });
        helper.succeed();
    }

    /**
     * Only meaningful without a body-temperature mod: those outrank the seasons, by design.
     */
    @GameTest(template = "empty")
    public static void sereneSeasonsWinterIsCold(GameTestHelper helper) {
        boolean bodyTemperatureMod = ColdSweatCompat.LOADED || ToughAsNailsCompat.LOADED || LegendarySurvivalCompat.LOADED;
        if (SereneSeasonsCompat.LOADED && !bodyTemperatureMod) withCompat(() -> {
            ServerPlayer player = player(helper);
            // Well above the test area, under open sky: winter only chills those outdoors.
            player.moveTo(helper.absoluteVec(new Vec3(0, 100, 0)));
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                    helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), "season set mid_winter");
            Climate climate = ClimateEffects.evaluate(player);
            String state = " (season " + SereneSeasonsScenario.season(helper.getLevel()) + ", sky " + helper.getLevel().canSeeSky(player.blockPosition())
                    + ", biome temperature " + helper.getLevel().getBiome(player.blockPosition()).value().getModifiedClimateSettings().temperature()
                    + ", at " + player.blockPosition() + ")";
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                    helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), "season set mid_spring");
            helper.assertValueEqual(climate, Climate.COLD, "outdoors in winter" + state);
        });
        helper.succeed();
    }
}
