package com.darkona.feathersoffatigue.foundation.events;

import com.darkona.feathersoffatigue.content.feathers.ClientFeathersData;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.client.ClientFeathers;
import com.darkona.feathersoffatigue.api.client.FeatherStylePack;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.content.feathers.BasicExertion;
import com.darkona.feathersoffatigue.content.feathers.FeatherColors;
import com.darkona.feathersoffatigue.infrastructure.FeathersDebugOverlay;
import com.darkona.feathersoffatigue.content.feathers.FeathersHud;
import com.darkona.feathersoffatigue.config.ClientConfig;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.BuiltInFeatherStyles;
import dev.darkona.jdlib.api.hud.JDDebugOverlay;
import dev.darkona.jdlib.api.hud.JDHudBars;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@EventBusSubscriber(modid = FeathersIds.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final ResourceLocation FEATHER_FONT = id("feather_font");
    private static final Style FEATHER_ICONS = Style.EMPTY.withFont(FEATHER_FONT);

    private static boolean wasCold;

    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientFeathers.setService(ClientFeathersData.INSTANCE);
        FeathersServiceImpl.setClientBridge(ClientFeathersData.INSTANCE);
        BuiltInFeatherStyles.registerAnimations();
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        JDDebugOverlay.register(event, id("debug"), FeathersDebugOverlay.SPEC);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            FeatherColors.clear();
            FeatherStylePack.PATCHES.load(manager);
        });
    }

    public static void registerHud() {
        JDHudBars.register(FeathersHud.spec());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ClientFeathersData.INSTANCE.tick();
        FeathersHud.tickAnimations();

        boolean cold = FeathersAPI.isCold(player);
        if (cold && !wasCold && ClientConfig.FROST_SOUND.get()) {
            player.playSound(SoundEvents.PLAYER_HURT_FREEZE, 0.6f, 1.2f);
        }
        wasCold = cold;

        if (BasicExertion.isActive() && player.isSprinting() && ClientFeathersData.INSTANCE.exhausted() && !player.getAbilities().instabuild) {
            player.setSprinting(false);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFeathersData.INSTANCE.clear();
        FeathersHud.reset();
    }

    private static final String[] WEIGHT_ICONS = new String[41];

    private static String weightIcons(int halves) {
        if (halves < WEIGHT_ICONS.length && WEIGHT_ICONS[halves] != null) return WEIGHT_ICONS[halves];
        StringBuilder icons = new StringBuilder();
        for (int i = 2; i <= halves + 1; i += 2) {
            icons.append(i - 1 == halves ? "b" : "a ");
        }
        String built = icons.reverse().toString();
        if (halves < WEIGHT_ICONS.length) WEIGHT_ICONS[halves] = built;
        return built;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ArmorItem) || !ClientConfig.DISPLAY_WEIGHTS.get()
                || !ServerConfig.ENABLE_ARMOR_WEIGHTS.get()) return;

        double weight = FeathersAPI.getPieceWeight(stack);
        if (weight <= 0) return;

        if (ClientConfig.VISUAL_WEIGHTS.get()) {
            int halves = Math.max(1, (int) Math.round(weight));
            event.getToolTip().add(Component.literal(weightIcons(halves)).withStyle(FEATHER_ICONS));
        } else {
            String shown = weight == Math.rint(weight) ? Integer.toString((int) weight) : "%.1f".formatted(weight);
            event.getToolTip().add(Component.translatable("text.feathers_of_fatigue.tooltip", shown).withStyle(ChatFormatting.BLUE));
        }
    }
}
