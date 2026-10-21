package com.darkona.feathersoffatigue.compatibility.lso;

import net.minecraft.world.entity.player.Player;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;
import sfiomn.legendarysurvivaloverhaul.common.attachments.ModAttachments;

final class LegendarySurvivalBridge {

    enum Level {FROSTBITE, COLD, NORMAL, HOT, HEAT_STROKE}

    private static final Level[] LEVELS = Level.values();

    private LegendarySurvivalBridge() {}

    static Level temperature(Player player) {
        return LEVELS[player.getData(ModAttachments.TEMPERATURE).getTemperatureEnum().ordinal()];
    }

    static int hydration(Player player) {
        return player.getData(ModAttachments.THIRST).getHydrationLevel();
    }

    static float saturation(Player player) {
        return player.getData(ModAttachments.THIRST).getSaturationLevel();
    }

    static void addThirstExhaustion(Player player, float exhaustion) {
        ThirstUtil.addExhaustion(player, exhaustion);
    }
}
