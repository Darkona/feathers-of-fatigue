package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.client.FeatherStyle;
import com.darkona.feathersoffatigue.api.client.FeatherStyles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.Map;
import java.util.Optional;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class FeatherStylePack {
    public static final ResourceLocation FILE = id("feather_styles.json");

    public record Patch(Optional<Integer> body, Optional<Integer> border, Optional<ResourceLocation> variant, Optional<ResourceLocation> overlay,
                        Optional<Integer> overlayColor, Optional<Integer> overlayAccent, Optional<ResourceLocation> sprites) {
        public static final Codec<Patch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                strictOptional("body", FeatherStyle.COLOR_CODEC).forGetter(Patch::body),
                strictOptional("border", FeatherStyle.COLOR_CODEC).forGetter(Patch::border),
                strictOptional("variant", ResourceLocation.CODEC).forGetter(Patch::variant),
                strictOptional("overlay", ResourceLocation.CODEC).forGetter(Patch::overlay),
                strictOptional("overlay_color", FeatherStyle.COLOR_CODEC).forGetter(Patch::overlayColor),
                strictOptional("overlay_accent", FeatherStyle.COLOR_CODEC).forGetter(Patch::overlayAccent),
                strictOptional("sprites", ResourceLocation.CODEC).forGetter(Patch::sprites)
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

        private static <A> MapCodec<Optional<A>> strictOptional(String name, Codec<A> codec) {
            return Codec.PASSTHROUGH.optionalFieldOf(name).flatXmap(
                    value -> value.isPresent() ? codec.parse(value.get()).map(Optional::of) : DataResult.success(Optional.empty()),
                    value -> value.isPresent()
                            ? codec.encodeStart(JsonOps.INSTANCE, value.get()).map(json -> Optional.<Dynamic<?>>of(new Dynamic<>(JsonOps.INSTANCE, json)))
                            : DataResult.success(Optional.empty()));
        }
    }

    private static final FeatherStyle MISSING = new FeatherStyle(0, 0);
    private static volatile Map<ResourceLocation, Patch> patches = Map.of();
    private static final Object2ObjectOpenHashMap<ResourceLocation, FeatherStyle> RESOLVED = new Object2ObjectOpenHashMap<>();
    private static int resolvedVersion = -1;

    private FeatherStylePack() {}

    public static synchronized @Nullable FeatherStyle resolve(ResourceLocation id) {
        int version = FeatherStyles.version();
        if (version != resolvedVersion) {
            RESOLVED.clear();
            resolvedVersion = version;
        }
        FeatherStyle cached = RESOLVED.get(id);
        if (cached == null) {
            Patch patch = patches.get(id);
            FeatherStyle registered = FeatherStyles.get(id);
            cached = patch != null ? patch.applyTo(registered) : registered;
            if (cached == null) cached = MISSING;
            RESOLVED.put(id, cached);
        }
        return cached == MISSING ? null : cached;
    }

    public static void load(ResourceManager manager) {
        Map<ResourceLocation, Patch> merged = new Object2ObjectOpenHashMap<>();
        for (Resource resource : manager.getResourceStack(FILE)) {
            try (Reader reader = resource.openAsReader()) {
                merge(merged, parse(JsonParser.parseReader(reader), resource.sourcePackId()));
            } catch (Exception e) {
                Feathers.LOGGER.error("Couldn't read {} from {}", FILE, resource.sourcePackId(), e);
            }
        }
        setPatches(merged);
    }

    public static void merge(Map<ResourceLocation, Patch> into, Map<ResourceLocation, Patch> higher) {
        higher.forEach((id, patch) -> into.merge(id, patch, (below, above) -> above.over(below)));
    }

    public static Map<ResourceLocation, Patch> parse(JsonElement json, String source) {
        Map<ResourceLocation, Patch> parsed = new Object2ObjectOpenHashMap<>();
        if (!(json instanceof JsonObject root) || !(root.get("styles") instanceof JsonObject styles)) {
            Feathers.LOGGER.error("{} in {} needs a \"styles\" object", FILE, source);
            return parsed;
        }
        for (Map.Entry<String, JsonElement> entry : styles.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) {
                Feathers.LOGGER.error("{} in {}: \"{}\" isn't a style id", FILE, source, entry.getKey());
                continue;
            }
            DataResult<Patch> result = Patch.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
            result.result().ifPresent(patch -> parsed.put(id, patch));
            result.error().ifPresent(error -> Feathers.LOGGER.error("{} in {}: {}: {}", FILE, source, id, error.message()));
        }
        return parsed;
    }

    public static synchronized void setPatches(Map<ResourceLocation, Patch> next) {
        patches = Map.copyOf(next);
        RESOLVED.clear();
    }
}
