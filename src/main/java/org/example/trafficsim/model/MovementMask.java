package org.example.trafficsim.model;

import java.util.ArrayList;
import java.util.List;

public final class MovementMask {

    public static final int LEFT = 1;
    public static final int STRAIGHT = 2;
    public static final int RIGHT = 4;


    private MovementMask() {}

    public static int bit(Movement m) {
        return switch (m) {
            case LEFT -> LEFT;
            case STRAIGHT -> STRAIGHT;
            case RIGHT -> RIGHT;
        };
    }

    /** Returns all {@link Movement}s encoded in the given bitmask. */
    public static List<Movement> movementsFromMask(int mask) {
        List<Movement> result = new ArrayList<>();
        if ((mask & LEFT)     != 0) result.add(Movement.LEFT);
        if ((mask & STRAIGHT) != 0) result.add(Movement.STRAIGHT);
        if ((mask & RIGHT)    != 0) result.add(Movement.RIGHT);
        return result;
    }
}
