package com.darkona.feathersoffatigue.style;

import com.darkona.feathersoffatigue.api.client.FeatherStyle;
import com.darkona.feathersoffatigue.api.client.FeatherStyles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.darkona.jdlib.api.hud.PackPatches;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class FeatherStylePack {

    public static final ResourceLocation FILE = id("feather_styles.json");

    public record Patch(Optional<Integer> body, Optional<Integer> border, Optional<ResourceLocation> variant, Optional<ResourceLocation> overlay,
                        Optional<Integer> overlayColor, Optional<Integer> overlayAccent, Optional<ResourceLocation> sprites) {

        public static final Codec<Patch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FeatherStyle.COLOR_CODEC.optionalFieldOf("body").forGetter(Patch::body),
                FeatherStyle.COLOR_CODEC.optionalFieldOf("border").forGetter(Patch::border),
                ResourceLocation.CODEC.optionalFieldOf("variant").forGetter(Patch::variant),
                ResourceLocation.CODEC.optionalFieldOf("overlay").forGetter(Patch::overlay),
                FeatherStyle.COLOR_CODEC.optionalFieldOf("overlay_color").forGetter(Patch::overlayColor),
                FeatherStyle.COLOR_CODEC.optionalFieldOf("overlay_accent").forGetter(Patch::overlayAccent),
                ResourceLocation.CODEC.optionalFieldOf("sprites").forGetter(Patch::sprites)
        ).apply(instance, Patch::new));

        public Patch over(Patch below) {
            return new Patch(body.or(below::body), border.or(below::border), variant.or(below::variant), overlay.or(below::overlay),
                    overlayColor.or(below::overlayColor), overlayAccent.or(below::overlayAccent), sprites.or(below::sprites));
        }

        public @Nullable FeatherStyle applyTo(@Nullable FeatherStyle base) {
            if (base == null) {
                if (body.isEmpty()) return null;
                base = new FeatherStyle(body.get(), FeatherStyle.BLACK);
            }
            ResourceLocation overlayId = overlay.isPresent() ? (overlay.get().getPath().equals("none") ? null : overlay.get()) : base.overlay();
            return new FeatherStyle(body.orElse(base.body()), border.orElse(base.border()), variant.orElse(base.variant()), overlayId,
                    overlayColor.orElse(base.overlayColor()), overlayAccent.orElse(base.overlayAccent()), sprites.orElse(base.sprites()));
        }
    }

    public static final PackPatches<Patch> PATCHES = new PackPatches<>(FILE, "styles", Patch.CODEC, Patch::over);

    private static final FeatherStyle MISSING = new FeatherStyle(0, 0);
    private static final Object2ObjectOpenHashMap<ResourceLocation, FeatherStyle> RESOLVED = new Object2ObjectOpenHashMap<>();
    private static int resolvedVersion = -1;
    private static int resolvedPatches = -1;

    private FeatherStylePack() {}

    public static synchronized @Nullable FeatherStyle resolve(ResourceLocation id) {
        int version = FeatherStyles.version();
        int patchVersion = PATCHES.version();
        if (version != resolvedVersion || patchVersion != resolvedPatches) {
            RESOLVED.clear();
            resolvedVersion = version;
            resolvedPatches = patchVersion;
        }
        FeatherStyle cached = RESOLVED.get(id);
        if (cached == null) {
            Patch patch = PATCHES.get(id);
            FeatherStyle registered = FeatherStyles.get(id);
            cached = patch != null ? patch.applyTo(registered) : registered;
            if (cached == null) cached = MISSING;
            RESOLVED.put(id, cached);
        }
        return cached == MISSING ? null : cached;
    }
}
