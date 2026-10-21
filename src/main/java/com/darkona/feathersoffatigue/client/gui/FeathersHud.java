package com.darkona.feathersoffatigue.client.gui;

import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.client.FeatherAnimation;
import com.darkona.feathersoffatigue.api.client.FeatherAnimations;
import com.darkona.feathersoffatigue.api.client.FeatherStyle;
import com.darkona.feathersoffatigue.api.client.FeatherStyles;
import com.darkona.feathersoffatigue.api.client.FeatherVariants;
import com.darkona.feathersoffatigue.client.ClientFeathersData;
import com.darkona.feathersoffatigue.client.SyncedFeathers;
import com.darkona.feathersoffatigue.config.FeathersClientConfig;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.weight.ArmorWeights;
import com.darkona.feathersoffatigue.weight.WeightSplit;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkona.jdlib.api.hud.HudCompat;
import dev.darkona.jdlib.api.hud.HudFrame;
import dev.darkona.jdlib.api.hud.HudGate;
import dev.darkona.jdlib.api.hud.HudSide;
import dev.darkona.jdlib.api.hud.JDHudBars;
import dev.darkona.jdlib.api.hud.VanillaLayer;
import dev.darkona.jdlib.core.hud.BarFade;
import dev.darkona.jdlib.core.hud.GainFlash;
import dev.darkona.jdlib.core.hud.IconMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.client.gui.Icons.*;

public final class FeathersHud {

    public static final ResourceLocation LAYER_ID = id("feathers");
    private static final ResourceLocation ICONS = FeatherVariants.SHEET;
    private static final int ICONS_PER_ROW = 10;
    private static final int FEATHERS_PER_ROW = ICONS_PER_ROW * 2;
    private static final int ROW_HEIGHT = 10;
    private static final int UNSTACKED_BASE = 49;
    private static final int ICON_SIZE = 9;

    private static final ClientFeathersData DATA = ClientFeathersData.INSTANCE;

    private static final int FLASH_TICKS = 18;
    private static final int FLASH_VISIBLE_TICKS = 2;
    private static final IconMotion MOTION = new IconMotion(ICONS_PER_ROW, ROW_HEIGHT);
    private static final BarFade FADE = new BarFade();
    private static final GainFlash FLASH = new GainFlash(FLASH_TICKS, FLASH_VISIBLE_TICKS);

    private static final FeatherStyle FALLBACK = FeatherStyle.opaque(0x00B53A, 0x000000);
    private static final Look OWN = new Look().set(FALLBACK);
    private static final Look STRAIN = new Look().set(FALLBACK);
    private static final Look ENDURANCE = new Look().set(FALLBACK);
    private static final Look ARMOR = new Look().set(FALLBACK);
    private static final Look EMPTY = new Look().set(FALLBACK);
    private static final Look EXHAUSTED = new Look().set(FALLBACK);
    private static final Look TINTED = new Look().set(FALLBACK);
    private static final int OPAQUE = 0xFF000000;

    private FeathersHud() {}

    public static void reset() {
        FLASH.reset();
        FADE.reset();
        MOTION.reset();
    }

    public static void tickAnimations() {
        FLASH.tick(DATA.feathers(), DATA.hasFeathers(), FeathersClientConfig.REGEN_EFFECT.get());

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            refreshStyles(player);
            FeathersView mount = DATA.mount();
            LivingEntity mountEntity = shownMount(player, mount);
            animate(mountEntity != null ? FeatherAnimations.select(mountEntity, mount) : FeatherAnimations.select(player, DATA));
        }

