package com.darkona.feathersoffatigue.compatibility.thirst;

import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.capability.ModAttachment;
import net.minecraft.world.entity.player.Player;

final class ThirstBridge {

    private ThirstBridge() {}

    static int thirst(Player player) {
        return player.getData(ModAttachment.PLAYER_THIRST).getThirst();
    }

    static int quench(Player player) {
        return player.getData(ModAttachment.PLAYER_THIRST).getQuenched();
    }

    static void drain(Player player, int points) {
        IThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
        thirst.setThirst(Math.max(0, thirst.getThirst() - points));
    }
}
