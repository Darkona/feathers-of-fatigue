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
import com.darkona.feathersoffatigue.content.feathers.StaminaHooks;
import com.darkona.feathersoffatigue.index.FeathersAttachmentRegistry;
import dev.darkona.jdlib.api.stat.ResourceCodecs;
import dev.darkona.jdlib.core.stat.Resource;
import dev.darkona.jdlib.core.stat.SourcedPools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;

@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class StaminaResourceTests {

    private static final ResourceLocation TEST = id("test");
    private static final ResourceLocation BACKPACK = id("test_backpack");
    private static final ResourceLocation TRAINING = id("test_training");

    @GameTest(template = "empty")
    public static void theClientsPredictionLandsOnTheServer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        tick(player, 5);
        FeathersAPI.spend(player, TEST, Stamina.ofFeathers(12));
        Resource<ResourceLocation> server = FeathersServiceImpl.stamina(player);
        Resource<ResourceLocation> client = new Resource<>(StaminaHooks.rules());
        ResourceCodecs.Synced.of(server).settle(client);

        int sentStep = server.stored() / Stamina.PER_FEATHER;
        long sentRate = server.rate();
        boolean sentDelayed = server.regenDelay() > 0;
        int syncs = 0;
        int worst = 0;
        for (int t = 0; t < 400; t++) {
            tick(player, 1);
            client.predict(helper.getLevel().getGameTime());
            worst = Math.max(worst, Math.abs(client.stored() - server.stored()));
            int step = server.stored() / Stamina.PER_FEATHER;
            boolean delayed = server.regenDelay() > 0;
            if (step != sentStep || server.rate() != sentRate || delayed != sentDelayed) {
                ResourceCodecs.Synced.of(server).settle(client);
                sentStep = step;
                sentRate = server.rate();
                sentDelayed = delayed;
                syncs++;
            }
        }
        helper.assertTrue(server.stored() > Stamina.ofFeathers(8), "the server regenerated: " + server.stored());
        helper.assertTrue(worst <= 1, "the client's prediction stays on the server's stamina between syncs, off by " + worst);
        helper.assertTrue(syncs < 20, "the server syncs only on whole feathers and rate changes, not every tick: " + syncs);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorWeightIsAnAttributeOtherModsAddTo(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AttributeInstance weight = player.getAttribute(FeathersAttributes.ARMOR_WEIGHT);
        helper.assertTrue(weight != null, "players have the armor weight attribute");
        weight.addTransientModifier(new AttributeModifier(BACKPACK, 3.0, AttributeModifier.Operation.ADD_VALUE));
        tick(player, 1);
        FeathersView f = FeathersAPI.get(player);
        helper.assertValueEqual(f.weight(), 3, "a backpack's weight");
        helper.assertValueEqual(f.availableFeathers(), 17, "the weight cannot be spent");
        helper.assertValueEqual(FeathersAPI.spend(player, TEST, Stamina.ofFeathers(17)), SpendResult.OK, "spend what is free");
        helper.assertValueEqual(f.stamina(), Stamina.ofFeathers(3), "the reserved feathers stay");
        weight.removeModifier(BACKPACK);
        tick(player, 1);
        helper.assertValueEqual(f.weight(), 0, "the backpack taken off");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strainCapFollowsItsAttribute(GameTestHelper helper) {
        boolean strain = ServerConfig.ENABLE_STRAIN.get();
        try {
            ServerConfig.ENABLE_STRAIN.set(true);
            ServerPlayer player = player(helper);
            AttributeInstance maxStrain = player.getAttribute(FeathersAttributes.MAX_STRAIN);
            helper.assertTrue(maxStrain != null, "players have the max strain attribute");
            helper.assertValueEqual(FeathersAPI.spend(player, TEST, Stamina.ofFeathers(28), SpendOptions.DEFAULT.simulated()),
                    SpendResult.INSUFFICIENT, "eight feathers of strain are past the default six");
            maxStrain.addTransientModifier(new AttributeModifier(TRAINING, 4.0, AttributeModifier.Operation.ADD_VALUE));
            tick(player, 1);
            FeathersView f = FeathersAPI.get(player);
            helper.assertValueEqual(f.maxStrain(), Stamina.ofFeathers(10), "training raises the strain cap");
            helper.assertValueEqual(FeathersAPI.spend(player, TEST, Stamina.ofFeathers(28)), SpendResult.OK, "spend into the raised strain");
            helper.assertValueEqual(f.strain(), Stamina.ofFeathers(8), "strain");
            tick(player, 1);
            helper.assertTrue(player.hasEffect(FeathersMobEffects.STRAINED), "the strained effect follows the strain");
            maxStrain.removeModifier(TRAINING);
        } finally {
            ServerConfig.ENABLE_STRAIN.set(strain);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oldSavesMoveIntoTheResource(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        CompoundTag old = new CompoundTag();
        old.putInt("stamina", 7000);
        old.putInt("strain", 1500);
        old.putInt("regen_delay", 30);
        old.putBoolean("exhausted", false);
        ListTag bonuses = new ListTag();
        CompoundTag bonus = new CompoundTag();
        bonus.putString("source", TEST.toString());
        bonus.putInt("amount", 2000);
        bonus.putLong("expires_at", SourcedPools.FOREVER);
        bonuses.add(bonus);
        old.put("bonuses", bonuses);
        CompoundTag counters = new CompoundTag();
        counters.putDouble("test.counter", 4.0);
        old.put("counters", counters);

        FeathersData loaded = new FeathersData();
        loaded.deserializeNBT(helper.getLevel().registryAccess(), old);
        player.setData(FeathersAttachmentRegistry.FEATHERS, loaded);

        FeathersView f = FeathersAPI.get(player);
        helper.assertValueEqual(f.stamina(), 7000, "stamina from the old save");
        helper.assertValueEqual(f.strain(), 1500, "strain from the old save");
        helper.assertValueEqual(f.regenDelay(), 30, "regen delay from the old save");
        helper.assertValueEqual(f.bonusStamina(), 2000, "bonus from the old save");
        helper.assertValueEqual(loaded.getCounter("test.counter"), 4.0, "counters stay with Feathers");
        CompoundTag saved = loaded.serializeNBT(helper.getLevel().registryAccess());
        helper.assertFalse(saved.contains("stamina"), "the new save keeps the stamina in the resource only: " + saved);
        helper.succeed();
    }
}
