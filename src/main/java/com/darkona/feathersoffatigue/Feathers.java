package com.darkona.feathersoffatigue;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.foundation.events.ClientEvents;
import com.darkona.feathersoffatigue.api.registry.FeathersDataMaps;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.ClimateEffects;
import com.darkona.feathersoffatigue.compat.dropletsofthirst.DropletsOfThirstCompat;
import com.darkona.feathersoffatigue.compat.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.compat.curios.CuriosCompat;
import com.darkona.feathersoffatigue.compat.lso.LegendarySurvivalCompat;
import com.darkona.feathersoffatigue.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.feathersoffatigue.compat.thirst.ThirstCompat;
import com.darkona.feathersoffatigue.compat.toughasnails.ToughAsNailsCompat;
import com.darkona.feathersoffatigue.config.FeathersClientConfig;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersAttachments;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.darkona.feathersoffatigue.content.feathers.HungerRegen;
import com.darkona.feathersoffatigue.content.feathers.ModEffects;
import com.darkona.feathersoffatigue.content.feathers.ModItems;
import com.darkona.feathersoffatigue.network.FeathersNetwork;
import com.darkona.feathersoffatigue.content.feathers.ModAttributes;
import com.darkona.feathersoffatigue.content.feathers.ModPotions;
import com.darkona.feathersoffatigue.content.feathers.BuiltInFeatherStyles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mod(FeathersIds.MOD_ID)
public final class Feathers {

    public static final Logger LOGGER = LoggerFactory.getLogger(FeathersIds.MOD_ID);

    public Feathers(IEventBus modEventBus, ModContainer modContainer) {
        FeathersAPI.setService(FeathersServiceImpl.INSTANCE);

        modContainer.registerConfig(ModConfig.Type.SERVER, FeathersServerConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Server.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, FeathersCompatConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Compat.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, FeathersClientConfig.SPEC, "feathers_of_fatigue/FeathersOfFatigue-Client.toml");
        if (FMLEnvironment.dist == Dist.CLIENT) ClientEvents.registerHud();

        ModAttributes.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModItems.register(modEventBus);
        FeathersAttachments.register(modEventBus);

        modEventBus.addListener(FeathersTicker::addAttributes);
        modEventBus.addListener(FeathersTicker::onConfigChanged);
        modEventBus.addListener(FeathersNetwork::register);
        modEventBus.addListener((RegisterDataMapTypesEvent event) -> {
            event.register(FeathersDataMaps.ARMOR_WEIGHT);
            event.register(FeathersDataMaps.MOUNT_STATS);
        });
        modEventBus.addListener(Feathers::commonSetup);

        ClimateEffects.registerBuiltIn();
        HungerRegen.registerBuiltIn();
        BuiltInFeatherStyles.register();
        ColdSweatCompat.init();
        ThirstCompat.init();
        DropletsOfThirstCompat.init();
        ToughAsNailsCompat.init();
        LegendarySurvivalCompat.init();
        SereneSeasonsCompat.init();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        if (ModItems.CURIOS) event.enqueueWork(CuriosCompat::init);
    }
}
