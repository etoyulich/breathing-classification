package com.breathingdetermination.util;

public class DataRecord {

    private Coordinate chestMarker;

    private Coordinate abdominalMarker;

    private Coordinate backMarker;

    private double timestamp;


    public DataRecord(Coordinate chestMarker, Coordinate abdominalMarker, Coordinate backMarker, double timestamp) {
        this.chestMarker = chestMarker;
        this.abdominalMarker = abdominalMarker;
        this.backMarker = backMarker;
        this.timestamp = timestamp;
    }

    public double getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return timestamp + " " + chestMarker + " " + abdominalMarker + " " + backMarker;
    }
}
