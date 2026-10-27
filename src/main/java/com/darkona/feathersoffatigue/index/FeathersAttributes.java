package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FeathersAttributes {

    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, FeathersIds.MOD_ID);

    static {
        register("max_feathers", 20.0, 0.0, 1000.0);
        register("max_strain", 6.0, 0.0, 1000.0);
        register("feathers_per_second", 0.4, -40.0, 40.0);
        register("usage_multiplier", 1.0, 0.0, 40.0);
        register("armor_weight_multiplier", 1.0, 0.0, 10.0);
    }

    private static void register(String name, double defaultValue, double min, double max) {
        ATTRIBUTES.register(name, () -> new RangedAttribute(FeathersIds.MOD_ID + "." + name, defaultValue, min, max).setSyncable(true));
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    private FeathersAttributes() {}
}
