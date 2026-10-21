package com.darkona.feathersoffatigue.compatibility.sereneseasons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.SeasonHelper;

final class SereneSeasonsBridge {

    enum Season {SPRING, SUMMER, AUTUMN, WINTER}

    private static final Season[] SEASONS = Season.values();

    private SereneSeasonsBridge() {}

    static Season season(Level level) {
        return SEASONS[SeasonHelper.getSeasonState(level).getSeason().ordinal()];
    }

    static boolean isTropical(Level level, BlockPos pos) {
        return SeasonHelper.usesTropicalSeasons(level.getBiome(pos));
    }
}
