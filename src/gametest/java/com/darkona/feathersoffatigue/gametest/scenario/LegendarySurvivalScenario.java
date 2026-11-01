package com.darkona.feathersoffatigue.gametest.scenario;

import net.minecraft.world.entity.player.Player;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureEnum;
import sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil;

public final class LegendarySurvivalScenario {
    private LegendarySurvivalScenario() {}

    public static void frostbite(Player player) {
        CapabilityUtil.getTempCapability(player).setTemperatureLevel(TemperatureEnum.getMin());
    }

    public static void heatstroke(Player player) {
        CapabilityUtil.getTempCapability(player).setTemperatureLevel(TemperatureEnum.getMax());
    }

    public static void parched(Player player) {
        CapabilityUtil.getThirstCapability(player).setHydrationLevel(0);
        CapabilityUtil.getThirstCapability(player).setSaturation(0f);
    }
}
