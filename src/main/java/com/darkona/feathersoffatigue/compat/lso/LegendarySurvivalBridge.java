package com.darkona.feathersoffatigue.compat.lso;

import net.minecraft.world.entity.player.Player;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;
import sfiomn.legendarysurvivaloverhaul.common.capabilities.temperature.TemperatureCapability;
import sfiomn.legendarysurvivaloverhaul.common.capabilities.thirst.ThirstCapability;
import sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil;

final class LegendarySurvivalBridge {
    enum Level {FROSTBITE, COLD, NORMAL, HOT, HEAT_STROKE}

    private static final Level[] LEVELS = Level.values();

    private LegendarySurvivalBridge() {}

    static Level temperature(Player player) {
        TemperatureCapability temperature = CapabilityUtil.getTempCapability(player);
        return temperature != null ? LEVELS[temperature.getTemperatureEnum().ordinal()] : Level.NORMAL;
    }

    static int hydration(Player player) {
        ThirstCapability thirst = CapabilityUtil.getThirstCapability(player);
        return thirst != null ? thirst.getHydrationLevel() : 20;
    }

    static float saturation(Player player) {
        ThirstCapability thirst = CapabilityUtil.getThirstCapability(player);
        return thirst != null ? thirst.getSaturationLevel() : 0f;
    }

    static void addThirstExhaustion(Player player, float exhaustion) {
        ThirstUtil.addExhaustion(player, exhaustion);
    }
}
