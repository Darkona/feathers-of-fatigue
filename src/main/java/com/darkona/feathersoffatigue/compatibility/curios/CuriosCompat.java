package com.darkona.feathersoffatigue.compatibility.curios;

import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.item.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CuriosCompat {

    private CuriosCompat() {}

    public static void init() {
        CuriosApi.registerCurio(ModItems.FEATHER_RING.get(), new ICurioItem() {
            @Override
            public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
                return ImmutableMultimap.of(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER,
                        new AttributeModifier(id, ModItems.FEATHER_RING_WEIGHT_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        });
    }
}
