package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.index.FeathersItemRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public class FeatherRingItem extends Item {

    private static final String TOOLTIP = "item.feathers_of_fatigue.feather_ring.tooltip." + (FeathersItemRegistry.CURIOS ? "ring" : "offhand");

    public FeatherRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(TOOLTIP).withStyle(ChatFormatting.GRAY));
    }
}
