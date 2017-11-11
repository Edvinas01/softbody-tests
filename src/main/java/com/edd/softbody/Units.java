package com.edd.softbody;

/**
 * Utility functions for dealing with simulation units.
 */
public final class Units {

    private static final float METERS_PER_PIXEL = 1 / 100f;

    private Units() {
    }

    /**
     * @return number converted to meters.
     */
    public static float toMeters(Number number) {
        return number.floatValue() * METERS_PER_PIXEL;
    }
}