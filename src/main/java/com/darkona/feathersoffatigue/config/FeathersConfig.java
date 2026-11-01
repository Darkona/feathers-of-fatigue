package com.darkona.feathersoffatigue.config;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class FeathersConfig {

    private FeathersConfig() {}

    public static void register() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Server.toml");
        context.registerConfig(ModConfig.Type.SERVER, CompatConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Compat.toml");
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Client.toml");
    }
}
