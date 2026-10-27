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
import com.darkona.feathersoffatigue.index.FeathersAttachmentTypes;
import com.darkona.feathersoffatigue.index.FeathersDataMaps;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.darkona.feathersoffatigue.content.feathers.HungerRegen;
import com.darkona.feathersoffatigue.index.FeathersMobEffects;
import com.darkona.feathersoffatigue.index.FeathersItems;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.index.FeathersAttributes;
import com.darkona.feathersoffatigue.index.FeathersPotions;
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

        FeathersAttributes.register(modEventBus);
        FeathersMobEffects.register(modEventBus);
        FeathersPotions.register(modEventBus);
        FeathersItems.register(modEventBus);
        FeathersAttachmentTypes.register(modEventBus);
        FeathersDataMaps.register(modEventBus);

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
        if (FeathersItems.CURIOS) event.enqueueWork(CuriosCompat::init);
    }
}
