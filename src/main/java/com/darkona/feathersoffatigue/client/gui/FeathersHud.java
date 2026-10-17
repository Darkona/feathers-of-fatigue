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
import com.darkona.feathersoffatigue.style.FeatherStylePack;
import com.darkona.feathersoffatigue.weight.ArmorWeights;
import com.darkona.feathersoffatigue.weight.WeightSplit;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkona.jdlib.api.hud.HudCompat;
import dev.darkona.jdlib.api.hud.HudFrame;
import dev.darkona.jdlib.api.hud.HudGate;
import dev.darkona.jdlib.api.hud.HudSide;
import dev.darkona.jdlib.api.hud.JDHudBars;
import dev.darkona.jdlib.api.hud.VanillaLayer;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.client.gui.Icons.*;

/**
 * Draws the feather row above the food bar and with other right-side bars. Each icon contains two feathers. Beyond a
 * full row, extra feathers use layered overflow colors and a row count. Gray icons show armor weight, red icons show
 * strain, and golden rows show bonus stamina. While riding a supported mount, the row displays the mount's stamina.
 * <p>
 * Each feather uses grayscale sprites tinted with its style's body and border colors. A style can also add a row
 * overlay. Providers or the client setting select the player's style. Fixed ids select the other styles. The HUD
 * resolves styles and animation once per client tick.
 */
public final class FeathersHud {

    public static final ResourceLocation LAYER_ID = id("feathers");
    private static final ResourceLocation ICONS = FeatherVariants.SHEET;
    private static final int ICONS_PER_ROW = 10;
    private static final int FEATHERS_PER_ROW = ICONS_PER_ROW * 2;
    private static final int ROW_HEIGHT = 10;
    /** The height of the stack the row sits on when it does not follow the right-hand stack. */
    private static final int UNSTACKED_BASE = 49;
    private static final int ICON_SIZE = 9;

    private static final ClientFeathersData DATA = ClientFeathersData.INSTANCE;

    /* Animation state, advanced once per client tick by tickAnimations and cleared by reset. */
    private static final int UNKNOWN_FEATHERS = -1;
    private static int previousFeathers = UNKNOWN_FEATHERS;
    private static int regenFlashTicks;
    private static int fullTicks;
    private static float alpha = 1.0f;
    /** The row's animation: its kind (null for none), and where it is. */
    private static FeatherAnimation.Kind motion;
    private static float motionSpeed;
    private static int motionAmplitude;
    private static float wave;
    private static float shakeClock;
    private static float pulsePhase;
    /** How far toward white the feathers are brightened this tick (PULSE). */
    private static float pulse;

    /* Styles, resolved once per client tick by refreshStyles. */
    private static final FeatherStyle FALLBACK = FeatherStyle.opaque(0x00B53A, 0x000000);
    private static final Look OWN = new Look().set(FALLBACK);
    private static final Look STRAIN = new Look().set(FALLBACK);
    private static final Look ENDURANCE = new Look().set(FALLBACK);
    private static final Look ARMOR = new Look().set(FALLBACK);
    private static final Look EMPTY = new Look().set(FALLBACK);
    private static final Look EXHAUSTED = new Look().set(FALLBACK);
    /** The plain feather, recolored per draw for texture-tinted feathers. */
    private static final Look TINTED = new Look().set(FALLBACK);
    /** FeatherColors' pairs are RGB: full alpha on top. */
    private static final int OPAQUE = 0xFF000000;

    private FeathersHud() {}

    /**
     * Back to a bar never drawn, on leaving a world: the next one starts without a regeneration flash or a fade.
     */
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
        // The first sync is no regeneration: the flash waits for a count the client already had.
        if (previousFeathers != UNKNOWN_FEATHERS && feathers > previousFeathers && FeathersClientConfig.REGEN_EFFECT.get()
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
        if (FeathersClientConfig.FADE_WHEN_FULL.get() && fullTicks >= FeathersClientConfig.FADE_COOLDOWN.get()) {
            alpha = Math.max(0f, alpha - 1f / FeathersClientConfig.FADE_OUT_DURATION.get());
        } else {
            alpha = Math.min(1f, alpha + 1f / FeathersClientConfig.FADE_IN_DURATION.get());
        }
    }

