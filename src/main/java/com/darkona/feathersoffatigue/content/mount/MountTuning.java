package com.darkona.feathersoffatigue.content.mount;

import com.darkona.feathersoffatigue.api.MountStats;
import com.darkona.feathersoffatigue.config.ServerConfig;

public record MountTuning(double minFeathers, double maxFeathers, double regenPerSecond, double gallopFeathersPerSecond,
                          double gallopSpeed, double jumpFeathers) {

    public static MountTuning of(MountStats stats) {
        int min = stats.minFeathers().orElseGet(ServerConfig.MOUNT_MIN_FEATHERS);
        int max = stats.maxFeathers().orElseGet(ServerConfig.MOUNT_MAX_FEATHERS);
        return new MountTuning(Math.min(min, max), Math.max(min, max),
                stats.regenPerSecond().orElseGet(ServerConfig.MOUNT_REGEN),
                stats.gallopFeathersPerSecond().orElseGet(ServerConfig.MOUNT_GALLOP_FEATHERS_PER_SECOND),
                stats.gallopSpeed().orElseGet(ServerConfig.MOUNT_GALLOP_SPEED),
                stats.jumpFeathers().orElseGet(ServerConfig.MOUNT_JUMP_FEATHERS));
    }
}
