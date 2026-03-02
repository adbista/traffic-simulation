package org.example.trafficsim.model;


// LEFT           = protected left turn (exclusive phase; signal guarantees no oncoming traffic)
// PERMISSIVE_LEFT = unprotected left turn (driver yields to oncoming traffic themselves)
public enum Movement {
    STRAIGHT, RIGHT, LEFT, PERMISSIVE_LEFT;

    public static Movement fromString(String s) {
        if (s == null) throw new IllegalArgumentException("Movement string is null");
        return Movement.valueOf(s.trim().toUpperCase());
    }

    public Road toRoad(Road from) {
        return switch (this) {
            case STRAIGHT        -> opposite(from);
            case RIGHT           -> rightOf(from);
            case LEFT,
                 PERMISSIVE_LEFT -> leftOf(from);
        };
    }

    public static Road opposite(Road r) {
        return switch (r) {
            case NORTH -> Road.SOUTH;
            case SOUTH -> Road.NORTH;
            case EAST  -> Road.WEST;
            case WEST  -> Road.EAST;
        };
    }

    private static Road rightOf(Road r) {
        return switch (r) {
            case NORTH -> Road.WEST;
            case SOUTH -> Road.EAST;
            case EAST  -> Road.NORTH;
            case WEST  -> Road.SOUTH;
        };
    }

    private static Road leftOf(Road r) {
        return switch (r) {
            case NORTH -> Road.EAST;
            case SOUTH -> Road.WEST;
            case EAST  -> Road.SOUTH;
            case WEST  -> Road.NORTH;
        };
    }
}