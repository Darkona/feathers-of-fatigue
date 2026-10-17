package com.darkona.feathersoffatigue.client;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.client.ClientFeathers;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.basic.BasicExertion;
import com.darkona.feathersoffatigue.client.gui.FeatherColors;
import com.darkona.feathersoffatigue.client.gui.FeathersHud;
import com.darkona.feathersoffatigue.config.FeathersClientConfig;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import com.darkona.feathersoffatigue.style.FeatherStylePack;
import com.darkona.feathersoffatigue.style.BuiltInFeatherAnimations;
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
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@EventBusSubscriber(modid = FeathersIds.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private static final ResourceLocation FEATHER_FONT = id("feather_font");
    /** The weight tooltip's icons, in the feather font: built once, since a tooltip is rebuilt every frame. */
    private static final Style FEATHER_ICONS = Style.EMPTY.withFont(FEATHER_FONT);

    private static boolean wasCold;

    private ClientEvents() {}

    /* Mod bus */

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientFeathers.setService(ClientFeathersData.INSTANCE);
        FeathersServiceImpl.setClientBridge(ClientFeathersData.INSTANCE);
        // Its triggers read the client config: registered here, not with the styles in common setup.
        BuiltInFeatherAnimations.register();
    }

    /** Feather colors come from textures and resource packs' feather_styles.json: reread them when packs change. */
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            FeatherColors.clear();
            FeatherStylePack.load(manager);
        });
    }

    /** Called from the mod constructor, before JDLib closes its bar list at {@code RegisterGuiLayersEvent}. */
    public static void registerHud() {
        JDHudBars.register(FeathersHud.spec());
    }

    /* Game bus */

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ClientFeathersData.INSTANCE.tick();
        FeathersHud.tickAnimations();

        boolean cold = FeathersAPI.isCold(player);
        if (cold && !wasCold && FeathersClientConfig.FROST_SOUND.get()) {
            player.playSound(SoundEvents.PLAYER_HURT_FREEZE, 0.6f, 1.2f);
        }
        wasCold = cold;

        // Sprinting is decided on the client, so the client is where running out has to stop it.
        if (BasicExertion.isActive() && player.isSprinting() && ClientFeathersData.INSTANCE.exhausted() && !player.getAbilities().instabuild) {
            player.setSprinting(false);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFeathersData.INSTANCE.clear();
        FeathersHud.reset();
    }

    /** Icon strings by weight in half icons: a tooltip is rebuilt every frame while hovered. */
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

    /**
     * Shows how much an armor piece weighs, counting its enchantments.
     */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ArmorItem) || !FeathersClientConfig.DISPLAY_WEIGHTS.get()
                || !FeathersServerConfig.ENABLE_ARMOR_WEIGHTS.get()) return;

        double weight = FeathersAPI.getPieceWeight(stack);
        if (weight <= 0) return;

        if (FeathersClientConfig.VISUAL_WEIGHTS.get()) {
            // The feather font maps 'a' to a full feather icon and 'b' to a half one; a weight point is half an icon.
            int halves = Math.max(1, (int) Math.round(weight));
            event.getToolTip().add(Component.literal(weightIcons(halves)).withStyle(FEATHER_ICONS));
        } else {
            String shown = weight == Math.rint(weight) ? Integer.toString((int) weight) : "%.1f".formatted(weight);
            event.getToolTip().add(Component.translatable("text.feathers_of_fatigue.tooltip", shown).withStyle(ChatFormatting.BLUE));
        }
    }
}
