package com.darkona.feathersoffatigue.gametest.scenario;

import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.world.entity.player.Player;

public final class ThirstScenario {
    private ThirstScenario() {}

    public static int thirst(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_THIRST).map(data -> data.getThirst()).orElse(20);
    }

    public static void thirst(Player player, int thirst, int quench) {
        player.getCapability(ModCapabilities.PLAYER_THIRST).ifPresent(data -> {
            data.setThirst(thirst);
            data.setQuenched(quench);
        });
    }
}
