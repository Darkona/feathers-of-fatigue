package com.darkona.feathersoffatigue.gametest.scenario;

import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.capability.ModAttachment;
import net.minecraft.world.entity.player.Player;

/**
 * Sets Thirst Was Taken state for tests. Only loaded when it is present.
 */
public final class ThirstScenario {

    private ThirstScenario() {}

    public static void thirst(Player player, int thirst, int quench) {
        IThirst data = player.getData(ModAttachment.PLAYER_THIRST);
        data.setThirst(thirst);
        data.setQuenched(quench);
    }
}
