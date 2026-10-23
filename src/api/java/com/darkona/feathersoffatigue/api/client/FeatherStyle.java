package com.darkona.feathersoffatigue.api.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.darkona.jdlib.core.CssColors;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * How a row of feathers looks. The HUD draws every feather from grayscale sprites in the style's {@link #variant}: the
 * body tinted with {@link #body}, the variant's white shine, and the outline tinted with {@link #border}. An
 * {@link #overlay}, if present, covers the row in its two colors. Colors use ARGB ({@code 0xAARRGGBB}), and their
 * alpha multiplies with the HUD's fade. Rows layered over a full row use deeper shades of the body color, with the same
 * border.
 *
 * @param body          the feather's color, ARGB
 * @param border        the outline's ARGB color. An alpha of 0 ({@link #NO_BORDER}) draws no outline
 * @param variant       the feather shape, a {@link FeatherVariants} id
 * @param overlay       a {@link FeatherVariants} overlay id drawn over the row, or null for none
 * @param overlayColor  the overlay's primary color, ARGB
 * @param overlayAccent the overlay's accent color, ARGB
 * @param sprites       a texture laid out like {@link FeatherVariants#SHEET} (56x72, the same cells) to take this
 *                      style's sprites from instead, or null. The colors still tint it: white body and border draw a
 *                      hand-colored sheet as is.
 */
public record FeatherStyle(int body, int border, ResourceLocation variant, @Nullable ResourceLocation overlay, int overlayColor, int overlayAccent,
                           @Nullable ResourceLocation sprites) {

    /** An alpha value that disables the border. */
    public static final int NO_BORDER = 0;
    /** Opaque black in ARGB format. */
    public static final int BLACK = 0xFF000000;
    /** Opaque white in ARGB format. */
    public static final int WHITE = 0xFFFFFFFF;

    /** {@code "#RRGGBB"} (opaque) or {@code "#AARRGGBB"}. */
    public static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(FeatherStyle::parseColor, FeatherStyle::formatColor);

    /**
     * {@code {"body": "#22A5F0", "border": "#000000", "variant": "feathers_of_fatigue:feather", "overlay": "feathers_of_fatigue:frost",
     * "overlay_color": "#8DC8FE", "overlay_accent": "#FFFFFF", "sprites": "mypack:textures/gui/feathers.png"}}: only
     * the body is required. The border defaults to black, the variant to the plain feather, the overlay colors to white.
     */
    public static final Codec<FeatherStyle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            COLOR_CODEC.fieldOf("body").forGetter(FeatherStyle::body),
            COLOR_CODEC.optionalFieldOf("border", BLACK).forGetter(FeatherStyle::border),
            ResourceLocation.CODEC.optionalFieldOf("variant", FeatherVariants.FEATHER).forGetter(FeatherStyle::variant),
            ResourceLocation.CODEC.optionalFieldOf("overlay").forGetter(style -> Optional.ofNullable(style.overlay())),
            COLOR_CODEC.optionalFieldOf("overlay_color", WHITE).forGetter(FeatherStyle::overlayColor),
            COLOR_CODEC.optionalFieldOf("overlay_accent", WHITE).forGetter(FeatherStyle::overlayAccent),
            ResourceLocation.CODEC.optionalFieldOf("sprites").forGetter(style -> Optional.ofNullable(style.sprites()))
    ).apply(instance, (body, border, variant, overlay, overlayColor, overlayAccent, sprites) ->
            new FeatherStyle(body, border, variant, overlay.orElse(null), overlayColor, overlayAccent, sprites.orElse(null))));

    /**
     * Validates required style values when a new record is created.
     *
     * @throws NullPointerException if {@code variant} is {@code null}
     */
    public FeatherStyle {
        Objects.requireNonNull(variant, "variant");
    }

    /**
     * Creates a plain feather with no overlay.
     *
     * @param body   the body color in ARGB format
     * @param border the border color in ARGB format
     */
    public FeatherStyle(int body, int border) {
        this(body, border, FeatherVariants.FEATHER, null, WHITE, WHITE, null);
    }

    /**
     * Creates a plain feather from opaque RGB colors. Use this helper when the source colors have no alpha channel.
     *
     * @param bodyRgb   the body color in {@code 0xRRGGBB} format
     * @param borderRgb the border color in {@code 0xRRGGBB} format
     * @return the opaque style
     */
    public static FeatherStyle opaque(int bodyRgb, int borderRgb) {
        return new FeatherStyle(0xFF000000 | bodyRgb, 0xFF000000 | borderRgb);
    }

    /**
     * Changes the body color.
     *
     * @param body the new ARGB color
     * @return a copy with the new body color
     */
    public FeatherStyle withBody(int body) {
        return new FeatherStyle(body, border, variant, overlay, overlayColor, overlayAccent, sprites);
    }

    /**
     * Changes the border color.
     *
     * @param border the new ARGB color
     * @return a copy with the new border color
     */
    public FeatherStyle withBorder(int border) {
        return new FeatherStyle(body, border, variant, overlay, overlayColor, overlayAccent, sprites);
    }

    /**
     * Changes the feather shape.
     *
     * @param variant the id of a registered variant
     * @return a copy with the new variant
     */
    public FeatherStyle withVariant(ResourceLocation variant) {
        return new FeatherStyle(body, border, variant, overlay, overlayColor, overlayAccent, sprites);
    }

    /**
     * Changes the row overlay and its colors.
     *
     * @param overlay the id of a registered overlay, or {@code null} for none
     * @param color   the primary ARGB color
     * @param accent  the accent ARGB color
     * @return a copy with the new overlay settings
     */
    public FeatherStyle withOverlay(@Nullable ResourceLocation overlay, int color, int accent) {
        return new FeatherStyle(body, border, variant, overlay, color, accent, sprites);
    }

    /**
     * Changes the optional style-specific sprite sheet.
     *
     * @param sprites the sprite sheet texture, or {@code null} to use the variant texture
     * @return a copy with the new sprite sheet
     */
    public FeatherStyle withSprites(@Nullable ResourceLocation sprites) {
        return new FeatherStyle(body, border, variant, overlay, overlayColor, overlayAccent, sprites);
    }

    /**
     * Parses a style color from {@code #RRGGBB} or {@code #AARRGGBB} text.
     *
     * @param text the color text
     * @return a successful ARGB value, or a codec error for invalid text
     */
    public static DataResult<Integer> parseColor(String text) {
        if (!CssColors.validArgb(text)) return DataResult.error(() -> "Not a #RRGGBB or #AARRGGBB color: " + text);
        return DataResult.success(CssColors.parseArgb(text, 0));
    }

    /**
     * Formats an ARGB color for the style codec. Opaque colors use the shorter {@code #RRGGBB} form.
     *
     * @param argb the color to format
     * @return the formatted color text
     */
    public static String formatColor(int argb) {
        return CssColors.formatArgb(argb);
    }
}
