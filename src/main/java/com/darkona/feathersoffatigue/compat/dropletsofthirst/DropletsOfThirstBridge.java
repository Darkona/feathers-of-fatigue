package com.darkona.feathersoffatigue.compat.dropletsofthirst;

import com.darkona.dropletsofthirst.api.DropletsAPI;
import com.darkona.dropletsofthirst.api.DropletsView;
import net.minecraft.world.entity.player.Player;

final class DropletsOfThirstBridge {

    private DropletsOfThirstBridge() {}

    static DropletsView view(Player player) {
        return DropletsAPI.view(player);
    }

    static void drain(Player player, int points) {
        DropletsAPI.addThirst(player, -points, 0);
    }
}
