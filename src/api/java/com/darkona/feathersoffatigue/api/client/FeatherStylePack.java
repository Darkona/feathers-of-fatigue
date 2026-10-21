package com.darkona.feathersoffatigue.api.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.darkona.jdlib.api.hud.PackPatches;
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
}
