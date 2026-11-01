package com.darkona.feathersoffatigue.content.feathers.client;

import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.client.FeatherAnimation;
import com.darkona.feathersoffatigue.api.client.FeatherAnimations;
import com.darkona.feathersoffatigue.api.client.FeatherStyle;
import com.darkona.feathersoffatigue.api.client.FeatherStyles;
import com.darkona.feathersoffatigue.api.client.FeatherVariants;
import com.darkona.feathersoffatigue.content.feathers.ClientFeathersData;
import com.darkona.feathersoffatigue.content.feathers.SyncedFeathers;
import com.darkona.feathersoffatigue.config.ClientConfig;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.feathers.FeatherStylePack;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import com.darkona.feathersoffatigue.content.weight.WeightSplit;
import com.mojang.blaze3d.systems.RenderSystem;
import com.darkona.feathersoffatigue.compat.overflowingbars.OverflowingBarsBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.ModList;

import static com.darkona.feathersoffatigue.content.feathers.client.Icons.*;

public final class FeathersHud {
    public static final String OVERLAY_ID = "feathers";
    private static final ResourceLocation ICONS = FeatherVariants.SHEET;
    private static final int ICONS_PER_ROW = 10;
    private static final int FEATHERS_PER_ROW = ICONS_PER_ROW * 2;
    private static final int ROW_HEIGHT = 10;

    private static boolean overflowingBars = ModList.get().isLoaded("overflowingbars");

    private static final ClientFeathersData DATA = ClientFeathersData.INSTANCE;

    private static final int UNKNOWN_FEATHERS = -1;
    private static int previousFeathers = UNKNOWN_FEATHERS;
    private static int regenFlashTicks;
    private static int fullTicks;
    private static float alpha = 1.0f;

    private static FeatherAnimation.Kind motion;
    private static float motionSpeed;
    private static int motionAmplitude;
    private static float wave;
    private static float shakeClock;
    private static float pulsePhase;

