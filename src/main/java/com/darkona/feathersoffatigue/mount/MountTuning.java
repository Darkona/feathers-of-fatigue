package com.darkona.feathersoffatigue.mount;

import com.darkona.feathersoffatigue.api.MountStats;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;

public record MountTuning(double minFeathers, double maxFeathers, double regenPerSecond, double gallopFeathersPerSecond,
                          double gallopSpeed, double jumpFeathers) {

    public static MountTuning of(MountStats stats) {
        int min = stats.minFeathers().orElseGet(FeathersServerConfig.MOUNT_MIN_FEATHERS);
        int max = stats.maxFeathers().orElseGet(FeathersServerConfig.MOUNT_MAX_FEATHERS);
        return new MountTuning(Math.min(min, max), Math.max(min, max),
                stats.regenPerSecond().orElseGet(FeathersServerConfig.MOUNT_REGEN),
                stats.gallopFeathersPerSecond().orElseGet(FeathersServerConfig.MOUNT_GALLOP_FEATHERS_PER_SECOND),
                stats.gallopSpeed().orElseGet(FeathersServerConfig.MOUNT_GALLOP_SPEED),
                stats.jumpFeathers().orElseGet(FeathersServerConfig.MOUNT_JUMP_FEATHERS));
    }
}
