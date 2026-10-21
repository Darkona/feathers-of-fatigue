package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.client.FeatherAnimation;
import com.darkona.feathersoffatigue.api.client.FeatherAnimations;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.FeathersClientConfig.StrainAnimation;
import com.darkona.feathersoffatigue.style.BuiltInFeatherAnimations;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;

/**
 * Tests the feather animation API and built-in triggers with explicit settings. GameTests do not load the client
 * configuration or register the client-only built-in provider.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class AnimationTests {

    private static final String MARK = "feathers_of_fatigue_animation_test";
    private static final String HIGH_MARK = "feathers_of_fatigue_animation_test_high";
    private static final FeatherAnimation SLOW_WAVE = new FeatherAnimation(FeatherAnimation.Kind.WAVE, 0.5f, 3f);

    static {
        FeatherAnimations.registerProvider(id("test_animation_low"), 100, (entity, feathers) -> entity.getTags().contains(MARK) ? SLOW_WAVE : null);
        FeatherAnimations.registerProvider(id("test_animation_high"), 200, (entity, feathers) -> entity.getTags().contains(HIGH_MARK) ? FeatherAnimation.PULSE : null);
    }

    @GameTest(template = "empty")
    public static void higherPriorityAnimationProvidersWin(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(FeatherAnimations.select(player, FeathersAPI.get(player)) == null, "no provider answers");
        player.addTag(MARK);
        helper.assertValueEqual(Objects.requireNonNull(FeatherAnimations.select(player, FeathersAPI.get(player))),
                SLOW_WAVE, "the only provider answering");
        player.addTag(HIGH_MARK);
        helper.assertValueEqual(Objects.requireNonNull(FeatherAnimations.select(player, FeathersAPI.get(player))),
                FeatherAnimation.PULSE, "the higher priority");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void animationsRefuseNonsense(GameTestHelper helper) {
        helper.assertTrue(animationRefused(0f, 1f), "a speed of 0 is refused");
        helper.assertTrue(animationRefused(Float.POSITIVE_INFINITY, 1f), "an infinite speed is refused");
        helper.assertTrue(animationRefused(1f, Float.NaN), "a NaN amplitude is refused");
        helper.succeed();
    }

    private static boolean animationRefused(float speed, float amplitude) {
        try {
            new FeatherAnimation(FeatherAnimation.Kind.SHAKE, speed, amplitude);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    @GameTest(template = "empty")
    public static void feathersOfFatigueTriggersPickByState(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        FeathersView full = FeathersAPI.get(player);
        helper.assertTrue(pick(player, full) == null, "full feathers, no effect: still");

        player.addEffect(new MobEffectInstance(FeathersMobEffects.ENERGIZED, 200));
        helper.assertValueEqual(pick(player, FeathersAPI.get(player)), FeatherAnimation.WAVE, "a wave while energized");
        helper.assertTrue(BuiltInFeatherAnimations.pick(player, FeathersAPI.get(player), StrainAnimation.PULSE, true, 2, false) == null, "the wave can be turned off");

        FeathersAPI.setStamina(player, Stamina.ofFeathers(2));
        helper.assertValueEqual(pick(player, FeathersAPI.get(player)), FeatherAnimation.SHAKE, "two feathers left: a shake, over the wave");
        FeathersAPI.setStamina(player, Stamina.ofFeathers(3));
        helper.assertValueEqual(pick(player, FeathersAPI.get(player)), FeatherAnimation.WAVE, "three left is above the threshold");

        FeathersAPI.setStamina(player, 0);
        FeathersAPI.spend(player, id("test_strain"), Stamina.ofFeathers(2));
        helper.assertTrue(FeathersAPI.get(player).strained(), "strained");
        helper.assertValueEqual(pick(player, FeathersAPI.get(player)), FeatherAnimation.PULSE, "strain first, as set");
        helper.assertValueEqual(Objects.requireNonNull(BuiltInFeatherAnimations.pick(player, FeathersAPI.get(player),
                        StrainAnimation.SHAKE, true, 2, true)), FeatherAnimation.SHAKE,
                "or a shake");
        helper.assertValueEqual(Objects.requireNonNull(BuiltInFeatherAnimations.pick(player, FeathersAPI.get(player),
                        StrainAnimation.NONE, false, 2, true)), FeatherAnimation.WAVE,
                "strain and low off: the wave is left");
        helper.succeed();
    }

    private static FeatherAnimation pick(ServerPlayer player, FeathersView feathers) {
        return BuiltInFeatherAnimations.pick(player, feathers, StrainAnimation.PULSE, true, 2, true);
    }
}
