package com.darkona.feathersoffatigue;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.ClimateEffects;
import com.darkona.feathersoffatigue.compat.coldsweat.ColdSweatCompat;
import com.darkona.feathersoffatigue.compat.curios.CuriosCompat;
import com.darkona.feathersoffatigue.compat.dropletsofthirst.DropletsOfThirstCompat;
import com.darkona.feathersoffatigue.compat.lso.LegendarySurvivalCompat;
import com.darkona.feathersoffatigue.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.feathersoffatigue.compat.thirst.ThirstCompat;
import com.darkona.feathersoffatigue.compat.toughasnails.ToughAsNailsCompat;
import com.darkona.feathersoffatigue.config.FeathersConfig;
import com.darkona.feathersoffatigue.index.FeathersAttachmentRegistry;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.darkona.feathersoffatigue.content.feathers.HungerRegen;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import com.darkona.feathersoffatigue.index.FeathersEffectRegistry;
import com.darkona.feathersoffatigue.index.FeathersItemRegistry;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.index.FeathersAttributeRegistry;
import com.darkona.feathersoffatigue.index.FeathersEnchantmentRegistry;
import com.darkona.feathersoffatigue.index.FeathersPotionRegistry;
import com.darkona.feathersoffatigue.content.feathers.BuiltInFeatherStyles;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FeathersIds.MOD_ID)
public final class Feathers {
    public static final Logger LOGGER = LoggerFactory.getLogger(FeathersIds.MOD_ID);

    public Feathers() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        FeathersAPI.setService(FeathersServiceImpl.INSTANCE);

        FeathersConfig.register();

        FeathersAttributeRegistry.register(modEventBus);
        FeathersEffectRegistry.register(modEventBus);
        FeathersEnchantmentRegistry.register(modEventBus);
        FeathersPotionRegistry.register(modEventBus);
        FeathersItemRegistry.register(modEventBus);
        FeathersAttachmentRegistry.register(modEventBus);

        modEventBus.addListener(FeathersTicker::addAttributes);
        modEventBus.addListener(FeathersTicker::onConfigChanged);
        modEventBus.addListener(Feathers::commonSetup);
        FeathersPackets.register();

        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new FeathersDataMapRegistry.Loader(event.getConditionContext())));
        MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> event.getPlayers().forEach(FeathersPackets::sendDataMaps));

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
        event.enqueueWork(() -> {
            FeathersEffectRegistry.bindAttributeModifiers();
            FeathersPotionRegistry.registerBrewingRecipes();
            if (FeathersItemRegistry.CURIOS) CuriosCompat.init();
        });
    }
}
