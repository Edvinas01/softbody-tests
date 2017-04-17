package com.edd.softbodies;

public final class Units {

    public static final int PPM = 100;
    public static final float MPP = 1f / PPM;

    private Units() {
    }

    public static float meters(float convert) {
        return convert * MPP;
    }

    public static int pixels(float convert) {
        return (int) convert * PPM;
    }
}