package com.darkona.feathersoffatigue.api.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.registries.DeferredHolder;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

/**
 * The feathers attributes. Change them with ordinary attribute modifiers (items, effects, Curios, commands).
 */
public final class FeathersAttributes {

    /** Maximum feathers. */
    public static final DeferredHolder<Attribute, Attribute> MAX_FEATHERS = DeferredHolder.create(Registries.ATTRIBUTE, id("max_feathers"));

    /** Maximum strain, in feathers. */
    public static final DeferredHolder<Attribute, Attribute> MAX_STRAIN = DeferredHolder.create(Registries.ATTRIBUTE, id("max_strain"));

    /** Regeneration, in feathers per second. */
    public static final DeferredHolder<Attribute, Attribute> FEATHERS_PER_SECOND = DeferredHolder.create(Registries.ATTRIBUTE, id("feathers_per_second"));

    /** Multiplies every cost. 1.0 = normal. */
    public static final DeferredHolder<Attribute, Attribute> USAGE_MULTIPLIER = DeferredHolder.create(Registries.ATTRIBUTE, id("usage_multiplier"));

    /** Multiplies armor weight. A value of 1.0 is normal. An ADD_MULTIPLIED_BASE modifier of -0.5 halves it. */
    public static final DeferredHolder<Attribute, Attribute> ARMOR_WEIGHT_MULTIPLIER = DeferredHolder.create(Registries.ATTRIBUTE, id("armor_weight_multiplier"));

    /**
     * Armor weight, in feathers: the part of the maximum that cannot be spent. Feathers adds the weight of the worn armor and of
     * every {@code WeightSource} as its own modifier; any ADD_VALUE modifier adds weight (a backpack), and multipliers scale it.
     */
    public static final DeferredHolder<Attribute, Attribute> ARMOR_WEIGHT = DeferredHolder.create(Registries.ATTRIBUTE, id("armor_weight"));

    private FeathersAttributes() {}
}
