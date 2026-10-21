package com.darkona.feathersoffatigue.client.gui;

import com.darkona.feathersoffatigue.Feathers;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.io.InputStream;
import java.util.Optional;

public final class FeatherColors {

    public static final int LEATHER = 0x9A6A3F;

    private static final long UNKNOWN = Long.MIN_VALUE;
    private static final Object2LongOpenHashMap<ResourceLocation> BY_TEXTURE = new Object2LongOpenHashMap<>();
    private static final Reference2LongOpenHashMap<Item> BY_ITEM = new Reference2LongOpenHashMap<>();
    private static final Int2IntOpenHashMap SHADES = new Int2IntOpenHashMap();
    private static final Int2LongOpenHashMap BY_DYE = new Int2LongOpenHashMap();

    static {
        BY_TEXTURE.defaultReturnValue(UNKNOWN);
        BY_ITEM.defaultReturnValue(UNKNOWN);
        SHADES.defaultReturnValue(-1);
        BY_DYE.defaultReturnValue(UNKNOWN);
    }

    private FeatherColors() {}

    public static int body(long pair) {
        return (int) (pair >>> 32);
    }

    public static int outline(long pair) {
        return (int) pair;
    }

    public static long of(LivingEntity entity) {
        ResourceLocation texture;
        try {
            texture = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).getTextureLocation(entity);
        } catch (RuntimeException e) {
            texture = null;
        }
        if (texture == null) texture = MissingTextureAtlasSprite.getLocation();
        long cached = BY_TEXTURE.getLong(texture);
        if (cached != UNKNOWN) return cached;
        long computed = pair(dominantOfTexture(texture));
        BY_TEXTURE.put(texture, computed);
        return computed;
    }

    public static long of(ItemStack stack) {
        DyedItemColor dye = stack.get(DataComponents.DYED_COLOR);
        if (dye != null) return ofColor(dye.rgb());
        Item item = stack.getItem();
        long cached = BY_ITEM.getLong(item);
        if (cached != UNKNOWN) return cached;
        long computed = pair(dominantOfItem(stack));
        BY_ITEM.put(item, computed);
        return computed;
    }

    public static long pair(int body, int outline) {
        return (long) body << 32 | (outline & 0xFFFFFFFFL);
    }

    public static int shade(int rgb, int layer) {
        int key = (rgb & 0xFFFFFF) | Math.min(layer, 127) << 24;
        int cached = SHADES.get(key);
        if (cached != -1) return cached;
        float[] hsl = hsl(rgb);
        float direction = hsl[2] >= 0.5f ? -1f : 1f;
        float lightness = hsl[2] + direction * 0.34f * layer;
        if (lightness < 0.19f || lightness > 0.87f) lightness = hsl[2] - direction * 0.25f * (layer - 1);
        lightness = Math.clamp(lightness, 0.2f, 0.86f);
        int shaded = rgb(hsl[0], hsl[1], lightness);
        SHADES.put(key, shaded);
        return shaded;
    }

    public static long ofColor(int rgb) {
        rgb &= 0xFFFFFF;
        long cached = BY_DYE.get(rgb);
        if (cached != UNKNOWN) return cached;
        long computed = pair(rgb);
        if (BY_DYE.size() >= 256) BY_DYE.clear();
        BY_DYE.put(rgb, computed);
        return computed;
    }

    public static long of(Item item) {
        long cached = BY_ITEM.getLong(item);
        return cached != UNKNOWN ? cached : of(item.getDefaultInstance());
    }

    public static void clear() {
        BY_TEXTURE.clear();
        BY_ITEM.clear();
        BY_DYE.clear();
    }

    private static int dominantOfTexture(ResourceLocation texture) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) return LEATHER;
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            return dominant(image);
        } catch (Exception e) {
            Feathers.LOGGER.debug("Couldn't read {} for its feather color", texture, e);
            return LEATHER;
        }
    }

    private static int dominantOfItem(ItemStack stack) {
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0)
                    .getParticleIcon(ModelData.EMPTY);
            return dominant(sprite.contents().getOriginalImage());
        } catch (RuntimeException e) {
            return LEATHER;
        }
    }

    static int dominant(NativeImage image) {
        int[] counts = new int[4096];
        long[] red = new long[4096];
        long[] green = new long[4096];
        long[] blue = new long[4096];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                if (FastColor.ABGR32.alpha(abgr) < 200) continue;
                int r = FastColor.ABGR32.red(abgr);
                int g = FastColor.ABGR32.green(abgr);
                int b = FastColor.ABGR32.blue(abgr);
                if (r + g + b < 24) continue;
                int bucket = (r >> 4) << 8 | (g >> 4) << 4 | b >> 4;
                counts[bucket]++;
                red[bucket] += r;
                green[bucket] += g;
                blue[bucket] += b;
            }
        }
        int best = -1;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0 && (best < 0 || counts[i] > counts[best])) best = i;
        }
        if (best < 0) return LEATHER;
        int n = counts[best];
        return (int) (red[best] / n) << 16 | (int) (green[best] / n) << 8 | (int) (blue[best] / n);
    }

    static long pair(int rgb) {
        float[] hsl = hsl(rgb);
        float lightness = Math.clamp(hsl[2], 0.42f, 0.74f);
        float saturation = hsl[1] > 0.2f ? Math.max(hsl[1], 0.28f) : hsl[1];
        int body = rgb(hsl[0], saturation, lightness);
        int outline = rgb((hsl[0] + 0.5f) % 1f, Math.max(saturation, 0.45f), lightness > 0.58f ? 0.22f : 0.82f);
        return pair(body, outline);
    }

    private static float[] hsl(int rgb) {
        float r = FastColor.ARGB32.red(rgb) / 255f;
        float g = FastColor.ARGB32.green(rgb) / 255f;
        float b = FastColor.ARGB32.blue(rgb) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2;
        float h = 0;
        float s = 0;
        if (max != min) {
            float d = max - min;
            s = l > 0.5f ? d / (2 - max - min) : d / (max + min);
            if (max == r) h = (g - b) / d + (g < b ? 6 : 0);
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6;
        }
        return new float[]{h, s, l};
    }

    private static int rgb(float h, float s, float l) {
        if (s == 0) {
            int v = Math.round(l * 255);
            return v << 16 | v << 8 | v;
        }
        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;
        return Math.round(hue(p, q, h + 1f / 3) * 255) << 16 | Math.round(hue(p, q, h) * 255) << 8 | Math.round(hue(p, q, h - 1f / 3) * 255);
    }

    private static float hue(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6) return p + (q - p) * 6 * t;
        if (t < 0.5f) return q;
        if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }
}
