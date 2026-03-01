package org.example.trafficsim.signal;

import org.example.trafficsim.model.LaneDeclaration;
import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.Road;

// Static conflict table for a 4-arm intersection.
public final class ConflictMatrix {

    private ConflictMatrix() {}

    // Returns true if movement (fromA, movA) conflicts with movement (fromB, movB).
    public static boolean conflict(Road fromA, Movement movA, Road fromB, Movement movB) {
        if (fromA == fromB) return false;
        // RIGHT does not conflict — short arc, does not enter the main intersection area
        if (movA == Movement.RIGHT) return false;
        if (movB == Movement.RIGHT) return false;

        // PERMISSIVE_LEFT: no conflict only against the OPPOSITE road
        // (driver yields to oncoming traffic; both directions belong to the same phase).
        // Against PERPENDICULAR roads PERMISSIVE_LEFT conflicts like a normal left turn.
        if (movA == Movement.PERMISSIVE_LEFT && Movement.opposite(fromA) == fromB) return false;
        if (movB == Movement.PERMISSIVE_LEFT && Movement.opposite(fromB) == fromA) return false;

        if (Movement.opposite(fromA) == fromB) {
            // Opposite roads
            if (movA == Movement.STRAIGHT && movB == Movement.STRAIGHT) return false;
            if (movA == Movement.LEFT     && movB == Movement.LEFT)     return false;
            // Handle PERMISSIVE_LEFT vs LEFT and vice versa (opposite left turns)
            if (movA == Movement.PERMISSIVE_LEFT && movB == Movement.LEFT)          return false;
            if (movA == Movement.LEFT            && movB == Movement.PERMISSIVE_LEFT) return false;
            if (movA == Movement.PERMISSIVE_LEFT && movB == Movement.PERMISSIVE_LEFT) return false;
            return true; // STRAIGHT vs LEFT/PERMISSIVE_LEFT or vice versa
        }

        // Perpendicular roads — every combination of STRAIGHT/LEFT/PERMISSIVE_LEFT conflicts
        return true;
    }

    // Returns true if two concrete signals (road, lane, movement) conflict with each other.
    // Delegates to conflict() — a signal is already a specific movement, not a whole lane.
    public static boolean signalsConflict(LaneSignal a, LaneSignal b) {
        return conflict(a.road(), a.movement(), b.road(), b.movement());
    }

    // Returns true if two lanes have at least one conflicting movement.
    // Conservative rule: if ANY movement from lane A can conflict
    // with ANY movement from lane B, both lanes cannot be green simultaneously.
    // Still used by legacy tests; new phases are built via signalsConflict.
    public static boolean lanesConflict(LaneDeclaration a, LaneDeclaration b) {
        if (a.road() == b.road()) return false;
        for (Movement mA : a.movements()) {
            for (Movement mB : b.movements()) {
                if (conflict(a.road(), mA, b.road(), mB)) return true;
            }
        }
        return false;
    }
}
