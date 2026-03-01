package org.example.trafficsim.model;

import java.util.Set;

public record LaneDeclaration(Road road, int index, Set<Movement> movements) {


    public static LaneDeclaration defaultLane(Road road, int index) {
        return new LaneDeclaration(road, index, Set.of(Movement.STRAIGHT, Movement.PERMISSIVE_LEFT, Movement.RIGHT));
    }
}
