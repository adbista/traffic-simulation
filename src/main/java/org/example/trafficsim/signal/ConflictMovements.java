package org.example.trafficsim.signal;

import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.MovementMask;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.TrafficLightType;

public final class ConflictMovements {

    private ConflictMovements() {}

    /**
     * Returns true if the two LaneSignal groups conflict, i.e. they cannot
     * share a phase.  Each group may encode multiple movements via its bitmask.
     * A conflict exists when at least one movement-pair across the two groups
     * would conflict according to the geometric rules.
     */
    public static boolean signalsConflict(LaneSignal a, LaneSignal b) {
        if (a.road() == b.road()) {
            return false;
        }

        boolean opposite = Movement.opposite(a.road()) == b.road();
        if (!opposite) {
            // Perpendicular roads always conflict (neither can yield safely)
            return true;
        }

        // Opposite road: check every movement-pair from both masks
        for (Movement movA : MovementMask.movementsFromMask(a.movementMask())) {
            for (Movement movB : MovementMask.movementsFromMask(b.movementMask())) {
                if (oppositeConflict(movA, a.trafficLightType(), movB, b.trafficLightType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Low-level per-movement conflict check (road + single movement + type).
     * Kept for callers that already decomposed the mask to individual movements.
     */
    public static boolean conflict(
            Road fromA, Movement movA, TrafficLightType typeA,
            Road fromB, Movement movB, TrafficLightType typeB
    ) {
        if (fromA == fromB) {
            return false;
        }

        boolean opposite = Movement.opposite(fromA) == fromB;
        if (opposite) {
            return oppositeConflict(movA, typeA, movB, typeB);
        }

        return true;
    }

    private static boolean oppositeConflict(
            Movement movA, TrafficLightType typeA,
            Movement movB, TrafficLightType typeB
    ) {
        // Two GENERIC movements from opposite roads share a phase (yield handled at runtime)
        if (typeA == TrafficLightType.GENERIC && typeB == TrafficLightType.GENERIC) {
            return false;
        }

        // At least one PROTECTED: apply strict geometric conflict check
        return geometricOppositeConflict(movA, movB);
    }

    private static boolean geometricOppositeConflict(Movement movA, Movement movB) {
        if (movA == Movement.RIGHT || movB == Movement.RIGHT) {
            return false;
        }

        if (movA == Movement.STRAIGHT && movB == Movement.STRAIGHT) {
            return false;
        }

        if (movA == Movement.LEFT && movB == Movement.LEFT) {
            return false;
        }
        return true;
    }

}