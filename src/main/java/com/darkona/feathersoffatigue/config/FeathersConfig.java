package com.darkona.feathersoffatigue.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public final class FeathersConfig {

    private FeathersConfig() {}

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, CompatConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Compat.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Client.toml");
    }
}
