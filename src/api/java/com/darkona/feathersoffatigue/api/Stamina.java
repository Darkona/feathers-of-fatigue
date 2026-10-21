package com.darkona.feathersoffatigue.api;

/**
 * Units. Everything in the API counts <b>stamina</b>: a thousandth of a feather, so mods can charge fractions of a
 * feather and regeneration can be very slow without losing precision. A feather is half an icon on the HUD.
 */
public final class Stamina {

    /** The number of stamina units in one feather. */
    public static final int PER_FEATHER = 1000;

    private Stamina() {}

    /**
     * Converts whole feathers to stamina units. Use this overload for fixed, integral costs.
     *
     * @param feathers the number of feathers
     * @return the converted value, saturated to the {@code int} range
     */
    public static int ofFeathers(int feathers) {
        return saturate((long) feathers * PER_FEATHER);
    }

    /**
     * Converts fractional feathers to stamina units. The result uses the nearest unit, and NaN produces zero.
     *
     * @param feathers the number of feathers
     * @return the converted value, saturated to the {@code int} range
     */
    public static int ofFeathers(double feathers) {
        return saturate(Math.round(feathers * PER_FEATHER));
    }

    /**
     * Converts stamina units to whole feathers, rounded down.
     *
     * @param stamina the amount in stamina units
     * @return the number of complete feathers
     */
    public static int toFeathers(int stamina) {
        return Math.floorDiv(stamina, PER_FEATHER);
    }

    /**
     * Converts stamina units to whole feathers, rounded up. Use this for displays that show a partial feather.
     *
     * @param stamina the amount in stamina units
     * @return the number of visible feathers
     */
    public static int toFeathersCeil(int stamina) {
        return (int) -Math.floorDiv(-(long) stamina, PER_FEATHER);
    }

    /**
     * Converts a rate in feathers per second to stamina units per game tick.
     *
     * @param feathersPerSecond the rate in feathers per second
     * @return the rate in stamina units per tick
     */
    public static double perTick(double feathersPerSecond) {
        return feathersPerSecond * PER_FEATHER / 20.0;
    }

    private static int saturate(long stamina) {
        return Math.clamp(stamina, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
