package com.darkona.feathersoffatigue;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.ClimateEffects;
import com.darkona.feathersoffatigue.compat.dropletsofthirst.DropletsOfThirstCompat;
import com.darkona.feathersoffatigue.compat.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.compat.curios.CuriosCompat;
import com.darkona.feathersoffatigue.compat.lso.LegendarySurvivalCompat;
import com.darkona.feathersoffatigue.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.feathersoffatigue.compat.thirst.ThirstCompat;
import com.darkona.feathersoffatigue.compat.toughasnails.ToughAsNailsCompat;
import com.darkona.feathersoffatigue.config.FeathersConfig;
import com.darkona.feathersoffatigue.index.FeathersAttachmentRegistry;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.darkona.feathersoffatigue.content.feathers.HungerRegen;
import com.darkona.feathersoffatigue.index.FeathersEffectRegistry;
import com.darkona.feathersoffatigue.index.FeathersItemRegistry;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.index.FeathersAttributeRegistry;
import com.darkona.feathersoffatigue.index.FeathersPotionRegistry;
import com.darkona.feathersoffatigue.content.feathers.BuiltInFeatherStyles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mod(FeathersIds.MOD_ID)
public final class Feathers {

    public static final Logger LOGGER = LoggerFactory.getLogger(FeathersIds.MOD_ID);

    public Feathers(IEventBus modEventBus, ModContainer modContainer) {
        FeathersAPI.setService(FeathersServiceImpl.INSTANCE);

        FeathersConfig.register(modContainer);

        FeathersAttributeRegistry.register(modEventBus);
        FeathersEffectRegistry.register(modEventBus);
        FeathersPotionRegistry.register(modEventBus);
        FeathersItemRegistry.register(modEventBus);
        FeathersAttachmentRegistry.register(modEventBus);
        FeathersDataMapRegistry.register(modEventBus);

        modEventBus.addListener(FeathersTicker::addAttributes);
        modEventBus.addListener(FeathersTicker::onConfigChanged);
        modEventBus.addListener(FeathersPackets::register);
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
        if (FeathersItemRegistry.CURIOS) event.enqueueWork(CuriosCompat::init);
    }
}
