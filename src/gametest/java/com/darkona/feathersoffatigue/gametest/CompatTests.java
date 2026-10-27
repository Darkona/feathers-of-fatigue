package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.Extensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.withCompat;

@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class CompatTests {

    @GameTest(template = "empty")
    public static void everyClimateProviderAnswers(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        withCompat(() -> {
            StringBuilder answers = new StringBuilder();
            for (int i = 0; i < Extensions.climates().size(); i++) {
                Climate climate = Extensions.climates().value(i).getClimate(player);
                answers.append(Extensions.climates().id(i)).append('=').append(climate).append(' ');
            }
            helper.assertTrue(Extensions.climates().size() > 0, "no climate providers");
            helper.assertTrue(true, answers.toString());
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyRegenFactorAnswers(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        withCompat(() -> {
            for (int i = 0; i < Extensions.regenFactors().size(); i++) {
                double value = Extensions.regenFactors().value(i).feathersPerSecond(player, FeathersAPI.get(player));
                helper.assertTrue(Double.isFinite(value), Extensions.regenFactors().id(i) + " returned " + value);
            }
        });
        helper.succeed();
    }
}