    /** Starts, keeps or stops the row's animation and moves it one tick on. */
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
            // Past the row's end, a short pause before it starts again.
            case WAVE -> wave = wave >= ICONS_PER_ROW * ROW_HEIGHT ? -40 : wave + 2 * motionSpeed;
            case SHAKE -> shakeClock += motionSpeed;
            case PULSE -> {
                pulsePhase = (pulsePhase + motionSpeed * Mth.TWO_PI / 20f) % Mth.TWO_PI;
                pulse = Math.min(1f, animation.amplitude()) * (0.5f - 0.5f * Mth.cos(pulsePhase));
            }
        }
    }

    /**
     * The registration of the bar for JDLib: after air, on the right-hand stack, where thirst mods such as Thirst Was
     * Taken draw their bar. The gate is JDLib's standard one plus having feathers.
     */
    public static JDHudBars.Spec spec(BooleanSupplier stacks, IntSupplier xOffset, IntSupplier yOffset) {
        return new JDHudBars.Spec(LAYER_ID, VanillaLayer.AIR_LEVEL, JDHudBars.Order.ABOVE, HudSide.RIGHT, 0,
                HudGate.standard().and(mc -> DATA.hasFeathers() && DATA.maxStamina() > 0),
                stacks, xOffset, yOffset, UNSTACKED_BASE, FeathersHud::paint);
    }

    /** The registration with the client config's values. */
    public static JDHudBars.Spec spec() {
        return spec(FeathersClientConfig.AFFECTED_BY_RIGHT_HEIGHT::get, FeathersClientConfig.X_OFFSET::get, FeathersClientConfig.Y_OFFSET::get);
    }

    /** What the bar takes of the right-hand stack: its row and the bonus rows, also while faded out. */
    static int reservedHeight(int bonusRows) {
        return ROW_HEIGHT * (1 + bonusRows);
    }

    /** The row's left icon x: JDLib's edge is the right end of the 9 px icon that is the last of the row. */
    static int rowX(int edgeX) {
        return edgeX - ICON_SIZE;
    }

    private static int paint(HudFrame frame) {
        GuiGraphics graphics = frame.graphics();
        LocalPlayer player = frame.player();
        int x = rowX(frame.edgeX());
        int y = frame.topY();

        // Riding a mount that has feathers, only the mount's matter: they replace the rider's, in the mount's colors.
        FeathersView mount = DATA.mount();
        LivingEntity vehicle = shownMount(player, mount);
        boolean riding = vehicle != null;
        FeathersView shown = riding ? mount : DATA;
        int bonusRows = bonusRows(shown);

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

        if (FeathersServerConfig.DEBUG_MODE.get()) drawDebug(graphics, Minecraft.getInstance().font, player);
        // Reserved while faded out so the bars above remain stable.
        return reservedHeight(bonusRows);
    }

    /** The creature the player rides, when its feathers are known and replace the player's row; else null. */
    private static LivingEntity shownMount(LocalPlayer player, FeathersView mount) {
        return player.getVehicle() instanceof LivingEntity living && mount.hasFeathers() && mount.maxStamina() > 0 ? living : null;
    }

    private static int bonusRows(FeathersView view) {
        return (Stamina.toFeathersCeil(view.bonusStamina()) + FEATHERS_PER_ROW - 1) / FEATHERS_PER_ROW;
    }

    /**
     * One row of feathers in {@code look} (its overlay only when {@code overlay}). {@code wearer} colors the armor weight
     * by piece. A null wearer leaves the weight gray.
     */
    private static void drawRow(GuiGraphics graphics, FeathersView view, Look look, boolean overlay, LivingEntity wearer, int x, int y) {
        int maxFeathers = view.maxFeathers();
        int feathers = view.feathers();
        int body = look.body;
        int border = look.border;

        // Empty slots up to the maximum (one row; higher maximums are layered over it). Reddish while exhausted.
        int backgroundIcons = Math.min(ICONS_PER_ROW, (maxFeathers + 1) / 2);
        drawSlots(graphics, x, y, backgroundIcons, view.exhausted() ? EXHAUSTED : EMPTY);

        // Feathers, layered: the first row in its own color, every further row over it in a deeper shade of that color
        // (a lighter one for dark colors), outlined like the first.
        int layers = Math.max(1, (feathers + FEATHERS_PER_ROW - 1) / FEATHERS_PER_ROW);
        for (int layer = 0; layer < layers; layer++) {
            int inLayer = Math.min(FEATHERS_PER_ROW, feathers - layer * FEATHERS_PER_ROW);
            int layerBody = layer == 0 ? body : (body & 0xFF000000) | FeatherColors.shade(body & 0xFFFFFF, layer);
            drawFeathers(graphics, x, y, inLayer, layerBody, border, look);
        }

        // Strain: red feathers growing over the empty row.
        drawFeathers(graphics, x, y, Math.min(FEATHERS_PER_ROW, Stamina.toFeathersCeil(view.strain())), STRAIN);

        // Armor weight: the first feathers are held back; spending stops when it reaches them.
        drawWeight(graphics, view, wearer, x, y);

        // Frost, flames: over the whole row.
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

    /**
     * Armor weight from the right, head to feet: each piece's share in that piece's colors (leather in its dye), and
     * weight from other sources in gray.
     */
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

    /**
     * Which share the {@code feather}-th weight feather belongs to, in drawing order: the armor pieces
     * (ArmorWeights.HEAD to FEET), then the colored weight sources ({@link #SOURCE} + index), then the rest
     * (ArmorWeights.OTHER).
     */
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

    /** Shares from here on are colored weight sources. */
    private static final int SOURCE = ArmorWeights.PARTS;

    private static void drawPart(GuiGraphics graphics, int x, int y, int index, boolean half, int part, LivingEntity wearer, WeightSplit split) {
        if (part >= SOURCE) {
            int tint = split.tint(part - SOURCE);
            drawTinted(graphics, x, y, index, half, tint >= 0 ? FeatherColors.ofColor(tint) : FeatherColors.of(BuiltInRegistries.ITEM.byId(-tint - 1)));
            return;
        }
        // A mount's armor is weighed as its chest piece (see ArmorWeights.totalWeight).
        EquipmentSlot slot = part == ArmorWeights.CHEST && !(wearer instanceof Player) ? EquipmentSlot.BODY
                : part < WEIGHT_SLOTS.length ? WEIGHT_SLOTS[part] : null;
        ItemStack piece = slot != null ? wearer.getItemBySlot(slot) : ItemStack.EMPTY;
        if (piece.isEmpty()) drawIcons(graphics, x, y, index, index + 1, half, ARMOR.body, ARMOR.border, ARMOR);
        else drawTinted(graphics, x, y, index, half, FeatherColors.of(piece));
    }

    /** One plain feather in a FeatherColors pair: its body color, outlined in the complementary one. */
    private static void drawTinted(GuiGraphics graphics, int x, int y, int index, boolean half, long tint) {
        drawIcons(graphics, x, y, index, index + 1, half, OPAQUE | FeatherColors.body(tint), OPAQUE | FeatherColors.outline(tint), TINTED);
    }

    private static void drawFeathers(GuiGraphics graphics, int x, int y, int count, Look look) {
        drawFeathers(graphics, x, y, count, look.body, look.border, look);
    }

    /**
     * {@code count} feathers from the right, two per icon; an odd count ends in a half icon.
     */
    private static void drawFeathers(GuiGraphics graphics, int x, int y, int count, int body, int border, Look sprites) {
        if (count <= 0) return;
        drawIcons(graphics, x, y, 0, (count + 1) / 2, (count & 1) == 1, body, border, sprites);
    }

    /**
     * Icons {@code from} to {@code to} (exclusive) of {@code sprites}' variant, the last one half when
     * {@code halfLast}: all bodies in one color, all shines, then all borders in the other, so the color changes three
     * times per run instead of per icon. A border with alpha 0 is not drawn.
     */
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

    /** Empty slots: the fill in the style's body color, the outline (its variant's) in its border color. */
    private static void drawSlots(GuiGraphics graphics, int x, int y, int icons, Look look) {
        setColor(graphics, look.body);
        for (int i = 0; i < icons; i++) draw(graphics, look.slotSheet, look.slotWidth, look.slotHeight, x, y, i, EMPTY_U, 0);
        if ((look.border >>> 24) != 0) {
            setColor(graphics, look.border);
            for (int i = 0; i < icons; i++) draw(graphics, look.sheet, look.sheetWidth, look.sheetHeight, x, y, i, BORDER_FULL_U, look.row * SIZE);
        }
        graphics.setColor(1f, 1f, 1f, alpha);
    }

    /** The style's overlay over {@code icons} slots: primary, then accent. */
    private static void drawOverlay(GuiGraphics graphics, int x, int y, int icons, Look look) {
        int v = look.overlayRow * SIZE;
        setColor(graphics, look.overlayColor);
        for (int i = 0; i < icons; i++) draw(graphics, look.overlaySheet, look.overlayWidth, look.overlayHeight, x, y, i, OVERLAY_U, v);
        setColor(graphics, look.overlayAccent);
        for (int i = 0; i < icons; i++) draw(graphics, look.overlaySheet, look.overlayWidth, look.overlayHeight, x, y, i, OVERLAY_ACCENT_U, v);
        graphics.setColor(1f, 1f, 1f, alpha);
    }

    /** An ARGB color, its alpha times the fade. */
    private static void setColor(GuiGraphics graphics, int argb) {
        graphics.setColor(FastColor.ARGB32.red(argb) / 255f, FastColor.ARGB32.green(argb) / 255f, FastColor.ARGB32.blue(argb) / 255f,
                FastColor.ARGB32.alpha(argb) / 255f * alpha);
    }

    /** The color moved toward white by the pulse. */
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

    /**
     * How far the {@code index}-th icon is moved by the row's animation: up where the wave is, or a random jitter
     * that changes with the shake's clock (a hash of clock and index: no Random, nothing allocated).
     */
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

    private static void drawRowCount(GuiGraphics graphics, int x, int y, int layers) {
        HudCompat.rowCount(graphics, x, y, layers, 1);
        // Overflowing Bars' renderer resets the color, fade included.
        graphics.setColor(1f, 1f, 1f, alpha);
    }

    /**
     * The player's style: the first provider's answer, else the configured color. Then the fixed ones, resource
     * packs' changes included.
     */
    private static void refreshStyles(LocalPlayer player) {
        ResourceLocation chosen = FeatherStyles.select(player, DATA);
        FeatherStyle style = chosen != null ? FeatherStylePack.resolve(chosen) : null;
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
