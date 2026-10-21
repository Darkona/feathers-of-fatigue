package com.darkona.feathersoffatigue.api.client;

import com.darkona.feathersoffatigue.api.FeathersView;
import dev.darkona.jdlib.api.hud.HudStyles;
import dev.darkona.jdlib.core.registry.PriorityList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class FeatherStyles {

    public static final ResourceLocation GREEN = id("green");
    public static final ResourceLocation BLUE = id("blue");
    public static final ResourceLocation WHITE = id("white");
    public static final ResourceLocation COLD = id("cold");
    public static final ResourceLocation HOT = id("hot");
    public static final ResourceLocation ENERGIZED = id("energized");
    public static final ResourceLocation MOMENTUM = id("momentum");
    public static final ResourceLocation STRAIN = id("strain");
    public static final ResourceLocation ENDURANCE = id("endurance");
    public static final ResourceLocation ARMOR = id("armor");
    public static final ResourceLocation EMPTY = id("empty");
    public static final ResourceLocation EXHAUSTED = id("exhausted");

    public static final int STATUS_PRIORITY = 0;

    private record Context(Player player, FeathersView feathers) {}

    private static final HudStyles<Context, FeatherStyle, FeatherStylePack.Patch> STYLES = HudStyles.patched(PriorityList.Ties.REGISTRATION,
            FeatherStylePack.PATCHES, (patch, registered) -> patch.applyTo(registered));

    private FeatherStyles() {}

    public static void registerStyle(ResourceLocation id, FeatherStyle style) {
        STYLES.define(id, style);
    }

    public static void registerStyleProvider(ResourceLocation id, int priority, FeatherStyleProvider provider) {
        STYLES.addRule(id, priority, context -> provider.styleFor(context.player(), context.feathers()));
    }

    public static @Nullable FeatherStyle get(ResourceLocation id) {
        return STYLES.registered(id);
    }

    public static @Nullable FeatherStyle resolve(ResourceLocation id) {
        return STYLES.style(id);
    }

    public static @Nullable ResourceLocation select(Player player, FeathersView feathers) {
        return STYLES.select(new Context(player, feathers));
    }

    public static int version() {
        return STYLES.version();
    }
}
