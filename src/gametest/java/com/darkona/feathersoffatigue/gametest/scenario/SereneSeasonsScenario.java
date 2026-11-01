package com.darkona.feathersoffatigue.gametest.scenario;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.Level;
import sereneseasons.api.season.SeasonHelper;

public final class SereneSeasonsScenario {
    private SereneSeasonsScenario() {}

    public static BlockPos winterPlace(ServerLevel level, BlockPos from, double maxTemperature) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> !SeasonHelper.usesTropicalSeasons(biome)
                && biome.value().getModifiedClimateSettings().temperature() < maxTemperature, from, 3200, 32, 64);
        return found != null ? found.getFirst() : from;
    }

    public static boolean isTropical(Level level, BlockPos pos) {
        return SeasonHelper.usesTropicalSeasons(level.getBiome(pos));
    }

    public static String season(Level level) {
        return SeasonHelper.getSeasonState(level).getSubSeason().name();
    }
}
