package org.example.trafficsim.model;

public enum Road {
    NORTH, SOUTH, EAST, WEST;

    public static Road fromString(String s) {
        if (s == null) throw new IllegalArgumentException("Road is null");
        return Road.valueOf(s.trim().toUpperCase());
    }
}