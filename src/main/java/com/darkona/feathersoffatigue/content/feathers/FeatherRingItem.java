package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.index.FeathersItemRegistry;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public class FeatherRingItem extends Item {

    private static final String TOOLTIP = "item.feathers_of_fatigue.feather_ring.tooltip." + (FeathersItemRegistry.CURIOS ? "ring" : "offhand");

    private Multimap<Attribute, AttributeModifier> offhandModifiers;

    public FeatherRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        if (FeathersItemRegistry.CURIOS || slot != EquipmentSlot.OFFHAND) return super.getDefaultAttributeModifiers(slot);
        if (offhandModifiers == null) {
            offhandModifiers = ImmutableMultimap.of(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER.get(),
                    new AttributeModifier(FeathersTicker.modifierId(id("feather_ring")), "feathers_of_fatigue:feather_ring",
                            FeathersItemRegistry.FEATHER_RING_WEIGHT_MULTIPLIER, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        return offhandModifiers;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(TOOLTIP).withStyle(ChatFormatting.GRAY));
    }
}
