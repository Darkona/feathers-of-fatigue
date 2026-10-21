package com.darkona.feathersoffatigue.compatibility.toughasnails;

import net.minecraft.world.entity.player.Player;
import toughasnails.api.temperature.TemperatureHelper;
import toughasnails.api.thirst.ThirstHelper;

final class ToughAsNailsBridge {

    enum Level {ICY, COLD, NEUTRAL, WARM, HOT}

    private static final Level[] LEVELS = Level.values();

    private ToughAsNailsBridge() {}

    static boolean temperatureEnabled() {
        return TemperatureHelper.isTemperatureEnabled();
    }

    static Level temperature(Player player) {
        return LEVELS[TemperatureHelper.getTemperatureForPlayer(player).ordinal()];
    }

    static float hyperthermia(Player player) {
        return TemperatureHelper.getPercentHyperthermic(player);
    }

    static boolean thirstEnabled() {
        return ThirstHelper.isThirstEnabled();
    }

    static int thirst(Player player) {
        return ThirstHelper.getThirst(player).getThirst();
    }

    static float hydration(Player player) {
        return ThirstHelper.getThirst(player).getHydration();
    }

    static void addThirstExhaustion(Player player, float exhaustion) {
        ThirstHelper.getThirst(player).addExhaustion(exhaustion);
    }
}
