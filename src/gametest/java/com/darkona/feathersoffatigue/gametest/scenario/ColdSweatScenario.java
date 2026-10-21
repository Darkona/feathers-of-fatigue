package com.darkona.feathersoffatigue.gametest.scenario;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.player.Player;

public final class ColdSweatScenario {

    private ColdSweatScenario() {}

    public static void bodyTemperature(Player player, double degrees) {
        Temperature.set(player, Temperature.Trait.BASE, 0);
        Temperature.set(player, Temperature.Trait.CORE, degrees);
    }
}
