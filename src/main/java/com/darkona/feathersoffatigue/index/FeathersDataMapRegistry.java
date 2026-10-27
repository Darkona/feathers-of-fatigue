package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import static com.darkona.feathersoffatigue.api.registry.FeathersDataMaps.ARMOR_WEIGHT;
import static com.darkona.feathersoffatigue.api.registry.FeathersDataMaps.MOUNT_STATS;

public final class FeathersDataMapRegistry {

    private FeathersDataMapRegistry() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FeathersDataMapRegistry::registerTypes);
    }

    private static void registerTypes(RegisterDataMapTypesEvent event) {
        event.register(ARMOR_WEIGHT);
        event.register(MOUNT_STATS);
    }
}