    private static float pulse;

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
        previousFeathers = UNKNOWN_FEATHERS;
        regenFlashTicks = 0;
        fullTicks = 0;
        alpha = 1.0f;
        motion = null;
        wave = 0;
        shakeClock = 0;
        pulsePhase = 0;
        pulse = 0;
    }

    public static void tickAnimations() {
        int feathers = DATA.feathers();

        if (previousFeathers != UNKNOWN_FEATHERS && feathers > previousFeathers && ClientConfig.REGEN_EFFECT.get()
                && regenFlashTicks <= 0) regenFlashTicks = 18;
        previousFeathers = DATA.hasFeathers() ? feathers : UNKNOWN_FEATHERS;
        if (regenFlashTicks > 0) regenFlashTicks--;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            refreshStyles(player);
            FeathersView mount = DATA.mount();
            LivingEntity mountEntity = shownMount(player, mount);
            animate(mountEntity != null ? FeatherAnimations.select(mountEntity, mount) : FeatherAnimations.select(player, DATA));
        }

        fullTicks = DATA.stamina() >= DATA.maxStamina() && DATA.bonusStamina() == 0 && DATA.strain() == 0 ? fullTicks + 1 : 0;
        if (ClientConfig.FADE_WHEN_FULL.get() && fullTicks >= ClientConfig.FADE_COOLDOWN.get()) {
            alpha = Math.max(0f, alpha - 1f / ClientConfig.FADE_OUT_DURATION.get());
        } else {
            alpha = Math.min(1f, alpha + 1f / ClientConfig.FADE_IN_DURATION.get());
        }
    }

    private static void animate(FeatherAnimation animation) {
        FeatherAnimation.Kind kind = animation != null ? animation.kind() : null;
        if (kind != motion) {
            motion = kind;
            wave = 0;
            shakeClock = 0;
            pulsePhase = 0;
        }
        pulse = 0;
        if (kind == null) return;
        motionSpeed = animation.speed();
        motionAmplitude = Math.round(animation.amplitude());
        switch (kind) {
            case WAVE -> wave = wave >= ICONS_PER_ROW * ROW_HEIGHT ? -40 : wave + 2 * motionSpeed;
            case SHAKE -> shakeClock += motionSpeed;
            case PULSE -> {
                pulsePhase = (pulsePhase + motionSpeed * Mth.TWO_PI / 20f) % Mth.TWO_PI;
                pulse = Math.min(1f, animation.amplitude()) * (0.5f - 0.5f * Mth.cos(pulsePhase));
            }
        }
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer() || !(mc.getCameraEntity() instanceof LocalPlayer player)) return;
        if (!DATA.hasFeathers() || DATA.maxStamina() <= 0) return;

        boolean stack = ClientConfig.AFFECTED_BY_RIGHT_HEIGHT.get();
        int x = graphics.guiWidth() / 2 + 91 - 9 + ClientConfig.X_OFFSET.get();
        int y = graphics.guiHeight() - (stack ? gui.rightHeight : 49) + ClientConfig.Y_OFFSET.get();

        FeathersView mount = DATA.mount();
        LivingEntity vehicle = shownMount(player, mount);
        boolean riding = vehicle != null;
        FeathersView shown = riding ? mount : DATA;
        int bonusRows = bonusRows(shown);

        if (stack) gui.rightHeight += ROW_HEIGHT * (1 + bonusRows);

        if (alpha > 0) {
            RenderSystem.enableBlend();
            graphics.setColor(1f, 1f, 1f, alpha);
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

        if (ServerConfig.DEBUG_MODE.get()) drawDebug(graphics, mc.font, player);
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

        if (view == DATA && regenFlashTicks >= 16) {
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

        EquipmentSlot slot = part < WEIGHT_SLOTS.length ? WEIGHT_SLOTS[part] : null;
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
        setColor(graphics, pulse > 0 ? brighten(body) : body);
        for (int i = from; i < to; i++) draw(graphics, sheet, width, height, x, y, i, i == halfIcon ? BODY_HALF_U : BODY_FULL_U, v);
        graphics.setColor(1f, 1f, 1f, alpha);
        for (int i = from; i < to; i++) draw(graphics, sheet, width, height, x, y, i, i == halfIcon ? SHINE_HALF_U : SHINE_FULL_U, v);
        if ((border >>> 24) != 0) {
            setColor(graphics, border);
            for (int i = from; i < to; i++) draw(graphics, sheet, width, height, x, y, i, i == halfIcon ? BORDER_HALF_U : BORDER_FULL_U, v);
            graphics.setColor(1f, 1f, 1f, alpha);
        }
    }

    private static void drawSlots(GuiGraphics graphics, int x, int y, int icons, Look look) {
        setColor(graphics, look.body);
        for (int i = 0; i < icons; i++) draw(graphics, look.slotSheet, look.slotWidth, look.slotHeight, x, y, i, EMPTY_U, 0);
        if ((look.border >>> 24) != 0) {
            setColor(graphics, look.border);
            for (int i = 0; i < icons; i++) draw(graphics, look.sheet, look.sheetWidth, look.sheetHeight, x, y, i, BORDER_FULL_U, look.row * SIZE);
        }
        graphics.setColor(1f, 1f, 1f, alpha);
    }

    private static void drawOverlay(GuiGraphics graphics, int x, int y, int icons, Look look) {
        int v = look.overlayRow * SIZE;
        setColor(graphics, look.overlayColor);
        for (int i = 0; i < icons; i++) draw(graphics, look.overlaySheet, look.overlayWidth, look.overlayHeight, x, y, i, OVERLAY_U, v);
        setColor(graphics, look.overlayAccent);
        for (int i = 0; i < icons; i++) draw(graphics, look.overlaySheet, look.overlayWidth, look.overlayHeight, x, y, i, OVERLAY_ACCENT_U, v);
        graphics.setColor(1f, 1f, 1f, alpha);
    }

    private static void setColor(GuiGraphics graphics, int argb) {
        graphics.setColor(FastColor.ARGB32.red(argb) / 255f, FastColor.ARGB32.green(argb) / 255f, FastColor.ARGB32.blue(argb) / 255f,
                FastColor.ARGB32.alpha(argb) / 255f * alpha);
    }

    private static int brighten(int argb) {
        int r = FastColor.ARGB32.red(argb);
        int g = FastColor.ARGB32.green(argb);
        int b = FastColor.ARGB32.blue(argb);
        r += (int) ((255 - r) * pulse);
        g += (int) ((255 - g) * pulse);
        b += (int) ((255 - b) * pulse);
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(argb), r, g, b);
    }

    private static void draw(GuiGraphics graphics, ResourceLocation sheet, int width, int height, int x, int y, int index, int u, int v) {
        graphics.blit(sheet, x - index * 8, y + offset(index), u, v, SIZE, SIZE, width, height);
    }

    private static int offset(int index) {
        if (motion == FeatherAnimation.Kind.WAVE) {
            return wave > index * ROW_HEIGHT && wave < (index + 1) * ROW_HEIGHT ? -motionAmplitude : 0;
        }
        if (motion == FeatherAnimation.Kind.SHAKE && motionAmplitude > 0) {
            int h = ((int) shakeClock * 0x9E3779B9) ^ (index * 0x85EBCA6B);
            h ^= h >>> 15;
            h *= 0x2C1B3C6D;
            h ^= h >>> 12;
            return (h & 0x7FFFFFFF) % (motionAmplitude + 1);
        }
        return 0;
    }

    private static final String[] ROW_COUNTS = new String[64];

    static {
        for (int i = 0; i < ROW_COUNTS.length; i++) ROW_COUNTS[i] = "x" + i;
    }

    private static void drawRowCount(GuiGraphics graphics, int x, int y, int layers) {
        Font font = Minecraft.getInstance().font;
        if (overflowingBars) {
            try {
                OverflowingBarsBridge.drawRowCount(graphics, x + 18, y, layers, font);
                graphics.setColor(1f, 1f, 1f, alpha);
                return;
            } catch (LinkageError e) {
                overflowingBars = false;
                Feathers.LOGGER.warn("Overflowing Bars' row count renderer isn't compatible, using the built-in one", e);
            }
        }
        graphics.drawString(font, layers < ROW_COUNTS.length ? ROW_COUNTS[layers] : "x" + layers, x + 11, y + 1, 0xFFFFFF);
    }

    private static void refreshStyles(LocalPlayer player) {
        ResourceLocation chosen = FeatherStyles.select(player, DATA);
        FeatherStyle style = chosen != null ? FeatherStylePack.resolve(chosen) : null;
        OWN.set(style != null ? style : style(switch (ClientConfig.FEATHER_COLOR.get()) {
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
        FeatherStyle style = FeatherStylePack.resolve(id);
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
