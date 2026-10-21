package com.darkona.feathersoffatigue.gametest.scenario;

import net.minecraft.world.entity.player.Player;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureEnum;
import sfiomn.legendarysurvivaloverhaul.common.attachments.ModAttachments;

public final class LegendarySurvivalScenario {

    private LegendarySurvivalScenario() {}

    public static void frostbite(Player player) {
        player.getData(ModAttachments.TEMPERATURE).setTemperatureLevel(TemperatureEnum.getMin());
    }

    public static void heatstroke(Player player) {
        player.getData(ModAttachments.TEMPERATURE).setTemperatureLevel(TemperatureEnum.getMax());
    }

    public static void parched(Player player) {
        player.getData(ModAttachments.THIRST).setHydrationLevel(0);
        player.getData(ModAttachments.THIRST).setSaturation(0f);
    }
}
