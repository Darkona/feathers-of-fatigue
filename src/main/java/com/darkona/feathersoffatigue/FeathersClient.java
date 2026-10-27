package com.darkona.feathersoffatigue;

import com.darkona.feathersoffatigue.api.client.ClientFeathers;
import com.darkona.feathersoffatigue.api.client.FeatherStylePack;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.BuiltInFeatherStyles;
import com.darkona.feathersoffatigue.content.feathers.ClientFeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeatherColors;
import com.darkona.feathersoffatigue.content.feathers.FeathersHud;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.infrastructure.FeathersDebugOverlay;
import dev.darkona.jdlib.api.hud.JDDebugOverlay;
import dev.darkona.jdlib.api.hud.JDHudBars;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@Mod(value = FeathersIds.MOD_ID, dist = Dist.CLIENT)
public final class FeathersClient {

    public FeathersClient(IEventBus modEventBus) {
        JDHudBars.register(FeathersHud.spec());
        modEventBus.addListener(FeathersClient::onClientSetup);
        modEventBus.addListener(FeathersClient::onRegisterGuiLayers);
        modEventBus.addListener(FeathersClient::onRegisterReloadListeners);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ClientFeathers.setService(ClientFeathersData.INSTANCE);
        FeathersServiceImpl.setClientBridge(ClientFeathersData.INSTANCE);
        BuiltInFeatherStyles.registerAnimations();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        JDDebugOverlay.register(event, id("debug"), FeathersDebugOverlay.SPEC);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            FeatherColors.clear();
            FeatherStylePack.PATCHES.load(manager);
        });
    }
}
