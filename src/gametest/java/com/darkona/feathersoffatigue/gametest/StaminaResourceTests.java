package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.SyncedFeathers;
import com.darkona.feathersoffatigue.network.SyncPayload;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.assertValueEqual;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;

@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class StaminaResourceTests {

    private static final ResourceLocation TEST = id("test");
    private static final UUID BACKPACK = UUID.nameUUIDFromBytes("test_backpack".getBytes());
    private static final UUID TRAINING = UUID.nameUUIDFromBytes("test_training".getBytes());

    @GameTest(template = "empty")
    public static void theClientsPredictionLandsOnTheServer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        tick(player, 5);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(12));
        tick(player, 1);
        FeathersData server = FeathersServiceImpl.data(player);
        SyncedFeathers client = new SyncedFeathers();
        client.follow(player);
        client.apply(SyncPayload.of(player.getId(), server));

        int sentStep = server.stamina() / Stamina.PER_FEATHER;
        double sentRate = server.rate();
        boolean sentDelayed = server.regenDelay() > 0;
        int syncs = 0;
        int worst = 0;
        for (int t = 0; t < 400; t++) {
            tick(player, 1);
            client.predict();
            worst = Math.max(worst, Math.abs(client.stamina() - server.stamina()));
            int step = server.stamina() / Stamina.PER_FEATHER;
            boolean delayed = server.regenDelay() > 0;
            if (step != sentStep || server.rate() != sentRate || delayed != sentDelayed) {
                client.apply(SyncPayload.of(player.getId(), server));
                sentStep = step;
                sentRate = server.rate();
                sentDelayed = delayed;
                syncs++;
            }
        }
        helper.assertTrue(server.stamina() > Stamina.ofFeathers(8), "the server regenerated: " + server.stamina());
        helper.assertTrue(worst <= 1, "the client's prediction stays on the server's stamina between syncs, off by " + worst);
        helper.assertTrue(syncs < 20, "the server syncs only on whole feathers and rate changes, not every tick: " + syncs);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorWeightIsAnAttributeOtherModsAddTo(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AttributeInstance weight = player.getAttribute(FeathersAttributes.ARMOR_WEIGHT.get());
        helper.assertTrue(weight != null, "players have the armor weight attribute");
        weight.addTransientModifier(new AttributeModifier(BACKPACK, "test_backpack", 3.0, AttributeModifier.Operation.ADDITION));
        tick(player, 1);
        FeathersView f = FeathersAPI.get(player);
        assertValueEqual(helper, f.weight(), 3, "a backpack's weight");
        assertValueEqual(helper, f.availableFeathers(), 17, "the weight cannot be spent");
        assertValueEqual(helper, FeathersAPI.spend(player, TEST, Stamina.ofFeathers(17)), SpendResult.OK, "spend what is free");
        assertValueEqual(helper, f.stamina(), Stamina.ofFeathers(3), "the reserved feathers stay");
        weight.removeModifier(BACKPACK);
        tick(player, 1);
        assertValueEqual(helper, f.weight(), 0, "the backpack taken off");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strainCapFollowsItsAttribute(GameTestHelper helper) {
        boolean strain = ServerConfig.ENABLE_STRAIN.get();
        try {
            ServerConfig.ENABLE_STRAIN.set(true);
            ServerPlayer player = player(helper);
            AttributeInstance maxStrain = player.getAttribute(FeathersAttributes.MAX_STRAIN.get());
            helper.assertTrue(maxStrain != null, "players have the max strain attribute");
            assertValueEqual(helper, FeathersAPI.spend(player, TEST, Stamina.ofFeathers(28), SpendOptions.DEFAULT.simulated()),
                    SpendResult.INSUFFICIENT, "eight feathers of strain are past the default six");
            maxStrain.addTransientModifier(new AttributeModifier(TRAINING, "test_training", 4.0, AttributeModifier.Operation.ADDITION));
            tick(player, 1);
            FeathersView f = FeathersAPI.get(player);
            assertValueEqual(helper, f.maxStrain(), Stamina.ofFeathers(10), "training raises the strain cap");
            assertValueEqual(helper, FeathersAPI.spend(player, TEST, Stamina.ofFeathers(28)), SpendResult.OK, "spend into the raised strain");
            assertValueEqual(helper, f.strain(), Stamina.ofFeathers(8), "strain");
            tick(player, 1);
            helper.assertTrue(player.hasEffect(FeathersMobEffects.STRAINED.get()), "the strained effect follows the strain");
            maxStrain.removeModifier(TRAINING);
        } finally {
            ServerConfig.ENABLE_STRAIN.set(strain);
        }
        helper.succeed();
    }
}
