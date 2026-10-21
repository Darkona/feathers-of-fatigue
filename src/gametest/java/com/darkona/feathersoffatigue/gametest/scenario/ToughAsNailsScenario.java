package com.darkona.feathersoffatigue.gametest.scenario;

import net.minecraft.world.entity.player.Player;
import toughasnails.api.temperature.ITemperature;
import toughasnails.api.temperature.TemperatureHelper;
import toughasnails.api.temperature.TemperatureLevel;
import toughasnails.api.thirst.IThirst;
import toughasnails.api.thirst.ThirstHelper;

/**
 * Sets Tough As Nails state for tests. Only loaded when it is present.
 */
public final class ToughAsNailsScenario {

    private ToughAsNailsScenario() {}

    public static boolean temperatureEnabled() {
        return TemperatureHelper.isTemperatureEnabled();
    }

    public static boolean thirstEnabled() {
        return ThirstHelper.isThirstEnabled();
    }

    public static void icy(Player player) {
        ITemperature data = TemperatureHelper.getTemperatureData(player);
        data.setLevel(TemperatureLevel.ICY);
        data.setHyperthermiaTicks(0);
    }

    public static void heatstroke(Player player) {
        ITemperature data = TemperatureHelper.getTemperatureData(player);
        data.setLevel(TemperatureLevel.HOT);
        data.setHyperthermiaTicks(TemperatureHelper.getTicksRequiredForHyperthermia());
    }

    public static void parched(Player player) {
        IThirst thirst = ThirstHelper.getThirst(player);
        thirst.setThirst(0);
        thirst.setHydration(0f);
    }
}
