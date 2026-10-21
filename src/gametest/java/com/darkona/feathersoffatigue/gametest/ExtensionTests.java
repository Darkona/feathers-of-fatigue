package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.StaminaModifier;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.basic.BasicExertion;
import com.darkona.feathersoffatigue.climate.ClimateEffects;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;


/**
 * The extension points, used the way another mod would. Registrations are global and permanent, so each only
 * affects players carrying this test's tag.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class ExtensionTests {

    private static final String MARK = "feathers_of_fatigue_extension_test";
    /** Separate from MARK: a cold player regenerates at half speed, which would skew the other tests. */
    private static final String COLD_MARK = "feathers_of_fatigue_extension_test_cold";
    private static final ResourceLocation FREE = id("test_free_action");

    static {
        FeathersAPI.registerStaminaModifier(id("test_free_actions"), 100, new StaminaModifier() {
            @Override
            public int modifyCost(LivingEntity entity, FeathersView feathers, ResourceLocation source, int cost) {
                return entity.getTags().contains(MARK) && source.equals(FREE) ? 0 : cost;
            }
        });
        FeathersAPI.registerClimateProvider(id("test_climate"), 1000, entity -> entity.getTags().contains(COLD_MARK) ? Climate.COLD : null);
        FeathersAPI.registerRegenFactor(id("test_regen"), (entity, feathers) -> entity.getTags().contains(MARK) ? 1.0 : 0.0);
        FeathersAPI.registerWeightSource(id("test_backpack"), entity -> entity.getTags().contains(MARK) ? 3.0 : 0.0);
    }

    private static ServerPlayer marked(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.addTag(MARK);
        return player;
    }

    @GameTest(template = "empty")
    public static void staminaModifiersSeeTheSource(GameTestHelper helper) {
        ServerPlayer player = marked(helper);
        FeathersAPI.spend(player, FREE, Stamina.ofFeathers(5));
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(20), "a free action costs nothing");
        FeathersAPI.spend(player, id("test_paid_action"), Stamina.ofFeathers(5));
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), Stamina.ofFeathers(15), "other actions still cost");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void higherPriorityClimateProvidersWin(GameTestHelper helper) {
        ServerPlayer cold = player(helper);
        cold.addTag(COLD_MARK);
        helper.assertValueEqual(ClimateEffects.evaluate(cold), Climate.COLD, "climate from the test provider");
        helper.assertValueEqual(ClimateEffects.evaluate(player(helper)), Climate.NEUTRAL, "unmarked players use the vanilla climate");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void regenFactorsAddToRegeneration(GameTestHelper helper) {
        ServerPlayer player = marked(helper);
        FeathersAPI.setStamina(player, 0);
        tick(player, 1);
        helper.assertValueEqual(FeathersAPI.getRegenPerSecond(player), 1.4, "0.4 base + 1.0 from the factor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weightSourcesAddToArmor(GameTestHelper helper) {
        boolean before = FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.get();
        FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.set(true);
        try {
            ServerPlayer player = marked(helper);
            FeathersAPI.recalculateWeight(player);
            helper.assertValueEqual(FeathersAPI.get(player).weight(), 3, "backpack weight with no armor");
        } finally {
            FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.set(before);
        }
        helper.succeed();
    }

    /**
     * A mod that charges player actions itself turns the basic exertion off. The take-over is global and permanent, so
     * this is the only test that jumps: basic exertion stays off for the rest of the run.
     */
    @GameTest(template = "empty")
    public static void takingOverPlayerActionsStopsBasicExertion(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(BasicExertion.isActive(), "basic exertion on before the take-over");
        player.jumpFromGround();
        int afterJump = FeathersAPI.get(player).stamina();
        helper.assertTrue(afterJump < Stamina.ofFeathers(20), "a jump costs feathers");

        FeathersAPI.takeOverPlayerActions("feathers_of_fatigue_test");
        helper.assertTrue(FeathersAPI.arePlayerActionsTakenOver(), "player actions taken over");
        helper.assertTrue(FeathersAPI.getPlayerActionOwners().contains("feathers_of_fatigue_test"), "the owner is listed");
        helper.assertFalse(BasicExertion.isActive(), "basic exertion off after the take-over");
        player.jumpFromGround();
        helper.assertValueEqual(FeathersAPI.get(player).stamina(), afterJump, "a jump is free after the take-over");
        helper.succeed();
    }
}
