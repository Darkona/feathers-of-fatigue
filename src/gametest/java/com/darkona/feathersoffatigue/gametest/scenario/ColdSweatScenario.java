package com.darkona.feathersoffatigue.gametest.scenario;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.player.Player;

/**
 * Sets Cold Sweat state for tests. Only loaded when Cold Sweat is present.
 */
public final class ColdSweatScenario {

    private ColdSweatScenario() {}

    /**
     * Body temperature is derived (core plus base) and read-only; setting the core with a zero base sets it.
     */
    public static void bodyTemperature(Player player, double degrees) {
        Temperature.set(player, Temperature.Trait.BASE, 0);
        Temperature.set(player, Temperature.Trait.CORE, degrees);
    }
}
