package com.breathingdetermination.util;

public class Coordinate {

    private double x;

    private double y;

    private double z;


    public Coordinate(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return  x + " " + y + " " + z;
    }
}
