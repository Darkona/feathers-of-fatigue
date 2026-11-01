package com.darkona.feathersoffatigue.compat.curios;

import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.index.FeathersItemRegistry;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public final class CuriosCompat {
    private CuriosCompat() {}

    public static void init() {
        CuriosApi.registerCurio(FeathersItemRegistry.FEATHER_RING.get(), new ICurioItem() {
            @Override
            public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
                return ImmutableMultimap.of(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER.get(),
                        new AttributeModifier(uuid, "feathers_of_fatigue:feather_ring", FeathersItemRegistry.FEATHER_RING_WEIGHT_MULTIPLIER, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        });
    }
}
