package com.darkona.feathersoffatigue.compat.thirst;

import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.world.entity.player.Player;

final class ThirstBridge {
    private ThirstBridge() {}

    static int thirst(Player player) {
        IThirst thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).orElse(null);
        return thirst != null ? thirst.getThirst() : 20;
    }

    static int quench(Player player) {
        IThirst thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).orElse(null);
        return thirst != null ? thirst.getQuenched() : 0;
    }

    static void drain(Player player, int points) {
        IThirst thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).orElse(null);
        if (thirst == null) return;
        thirst.setThirst(Math.max(0, thirst.getThirst() - points));
        thirst.updateThirstData(player);
    }
}
