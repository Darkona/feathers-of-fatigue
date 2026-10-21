package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.core.Extensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.withCompat;

/**
 * Calls every registered climate provider and regeneration factor on a real player with its compat switched on,
 * so each bridge actually links against the other mod. Meaningful with compat mods on the runtime
 * ({@code -Pcompat=...}). Without them, only the built-in integrations run.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class CompatTests {

    @GameTest(template = "empty")
    public static void everyClimateProviderAnswers(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        withCompat(() -> {
            StringBuilder answers = new StringBuilder();
            for (Extensions.ClimateEntry entry : Extensions.climates()) {
                Climate climate = entry.provider().getClimate(player);
                answers.append(entry.id()).append('=').append(climate).append(' ');
            }
            helper.assertTrue(Extensions.climates().length > 0, "no climate providers");
            helper.assertTrue(true, answers.toString());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyRegenFactorAnswers(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        withCompat(() -> {
            for (Extensions.RegenEntry entry : Extensions.regenFactors()) {
                double value = entry.factor().feathersPerSecond(player, FeathersAPI.get(player));
                helper.assertTrue(Double.isFinite(value), entry.id() + " returned " + value);
            }
        });
        helper.succeed();
    }
}
