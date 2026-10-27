package com.darkona.feathersoffatigue.core;

import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import dev.darkona.jdlib.api.compat.Bridges;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class ModItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FeathersIds.MOD_ID);

    public static final double FEATHER_RING_WEIGHT_MULTIPLIER = -0.5;

    public static final boolean CURIOS = Bridges.isLoaded("curios");

    public static final DeferredItem<Item> FEATHER_RING = ITEMS.registerItem("feather_ring", FeatherRingItem::new, featherRingProperties());

    private static Item.Properties featherRingProperties() {
        Item.Properties properties = new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON);
        if (!CURIOS) {
            properties.attributes(ItemAttributeModifiers.builder()
                    .add(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER,
                            new AttributeModifier(id("feather_ring"), FEATHER_RING_WEIGHT_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                            EquipmentSlotGroup.OFFHAND)
                    .build());
        }
        return properties;
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(FEATHER_RING);
        }
    }

    private ModItems() {}

    public static class FeatherRingItem extends Item {

        private static final String TOOLTIP = "item.feathers_of_fatigue.feather_ring.tooltip." + (ModItems.CURIOS ? "ring" : "offhand");

        public FeatherRingItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
            tooltip.add(Component.translatable(TOOLTIP).withStyle(ChatFormatting.GRAY));
        }
    }
}
