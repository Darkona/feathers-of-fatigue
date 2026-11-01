package com.darkona.feathersoffatigue.api.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

/**
 * The feathers attributes. Change them with ordinary attribute modifiers (items, effects, Curios, commands).
 */
public final class FeathersAttributes {

    /** Maximum feathers. */
    public static final RegistryObject<Attribute> MAX_FEATHERS = RegistryObject.create(id("max_feathers"), ForgeRegistries.ATTRIBUTES);

    /** Maximum strain, in feathers. */
    public static final RegistryObject<Attribute> MAX_STRAIN = RegistryObject.create(id("max_strain"), ForgeRegistries.ATTRIBUTES);

    /** Regeneration, in feathers per second. */
    public static final RegistryObject<Attribute> FEATHERS_PER_SECOND = RegistryObject.create(id("feathers_per_second"), ForgeRegistries.ATTRIBUTES);

    /** Multiplies every cost. 1.0 = normal. */
    public static final RegistryObject<Attribute> USAGE_MULTIPLIER = RegistryObject.create(id("usage_multiplier"), ForgeRegistries.ATTRIBUTES);

    /** Multiplies armor weight. A value of 1.0 is normal. A MULTIPLY_BASE modifier of -0.5 halves it. */
    public static final RegistryObject<Attribute> ARMOR_WEIGHT_MULTIPLIER = RegistryObject.create(id("armor_weight_multiplier"), ForgeRegistries.ATTRIBUTES);

    /**
     * Armor weight, in feathers: the part of the maximum that cannot be spent. Feathers adds the weight of the worn armor and of
     * every {@code WeightSource} as its own modifier; any ADDITION modifier adds weight (a backpack), and multipliers scale it.
     */
    public static final RegistryObject<Attribute> ARMOR_WEIGHT = RegistryObject.create(id("armor_weight"), ForgeRegistries.ATTRIBUTES);

    private FeathersAttributes() {}
}
