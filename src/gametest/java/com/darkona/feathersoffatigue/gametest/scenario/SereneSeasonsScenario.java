package com.darkona.feathersoffatigue.gametest.scenario;

import net.minecraft.world.level.Level;
import sereneseasons.api.season.SeasonHelper;

/**
 * Reads Serene Seasons state for tests. Only loaded when it is present.
 */
public final class SereneSeasonsScenario {

    private SereneSeasonsScenario() {}

    public static String season(Level level) {
        return SeasonHelper.getSeasonState(level).getSubSeason().name();
    }
}
