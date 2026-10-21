package com.darkona.feathersoffatigue;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.client.ClientEvents;
import com.darkona.feathersoffatigue.api.registry.FeathersDataMaps;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.climate.ClimateEffects;
import com.darkona.feathersoffatigue.compatibility.dropletsofthirst.DropletsOfThirstCompat;
import com.darkona.feathersoffatigue.compatibility.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.compatibility.curios.CuriosCompat;
import com.darkona.feathersoffatigue.compatibility.lso.LegendarySurvivalCompat;
import com.darkona.feathersoffatigue.compatibility.sereneseasons.SereneSeasonsCompat;
import com.darkona.feathersoffatigue.compatibility.thirst.ThirstCompat;
import com.darkona.feathersoffatigue.compatibility.toughasnails.ToughAsNailsCompat;
import com.darkona.feathersoffatigue.config.FeathersClientConfig;
import com.darkona.feathersoffatigue.config.FeathersCompatConfig;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.core.FeathersAttachments;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import com.darkona.feathersoffatigue.core.FeathersTicker;
import com.darkona.feathersoffatigue.core.HungerRegen;
import com.darkona.feathersoffatigue.effect.ModEffects;
import com.darkona.feathersoffatigue.item.ModItems;
import com.darkona.feathersoffatigue.network.FeathersNetwork;
import com.darkona.feathersoffatigue.registry.ModAttributes;
import com.darkona.feathersoffatigue.registry.ModPotions;
import com.darkona.feathersoffatigue.style.BuiltInFeatherStyles;
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
        // JDLib builds the HUD layer from this at RegisterGuiLayersEvent, so it has to be registered here.
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

        // Built-in extensions go through the same API as other mods' do.
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
