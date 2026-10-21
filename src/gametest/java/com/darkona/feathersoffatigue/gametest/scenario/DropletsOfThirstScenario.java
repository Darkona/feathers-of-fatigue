package com.darkona.feathersoffatigue.gametest.scenario;

import com.darkona.dropletsofthirst.api.DropletsAPI;
import net.minecraft.world.entity.player.Player;

/**
 * Sets Droplets of Thirst state for tests through its API. Only loaded when it is present.
 */
public final class DropletsOfThirstScenario {

    private DropletsOfThirstScenario() {}

    public static void thirst(Player player, int thirst, int quenched) {
        DropletsAPI.setThirst(player, thirst);
        DropletsAPI.setQuenched(player, quenched);
    }

    public static int thirst(Player player) {
        return DropletsAPI.view(player).thirst();
    }
}
