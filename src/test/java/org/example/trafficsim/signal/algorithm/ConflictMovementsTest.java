package org.example.trafficsim.signal.algorithm;

import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.MovementMask;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.TrafficLightType;
import org.example.trafficsim.signal.ConflictMovements;
import org.example.trafficsim.model.LaneSignal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link ConflictMovements} conflict-detection rules.
 *
 * Key rules under test:
 *  - Same road → never conflict
 *  - Opposite road, both GENERIC → never conflict (yield handled at runtime by YieldCheck)
 *  - Opposite road, at least one PROTECTED → geometric conflict check applies
 *  - Perpendicular roads → always conflict unless both are RIGHT turns
 */
class ConflictMovementsTest {

    private static LaneSignal sig(Road road, Movement mov, TrafficLightType type) {
        return new LaneSignal(road, 0, MovementMask.bit(mov), type);
    }

    // -----------------------------------------------------------------------
    // Same road
    // -----------------------------------------------------------------------

    @Test
    void sameRoad_neverConflict() {
        for (Movement movA : Movement.values()) {
            for (Movement movB : Movement.values()) {
                assertFalse(
                    ConflictMovements.conflict(Road.NORTH, movA, TrafficLightType.GENERIC,
                                               Road.NORTH, movB, TrafficLightType.GENERIC),
                    "Same road should never conflict: " + movA + " vs " + movB
                );
            }
        }
    }

    // -----------------------------------------------------------------------
    // Opposite roads, both GENERIC → no conflict (yield handled elsewhere)
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "GENERIC {0} vs GENERIC {1}")
    @CsvSource({
        "LEFT,     LEFT",
        "LEFT,     STRAIGHT",
        "LEFT,     RIGHT",
        "STRAIGHT, LEFT",
        "STRAIGHT, STRAIGHT",
        "STRAIGHT, RIGHT",
        "RIGHT,    LEFT",
        "RIGHT,    STRAIGHT",
        "RIGHT,    RIGHT"
    })
    void oppositeBothGeneric_neverConflict(String movA, String movB) {
        assertFalse(
            ConflictMovements.conflict(
                Road.NORTH, Movement.valueOf(movA), TrafficLightType.GENERIC,
                Road.SOUTH, Movement.valueOf(movB), TrafficLightType.GENERIC
            ),
            "Opposite + both GENERIC must never conflict: " + movA + " vs " + movB
        );
    }

    // -----------------------------------------------------------------------
    // Opposite roads, at least one PROTECTED – geometric rules
    // -----------------------------------------------------------------------

    @Test
    void opposite_protectedLeft_vs_genericStraight_conflicts() {
        assertTrue(
            ConflictMovements.conflict(
                Road.NORTH, Movement.LEFT,     TrafficLightType.PROTECTED,
                Road.SOUTH, Movement.STRAIGHT, TrafficLightType.GENERIC
            )
        );
    }

    @Test
    void opposite_protectedLeft_vs_genericLeft_noConflict() {
        // Both LEFT from opposite directions: geometricOppositeConflict(LEFT, LEFT) → false
        assertFalse(
            ConflictMovements.conflict(
                Road.NORTH, Movement.LEFT, TrafficLightType.PROTECTED,
                Road.SOUTH, Movement.LEFT, TrafficLightType.GENERIC
            )
        );
    }

    @Test
    void opposite_protectedStraight_vs_genericStraight_noConflict() {
        // Both STRAIGHT from opposite directions → no geometric conflict
        assertFalse(
            ConflictMovements.conflict(
                Road.NORTH, Movement.STRAIGHT, TrafficLightType.PROTECTED,
                Road.SOUTH, Movement.STRAIGHT, TrafficLightType.GENERIC
            )
        );
    }

    @Test
    void opposite_protectedLeft_vs_genericRight_noConflict() {
        // RIGHT from either direction → always safe
        assertFalse(
            ConflictMovements.conflict(
                Road.NORTH, Movement.LEFT,  TrafficLightType.PROTECTED,
                Road.SOUTH, Movement.RIGHT, TrafficLightType.GENERIC
            )
        );
    }

    @Test
    void opposite_protectedStraight_vs_genericLeft_conflicts() {
        // STRAIGHT(PROTECTED) vs LEFT(GENERIC): straight vs left = conflict
        assertTrue(
            ConflictMovements.conflict(
                Road.NORTH, Movement.STRAIGHT, TrafficLightType.PROTECTED,
                Road.SOUTH, Movement.LEFT,     TrafficLightType.GENERIC
            )
        );
    }


    //
    // Symmetry – conflict(a,b) == conflict(b,a)
    //

    @Test
    void conflict_isSymmetric() {
        Road[] roads = Road.values();
        Movement[] movs = Movement.values();
        TrafficLightType[] types = { TrafficLightType.GENERIC, TrafficLightType.PROTECTED };

        for (Road rA : roads) {
            for (Road rB : roads) {
                if (rA == rB) continue;
                for (Movement mA : movs) {
                    for (Movement mB : movs) {
                        for (TrafficLightType tA : types) {
                            for (TrafficLightType tB : types) {
                                boolean ab = ConflictMovements.conflict(rA, mA, tA, rB, mB, tB);
                                boolean ba = ConflictMovements.conflict(rB, mB, tB, rA, mA, tA);
                                assertEquals(ab, ba,
                                    "Conflict asymmetry: " + rA + "-" + mA + "(" + tA + ") vs "
                                    + rB + "-" + mB + "(" + tB + ")");
                            }
                        }
                    }
                }
            }
        }
    }

    // signalsConflict() delegates to conflict() correctly

    @Test
    void signalsConflict_wrapperMatchesDirectCall() {
        LaneSignal a = sig(Road.NORTH, Movement.LEFT, TrafficLightType.PROTECTED);
        LaneSignal b = sig(Road.SOUTH, Movement.STRAIGHT, TrafficLightType.GENERIC);

        boolean viaWrapper = ConflictMovements.signalsConflict(a, b);
        // Each test signal carries exactly one movement bit, so extract it for the direct call
        Movement movA = MovementMask.movementsFromMask(a.movementMask()).get(0);
        Movement movB = MovementMask.movementsFromMask(b.movementMask()).get(0);
        boolean viaDirect = ConflictMovements.conflict(
                a.road(), movA, a.trafficLightType(),
                b.road(), movB, b.trafficLightType());

        assertEquals(viaDirect, viaWrapper);
    }
}
