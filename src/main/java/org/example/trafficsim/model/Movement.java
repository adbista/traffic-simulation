package org.example.trafficsim.model;

public enum Movement {
    RIGHT(1),
    STRAIGHT(2),
    LEFT(3);

    // offset in cycle [NORTH(0), WEST(1), SOUTH(2), EAST(3)]
    private final int offset;

    Movement(int offset) {
        this.offset = offset;
    }

    public static Movement fromString(String s) {
        if (s == null) throw new IllegalArgumentException("Movement string is null");
        return Movement.valueOf(s.trim().toUpperCase());
    }

    public Road toRoad(Road from) {
        if (from == null) throw new IllegalArgumentException("From road is null");
        return rotate(from, offset);
    }

    public static Movement fromRoads(Road from, Road to) {
        if (from == null || to == null) throw new IllegalArgumentException("Road is null");
        if (to == from) throw new IllegalArgumentException("U-turn: from and to are the same road");

        int d = delta(from, to);
        return switch (d) {
            case 2 -> STRAIGHT;
            case 1 -> RIGHT;
            case 3 -> LEFT;
            default -> throw new IllegalArgumentException("Invalid roads: " + from + " -> " + to);
        };
    }
    public static Road opposite(Road r) {
        if (r == null) throw new IllegalArgumentException("Road is null");
        return rotate(r, 2);
    }
    private static int delta(Road from, Road to) {
        return (to.ordinal() - from.ordinal() + 4) % 4;
    }
    private static Road rotate(Road r, int offset) {
        Road[] v = Road.values();
        return v[(r.ordinal() + offset) % 4];
    }
}