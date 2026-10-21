package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersEnchantments;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.core.FeathersData;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import com.darkona.feathersoffatigue.item.ModItems;
import com.darkona.feathersoffatigue.network.SyncPayload;
import com.darkona.feathersoffatigue.weight.ArmorWeights;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;
import static com.darkona.feathersoffatigue.gametest.TestSupport.tick;

/**
 * Armor weight with the default rules (per material), Lightweight, and the weight multiplier attribute.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class ArmorWeightTests {

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static void wear(ServerPlayer player, ItemStack... pieces) {
        for (int i = 0; i < pieces.length; i++) player.setItemSlot(SLOTS[i], pieces[i]);
        FeathersAPI.recalculateWeight(player);
    }

    private static ItemStack[] iron() {
        return new ItemStack[]{
                new ItemStack(Items.IRON_HELMET),
                new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.IRON_LEGGINGS),
                new ItemStack(Items.IRON_BOOTS)
        };
    }

    /** Runs a test with armor weight enabled, then restores the previous setting. */
    private static void withWeights(GameTestHelper helper, Runnable test) {
        boolean before = FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.get();
        FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.set(true);
        try {
            test.run();
        } finally {
            FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.set(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialRulesWeighEachPiece(GameTestHelper helper) {
        withWeights(helper, () -> {
            helper.assertValueEqual(FeathersAPI.getPieceWeight(new ItemStack(Items.DIAMOND_CHESTPLATE)), 3.0, "diamond chestplate");
            helper.assertValueEqual(FeathersAPI.getPieceWeight(new ItemStack(Items.LEATHER_BOOTS)), 1.0, "leather boots");
            helper.assertValueEqual(FeathersAPI.getPieceWeight(new ItemStack(Items.CARVED_PUMPKIN)), 0.0, "non-armor on the head");

            ServerPlayer player = player(helper);
            wear(player, iron());
            helper.assertValueEqual(FeathersAPI.get(player).weight(), 8, "full iron");
            helper.assertValueEqual(FeathersAPI.get(player).availableFeathers(), 12, "usable feathers under full iron");
            FeathersData data = FeathersServiceImpl.data(player);
            for (int part = ArmorWeights.HEAD; part <= ArmorWeights.FEET; part++) {
                helper.assertValueEqual(data.weightPart(part), 2, "each iron piece's share of the weight");
            }
            helper.assertValueEqual(data.weightPart(ArmorWeights.OTHER), 0, "nothing else weighs");
        });
    }

    @GameTest(template = "empty")
    public static void disabledWeightsWeighNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        wear(player, iron());
        helper.assertValueEqual(FeathersAPI.get(player).weight(), 0, "weight with armor weights off");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lightweightRemovesAQuarterPerLevel(GameTestHelper helper) {
        withWeights(helper, () -> {
            Holder<Enchantment> lightweight = helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(FeathersEnchantments.LIGHTWEIGHT);

            ItemStack[] netherite = {
                    new ItemStack(Items.NETHERITE_HELMET),
                    new ItemStack(Items.NETHERITE_CHESTPLATE),
                    new ItemStack(Items.NETHERITE_LEGGINGS),
                    new ItemStack(Items.NETHERITE_BOOTS)
            };

            for (ItemStack piece : netherite) piece.enchant(lightweight, 3);

            ServerPlayer player = player(helper);
            wear(player, netherite);
            helper.assertValueEqual(FeathersAPI.getPieceWeight(netherite[1]), 1.0, "netherite chestplate with Lightweight III");
            helper.assertValueEqual(FeathersAPI.get(player).weight(), 4, "full netherite with Lightweight III");

            ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
            boots.enchant(lightweight, 3);
            helper.assertValueEqual(FeathersAPI.getPieceWeight(boots), 0.25, "leather boots keep a quarter");
        });
    }

    @GameTest(template = "empty")
    public static void weightMultiplierIsNoticedByItself(GameTestHelper helper) {
        withWeights(helper, () -> {
            ServerPlayer player = player(helper);
            wear(player, iron());
            tick(player, 1);

            Objects.requireNonNull(player.getAttribute(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER))
                    .addTransientModifier(new AttributeModifier(id("test_ring"), -0.5,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

            tick(player, 10);
            helper.assertValueEqual(FeathersAPI.get(player).weight(), 4, "weight with the ring");
            helper.assertValueEqual(FeathersServiceImpl.data(player).weightPart(ArmorWeights.CHEST), 1, "the ring halves each piece's share");
        });
    }

    @GameTest(template = "empty")
    public static void featherRingTooltipNamesItsSlot(GameTestHelper helper) {
        List<Component> tooltip = new ArrayList<>();
        ItemStack ring = new ItemStack(ModItems.FEATHER_RING.get());
        ring.getItem().appendHoverText(ring, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        String where = ModList.get().isLoaded("curios") ? "ring" : "offhand";
        helper.assertTrue(tooltip.size() == 1 && tooltip.getFirst().getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("item.feathers_of_fatigue.feather_ring.tooltip." + where), "the ring's tooltip names where it goes: " + tooltip);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void syncPayloadRoundTrips(GameTestHelper helper) {
        withWeights(helper, () -> {
            ServerPlayer player = player(helper);
            wear(player, iron());
            FeathersAPI.addBonusStamina(player, id("test"), 5000, -1);
            FeathersAPI.spend(player, id("test"), 2000);
            tick(player, 1);

            SyncPayload sent = SyncPayload.of(123456, FeathersServiceImpl.data(player));
            ByteBuf buf = Unpooled.buffer();
            SyncPayload.STREAM_CODEC.encode(buf, sent);
            SyncPayload read = SyncPayload.STREAM_CODEC.decode(buf);
            helper.assertValueEqual(buf.readableBytes(), 0, "bytes left after reading the sync");
            helper.assertTrue(read.entityId() == sent.entityId() && read.stamina() == sent.stamina() && read.maxStamina() == sent.maxStamina()
                    && read.strain() == sent.strain() && read.maxStrain() == sent.maxStrain() && read.bonus() == sent.bonus()
                    && read.weight() == sent.weight() && read.regenDelay() == sent.regenDelay() && read.exhausted() == sent.exhausted()
                    && read.rest() == sent.rest() && Arrays.equals(read.weightSplit(), sent.weightSplit()), "the sync reads back as sent: " + sent + " / " + read);
            helper.assertTrue(sent.weight() > 0 && sent.bonus() > 0 && sent.regenDelay() > 0, "the sync carries weight, bonus and a delay: " + sent);
        });
    }
}
