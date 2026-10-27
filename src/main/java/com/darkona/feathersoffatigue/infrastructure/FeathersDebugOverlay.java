package com.darkona.feathersoffatigue.infrastructure;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.content.feathers.ClientFeathersData;
import com.darkona.feathersoffatigue.config.ServerConfig;
import dev.darkona.jdlib.api.debug.DebugLines;
import dev.darkona.jdlib.api.debug.JDDebugFeed;
import dev.darkona.jdlib.api.hud.JDDebugOverlay;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public final class FeathersDebugOverlay {

    private static final ClientFeathersData DATA = ClientFeathersData.INSTANCE;
    private static final int LEFT_TOP = 2;
    private static final long LAST_SPEND_MILLIS = 3000;

    public static final JDDebugOverlay.Spec SPEC = new JDDebugOverlay.Spec(JDDebugOverlay.Side.LEFT, LEFT_TOP, null,
            ServerConfig.DEBUG_MODE::get, FeathersDebugOverlay::describe);

    private FeathersDebugOverlay() {}

    private static void describe(LocalPlayer player, DebugLines lines) {
        lines.add("Feathers %d/%d  stamina %d/%d  bonus %d  weight %d".formatted(DATA.feathers(), DATA.maxFeathers(),
                DATA.stamina(), DATA.maxStamina(), DATA.bonusStamina(), DATA.weight()), 0xFFFFFF);
        lines.add("Strain %d/%d  regen delay %d  exhausted %s  rest %s".formatted(DATA.strain(), DATA.maxStrain(),
                DATA.regenDelay(), DATA.exhausted(), DATA.restState()), 0xFF8080);
        lines.add("Regen %.2f f/s  usage x%.2f".formatted(FeathersAPI.getRegenPerSecond(player),
                FeathersAPI.getUsageMultiplier(player)), 0xDDDD00);
        ResourceLocation source = JDDebugFeed.latestSource(LAST_SPEND_MILLIS);
        if (source != null) lines.add("Spent %d on %s".formatted(JDDebugFeed.latestAmount(), source), 0x80FF80);
    }
}
