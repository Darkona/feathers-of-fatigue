package com.darkona.feathersoffatigue;

import com.darkona.feathersoffatigue.api.client.ClientFeathers;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.BuiltInFeatherStyles;
import com.darkona.feathersoffatigue.content.feathers.ClientFeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeatherStylePack;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.client.FeatherColors;
import com.darkona.feathersoffatigue.content.feathers.client.FeathersHud;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FeathersIds.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FeathersClient {

    private FeathersClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientFeathers.setService(ClientFeathersData.INSTANCE);
        FeathersServiceImpl.setClientBridge(ClientFeathersData.INSTANCE);
        BuiltInFeatherStyles.registerAnimations();
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            FeatherColors.clear();
            FeatherStylePack.load(manager);
        });
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), FeathersHud.OVERLAY_ID, FeathersHud::render);
    }
}