        boolean full = DATA.stamina() >= DATA.maxStamina() && DATA.bonusStamina() == 0 && DATA.strain() == 0;
        FADE.tick(full, FeathersClientConfig.FADE_WHEN_FULL.get(), FeathersClientConfig.FADE_COOLDOWN.get(),
                FeathersClientConfig.FADE_OUT_DURATION.get(), FeathersClientConfig.FADE_IN_DURATION.get());
    }

    private static void animate(FeatherAnimation animation) {
        if (animation == null) {
            MOTION.tick(IconMotion.Kind.NONE, 0f, 0f);
            return;
        }
        IconMotion.Kind kind = switch (animation.kind()) {
            case WAVE -> IconMotion.Kind.WAVE;
            case SHAKE -> IconMotion.Kind.SHAKE;
            case PULSE -> IconMotion.Kind.PULSE;
        };
        MOTION.tick(kind, animation.speed(), animation.amplitude());
    }

    public static JDHudBars.Spec spec(BooleanSupplier stacks, IntSupplier xOffset, IntSupplier yOffset) {
        return new JDHudBars.Spec(LAYER_ID, VanillaLayer.AIR_LEVEL, JDHudBars.Order.ABOVE, HudSide.RIGHT, 0,
                HudGate.standard().and(mc -> DATA.hasFeathers() && DATA.maxStamina() > 0),
                stacks, xOffset, yOffset, UNSTACKED_BASE, FeathersHud::paint);
    }

    public static JDHudBars.Spec spec() {
        return spec(FeathersClientConfig.AFFECTED_BY_RIGHT_HEIGHT::get, FeathersClientConfig.X_OFFSET::get, FeathersClientConfig.Y_OFFSET::get);
    }

    static int reservedHeight(int bonusRows) {
        return ROW_HEIGHT * (1 + bonusRows);
    }

    static int rowX(int edgeX) {
        return edgeX - ICON_SIZE;
    }

    private static int paint(HudFrame frame) {
        GuiGraphics graphics = frame.graphics();
        LocalPlayer player = frame.player();
        int x = rowX(frame.edgeX());
        int y = frame.topY();

        FeathersView mount = DATA.mount();
        LivingEntity vehicle = shownMount(player, mount);
        boolean riding = vehicle != null;
        FeathersView shown = riding ? mount : DATA;
        int bonusRows = bonusRows(shown);

        if (FADE.alpha() > 0) {
            RenderSystem.enableBlend();
            graphics.setColor(1f, 1f, 1f, FADE.alpha());
            if (riding) {
                long tint = FeatherColors.of(vehicle);
                drawRow(graphics, mount, TINTED.colors(OPAQUE | FeatherColors.body(tint), OPAQUE | FeatherColors.outline(tint)), false, vehicle, x, y);
            } else {
                drawRow(graphics, DATA, OWN, true, player, x, y);
            }
            drawBonus(graphics, shown, x, y - ROW_HEIGHT, bonusRows);
            graphics.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        if (FeathersServerConfig.DEBUG_MODE.get()) drawDebug(graphics, Minecraft.getInstance().font, player);
        return reservedHeight(bonusRows);
    }

    private static LivingEntity shownMount(LocalPlayer player, FeathersView mount) {
        return player.getVehicle() instanceof LivingEntity living && mount.hasFeathers() && mount.maxStamina() > 0 ? living : null;
    }

    private static int bonusRows(FeathersView view) {
        return (Stamina.toFeathersCeil(view.bonusStamina()) + FEATHERS_PER_ROW - 1) / FEATHERS_PER_ROW;
    }

    private static void drawRow(GuiGraphics graphics, FeathersView view, Look look, boolean overlay, LivingEntity wearer, int x, int y) {
        int maxFeathers = view.maxFeathers();
        int feathers = view.feathers();
        int body = look.body;
        int border = look.border;

        int backgroundIcons = Math.min(ICONS_PER_ROW, (maxFeathers + 1) / 2);
        drawSlots(graphics, x, y, backgroundIcons, view.exhausted() ? EXHAUSTED : EMPTY);

        int layers = Math.max(1, (feathers + FEATHERS_PER_ROW - 1) / FEATHERS_PER_ROW);
        for (int layer = 0; layer < layers; layer++) {
            int inLayer = Math.min(FEATHERS_PER_ROW, feathers - layer * FEATHERS_PER_ROW);
            int layerBody = layer == 0 ? body : (body & 0xFF000000) | FeatherColors.shade(body & 0xFFFFFF, layer);
            drawFeathers(graphics, x, y, inLayer, layerBody, border, look);
        }

        drawFeathers(graphics, x, y, Math.min(FEATHERS_PER_ROW, Stamina.toFeathersCeil(view.strain())), STRAIN);

        drawWeight(graphics, view, wearer, x, y);

        if (overlay && look.overlay) drawOverlay(graphics, x, y, backgroundIcons, look);

        if (view == DATA && FLASH.visible()) {
            for (int i = 0; i < backgroundIcons; i++) draw(graphics, ICONS, FeatherVariants.SHEET_WIDTH, FeatherVariants.SHEET_HEIGHT, x, y, i, REGEN_OVERLAY_U, 0);
        }

        if (layers > 1) drawRowCount(graphics, x, y, layers);
    }

    private static void drawBonus(GuiGraphics graphics, FeathersView view, int x, int y, int rows) {
        int bonusFeathers = Stamina.toFeathersCeil(view.bonusStamina());
        for (int row = 0; row < rows; row++) {
            int inRow = Math.min(FEATHERS_PER_ROW, bonusFeathers - row * FEATHERS_PER_ROW);
            drawFeathers(graphics, x, y - row * ROW_HEIGHT, inRow, ENDURANCE);
        }
    }

    private static final EquipmentSlot[] WEIGHT_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static void drawWeight(GuiGraphics graphics, FeathersView view, LivingEntity wearer, int x, int y) {
        int weight = Math.min(FEATHERS_PER_ROW, view.weight());
        if (weight <= 0) return;
        if (wearer == null || !(view instanceof SyncedFeathers synced)) {
            drawFeathers(graphics, x, y, weight, ARMOR);
            return;
        }
        int icons = (weight + 1) / 2;
        for (int i = 0; i < icons; i++) {
            int first = partOf(synced.weightSplit(), 2 * i);
            int second = 2 * i + 1 < weight ? partOf(synced.weightSplit(), 2 * i + 1) : -1;
            if (second < 0) {
                drawPart(graphics, x, y, i, true, first, wearer, synced.weightSplit());
            } else {
                drawPart(graphics, x, y, i, false, second, wearer, synced.weightSplit());
                if (second != first) drawPart(graphics, x, y, i, true, first, wearer, synced.weightSplit());
            }
        }
    }

    private static int partOf(WeightSplit split, int feather) {
        int end = 0;
        for (int part = ArmorWeights.HEAD; part <= ArmorWeights.FEET; part++) {
            end += split.part(part);
            if (feather < end) return part;
        }
        int sourceCount = split.sourceCount();
        for (int source = 0; source < sourceCount; source++) {
            end += split.feathers(source);
            if (feather < end) return SOURCE + source;
        }
        return ArmorWeights.OTHER;
    }

    private static final int SOURCE = ArmorWeights.PARTS;

    private static void drawPart(GuiGraphics graphics, int x, int y, int index, boolean half, int part, LivingEntity wearer, WeightSplit split) {
        if (part >= SOURCE) {
            int tint = split.tint(part - SOURCE);
            drawTinted(graphics, x, y, index, half, tint >= 0 ? FeatherColors.ofColor(tint) : FeatherColors.of(BuiltInRegistries.ITEM.byId(-tint - 1)));
            return;
        }
        EquipmentSlot slot = part == ArmorWeights.CHEST && !(wearer instanceof Player) ? EquipmentSlot.BODY
                : part < WEIGHT_SLOTS.length ? WEIGHT_SLOTS[part] : null;
        ItemStack piece = slot != null ? wearer.getItemBySlot(slot) : ItemStack.EMPTY;
        if (piece.isEmpty()) drawIcons(graphics, x, y, index, index + 1, half, ARMOR.body, ARMOR.border, ARMOR);
        else drawTinted(graphics, x, y, index, half, FeatherColors.of(piece));
    }

    private static void drawTinted(GuiGraphics graphics, int x, int y, int index, boolean half, long tint) {
        drawIcons(graphics, x, y, index, index + 1, half, OPAQUE | FeatherColors.body(tint), OPAQUE | FeatherColors.outline(tint), TINTED);
    }

    private static void drawFeathers(GuiGraphics graphics, int x, int y, int count, Look look) {
        drawFeathers(graphics, x, y, count, look.body, look.border, look);
    }

    private static void drawFeathers(GuiGraphics graphics, int x, int y, int count, int body, int border, Look sprites) {
        if (count <= 0) return;
        drawIcons(graphics, x, y, 0, (count + 1) / 2, (count & 1) == 1, body, border, sprites);
    }

    private static void drawIcons(GuiGraphics graphics, int x, int y, int from, int to, boolean halfLast, int body, int border, Look sprites) {
        ResourceLocation sheet = sprites.sheet;
        int width = sprites.sheetWidth;
        int height = sprites.sheetHeight;
        int v = sprites.row * SIZE;
        int halfIcon = halfLast ? to - 1 : -1;
        setColor(graphics, MOTION.brighten(body));
        for (int i = from; i < to; i++) draw(graphics, sheet, width, height, x, y, i, i == halfIcon ? BODY_HALF_U : BODY_FULL_U, v);
        graphics.setColor(1f, 1f, 1f, FADE.alpha());
        for (int i = from; i < to; i++) draw(graphics, sheet, width, height, x, y, i, i == halfIcon ? SHINE_HALF_U : SHINE_FULL_U, v);
        if ((border >>> 24) != 0) {
            setColor(graphics, border);
            for (int i = from; i < to; i++) draw(graphics, sheet, width, height, x, y, i, i == halfIcon ? BORDER_HALF_U : BORDER_FULL_U, v);
            graphics.setColor(1f, 1f, 1f, FADE.alpha());
        }
    }

    private static void drawSlots(GuiGraphics graphics, int x, int y, int icons, Look look) {
        setColor(graphics, look.body);
        for (int i = 0; i < icons; i++) draw(graphics, look.slotSheet, look.slotWidth, look.slotHeight, x, y, i, EMPTY_U, 0);
        if ((look.border >>> 24) != 0) {
            setColor(graphics, look.border);
            for (int i = 0; i < icons; i++) draw(graphics, look.sheet, look.sheetWidth, look.sheetHeight, x, y, i, BORDER_FULL_U, look.row * SIZE);
        }
        graphics.setColor(1f, 1f, 1f, FADE.alpha());
    }

    private static void drawOverlay(GuiGraphics graphics, int x, int y, int icons, Look look) {
        int v = look.overlayRow * SIZE;
        setColor(graphics, look.overlayColor);
        for (int i = 0; i < icons; i++) draw(graphics, look.overlaySheet, look.overlayWidth, look.overlayHeight, x, y, i, OVERLAY_U, v);
        setColor(graphics, look.overlayAccent);
        for (int i = 0; i < icons; i++) draw(graphics, look.overlaySheet, look.overlayWidth, look.overlayHeight, x, y, i, OVERLAY_ACCENT_U, v);
        graphics.setColor(1f, 1f, 1f, FADE.alpha());
    }

    private static void setColor(GuiGraphics graphics, int argb) {
        graphics.setColor(FastColor.ARGB32.red(argb) / 255f, FastColor.ARGB32.green(argb) / 255f, FastColor.ARGB32.blue(argb) / 255f,
                FastColor.ARGB32.alpha(argb) / 255f * FADE.alpha());
    }

    private static void draw(GuiGraphics graphics, ResourceLocation sheet, int width, int height, int x, int y, int index, int u, int v) {
        graphics.blit(sheet, x - index * 8, y + MOTION.offset(index), u, v, SIZE, SIZE, width, height);
    }

    private static void drawRowCount(GuiGraphics graphics, int x, int y, int layers) {
        HudCompat.rowCount(graphics, x, y, layers, 1);
        graphics.setColor(1f, 1f, 1f, FADE.alpha());
    }

    private static void refreshStyles(LocalPlayer player) {
        ResourceLocation chosen = FeatherStyles.select(player, DATA);
        FeatherStyle style = chosen != null ? FeatherStyles.resolve(chosen) : null;
        OWN.set(style != null ? style : style(switch (FeathersClientConfig.FEATHER_COLOR.get()) {
            case GREEN -> FeatherStyles.GREEN;
            case BLUE -> FeatherStyles.BLUE;
            case WHITE -> FeatherStyles.WHITE;
        }));
        STRAIN.set(style(FeatherStyles.STRAIN));
        ENDURANCE.set(style(FeatherStyles.ENDURANCE));
        ARMOR.set(style(FeatherStyles.ARMOR));
        EMPTY.set(style(FeatherStyles.EMPTY));
        EXHAUSTED.set(style(FeatherStyles.EXHAUSTED));
        TINTED.set(FALLBACK);
    }

    private static FeatherStyle style(ResourceLocation id) {
        FeatherStyle style = FeatherStyles.resolve(id);
        return style != null ? style : FALLBACK;
    }

    private static void drawDebug(GuiGraphics graphics, Font font, LocalPlayer player) {
        int line = 2;
        graphics.drawString(font, "Feathers %d/%d  stamina %d/%d  bonus %d  weight %d".formatted(DATA.feathers(), DATA.maxFeathers(),
                DATA.stamina(), DATA.maxStamina(), DATA.bonusStamina(), DATA.weight()), 2, line, 0xFFFFFF);
        line += 10;
        graphics.drawString(font, "Strain %d/%d  regen delay %d  exhausted %s  rest %s".formatted(DATA.strain(), DATA.maxStrain(),
                DATA.regenDelay(), DATA.exhausted(), DATA.restState()), 2, line, 0xFF8080);
        line += 10;
        graphics.drawString(font, "Regen %.2f f/s  usage x%.2f".formatted(FeathersAPI.getRegenPerSecond(player),
                FeathersAPI.getUsageMultiplier(player)), 2, line, 0xDDDD00);
        line += 10;
        ResourceLocation source = DATA.lastSpendSource();
        if (source != null) {
            graphics.drawString(font, "Spent %d on %s".formatted(DATA.lastSpendCost(), source), 2, line, 0x80FF80);
        }
    }
}
