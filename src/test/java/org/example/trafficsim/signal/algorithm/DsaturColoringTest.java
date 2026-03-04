package org.example.trafficsim.signal.algorithm;

import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.MovementMask;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.TrafficLightType;
import org.example.trafficsim.signal.ConflictMovements;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class DsaturColoringTest {

    private final DsaturColoring dsatur = new DsaturColoring();


    private static LaneSignal sig(Road road, Movement mov, TrafficLightType type) {
        return new LaneSignal(road, 0, MovementMask.bit(mov), type);
    }

    private static LaneSignal sigMerged(Road road, int mask) {
        return new LaneSignal(road, 0, mask, TrafficLightType.GENERIC);
    }


    private void assertColoringValid(List<LaneSignal> signals, int[] colors) {
        for (int i = 0; i < signals.size(); i++) {
            for (int j = i + 1; j < signals.size(); j++) {
                if (ConflictMovements.signalsConflict(signals.get(i), signals.get(j))) {
                    assertNotEquals(colors[i], colors[j],
                            "Conflicting signals share color " + colors[i] + ": "
                                    + signals.get(i) + " vs " + signals.get(j));
                }
            }
        }
    }

    private void assertAllColored(int[] colors) {
        for (int i = 0; i < colors.length; i++) {
            assertTrue(colors[i] >= 0, "Signal " + i + " was not colored (value=" + colors[i] + ")");
        }
    }


    @Test
    void emptyInput_returnsEmptyArray() {
        int[] colors = dsatur.color(List.of(), ConflictMovements::signalsConflict);
        assertEquals(0, colors.length);
    }

    @Test
    void singleSignal_getsColorZero() {
        LaneSignal s = sig(Road.NORTH, Movement.STRAIGHT, TrafficLightType.GENERIC);
        int[] colors = dsatur.color(List.of(s), ConflictMovements::signalsConflict);

        assertEquals(1, colors.length);
        assertEquals(0, colors[0]);
    }


    @Test
    void twoNonConflictingSignals_sameColor() {
        // Opposite roads, both GENERIC -> no conflict -> can share a phase
        LaneSignal ns = sig(Road.NORTH, Movement.STRAIGHT, TrafficLightType.GENERIC);
        LaneSignal ss = sig(Road.SOUTH, Movement.STRAIGHT, TrafficLightType.GENERIC);

        assertFalse(ConflictMovements.signalsConflict(ns, ss));

        int[] colors = dsatur.color(List.of(ns, ss), ConflictMovements::signalsConflict);

        assertAllColored(colors);
        // assert proper size
        assertEquals(colors[0], colors[1], "Non-conflicting signals should share a color: ");
    }

    @Test
    void twoConflictingSignals_differentColors() {
        // Perpendicular roads always conflict
        LaneSignal north = sig(Road.NORTH, Movement.STRAIGHT, TrafficLightType.GENERIC);
        LaneSignal east  = sig(Road.EAST,  Movement.STRAIGHT, TrafficLightType.GENERIC);

        assertTrue(ConflictMovements.signalsConflict(north, east));

        int[] colors = dsatur.color(List.of(north, east), ConflictMovements::signalsConflict);

        assertAllColored(colors);
        assertNotEquals(colors[0], colors[1], "Conflicting signals must get different colors");
    }

    // ── standard 4-road all-GENERIC topology ─────────────────────────────────

    @Test
    void fourRoads_allGenericMergedPerRoad_exactlyTwoColors() {
        // Each road has one merged GENERIC group covering all three movements.
        // Conflict graph: N<->E, N<->W, S<->E, S<->W (perpendicular); N–S and E–W are compatible.
        // Chromatic number = 2.
        LaneSignal n = sigMerged(Road.NORTH, MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT);
        LaneSignal s = sigMerged(Road.SOUTH, MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT);
        LaneSignal e = sigMerged(Road.EAST,  MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT);
        LaneSignal w = sigMerged(Road.WEST,  MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT);

        List<LaneSignal> signals = List.of(n, s, e, w);
        int[] colors = dsatur.color(signals, ConflictMovements::signalsConflict);

        assertAllColored(colors);
        assertColoringValid(signals, colors);

        long distinctColors = Arrays.stream(colors).distinct().count();
        assertEquals(2, distinctColors,
                "4-road all-GENERIC graph has chromatic number 2, got colors: " + Arrays.toString(colors));

        // N and S must share a color; E and W must share the other color
        assertEquals(colors[0], colors[1], "NORTH and SOUTH should be in the same phase");
        assertEquals(colors[2], colors[3], "EAST and WEST should be in the same phase");
        assertNotEquals(colors[0], colors[2], "NS phase and EW phase must differ");
    }

    // ── protected-left topology

    @Test
    void protectedLeft_conflictsWithOppositeGenericGroup_separatePhases() {
        // NORTH protected LEFT conflicts with SOUTH generic group that contains STRAIGHT.
        // They must land in different phases.
        LaneSignal northProtLeft = sig(Road.NORTH, Movement.LEFT, TrafficLightType.PROTECTED);
        LaneSignal southGeneric  = sigMerged(Road.SOUTH,
                MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT);

        assertTrue(ConflictMovements.signalsConflict(northProtLeft, southGeneric),
                "PROTECTED LEFT vs GENERIC group containing STRAIGHT should conflict");

        int[] colors = dsatur.color(List.of(northProtLeft, southGeneric), ConflictMovements::signalsConflict);

        assertAllColored(colors);
        assertNotEquals(colors[0], colors[1],
                "Conflicting PROTECTED left and opposite GENERIC group must be in different phases");
    }

    @Test
    void protectedLeft_compatibleWithOppositeGenericRight_samePhase() {
        // NORTH protected LEFT does NOT conflict with SOUTH generic RIGHT
        // (RIGHT from opposite is always safe geometrically)
        LaneSignal northProtLeft = sig(Road.NORTH, Movement.LEFT, TrafficLightType.PROTECTED);
        LaneSignal southRight    = sig(Road.SOUTH, Movement.RIGHT, TrafficLightType.GENERIC);

        assertFalse(ConflictMovements.signalsConflict(northProtLeft, southRight));

        int[] colors = dsatur.color(List.of(northProtLeft, southRight), ConflictMovements::signalsConflict);

        assertAllColored(colors);
        assertEquals(colors[0], colors[1],
                "PROTECTED LEFT and opposite GENERIC RIGHT should share a phase");
    }

    // ── triangle graph (chromatic number = 3) ──────────────────────────────

    @Test
    void triangleConflictGraph_needsThreeColors() {
        // Three mutually-conflicting signals:
        //   A = NORTH PROTECTED LEFT
        //   B = SOUTH GENERIC STRAIGHT  (conflicts A: PROTECTED LEFT vs GENERIC STRAIGHT)
        //   C = EAST  GENERIC STRAIGHT  (conflicts A and B: perpendicular)
        LaneSignal a = sig(Road.NORTH, Movement.LEFT,     TrafficLightType.PROTECTED);
        LaneSignal b = sig(Road.SOUTH, Movement.STRAIGHT, TrafficLightType.GENERIC);
        LaneSignal c = sig(Road.EAST,  Movement.STRAIGHT, TrafficLightType.GENERIC);

        assertTrue(ConflictMovements.signalsConflict(a, b), "A-B should conflict");
        assertTrue(ConflictMovements.signalsConflict(a, c), "A-C should conflict");
        assertTrue(ConflictMovements.signalsConflict(b, c), "B-C should conflict");

        List<LaneSignal> signals = List.of(a, b, c);
        int[] colors = dsatur.color(signals, ConflictMovements::signalsConflict);

        assertAllColored(colors);
        assertColoringValid(signals, colors);

        long distinctColors = Arrays.stream(colors).distinct().count();
        assertEquals(3, distinctColors, "Triangle graph needs 3 colors, got: " + Arrays.toString(colors));
    }

    // ── general validity property ─

    @Test
    void generalValidity_allSignalsColored_noConflictingPairSharesColor() {
        // Mix of GENERIC and PROTECTED signals across all 4 roads + 2 lanes
        List<LaneSignal> signals = List.of(
                // NORTH lane 0: generic STRAIGHT+RIGHT, lane 1: protected LEFT
                new LaneSignal(Road.NORTH, 0, MovementMask.STRAIGHT | MovementMask.RIGHT, TrafficLightType.GENERIC),
                new LaneSignal(Road.NORTH, 1, MovementMask.LEFT, TrafficLightType.PROTECTED),
                // SOUTH lane 0: generic all-movements merged
                sigMerged(Road.SOUTH, MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT),
                // EAST and WEST: each one merged GENERIC group
                sigMerged(Road.EAST, MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT),
                sigMerged(Road.WEST, MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT)
        );

        int[] colors = dsatur.color(signals, ConflictMovements::signalsConflict);

        assertAllColored(colors);
        assertColoringValid(signals, colors);
    }

    @Test
void protectedLeftAndOppositeGenericGroups_needTwoPhases() {
    LaneSignal northProtectedLeft =
            new LaneSignal(Road.NORTH, 0, MovementMask.LEFT, TrafficLightType.PROTECTED);

    LaneSignal northGenericStraightRight =
            new LaneSignal(Road.NORTH, 1,
                    MovementMask.STRAIGHT | MovementMask.RIGHT,
                    TrafficLightType.GENERIC);

    LaneSignal southGenericAll =
            new LaneSignal(Road.SOUTH, 0,
                    MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT,
                    TrafficLightType.GENERIC);

    List<LaneSignal> signals = List.of(
            northProtectedLeft,
            northGenericStraightRight,
            southGenericAll
    );

    assertTrue(ConflictMovements.signalsConflict(northProtectedLeft, southGenericAll),
            "Protected LEFT should conflict with opposite GENERIC group containing STRAIGHT");

    assertFalse(ConflictMovements.signalsConflict(northGenericStraightRight, southGenericAll),
            "Opposite GENERIC groups should be compatible");

    assertFalse(ConflictMovements.signalsConflict(northProtectedLeft, northGenericStraightRight),
            "Signals from the same road should not conflict");

    int[] colors = dsatur.color(signals, ConflictMovements::signalsConflict);

    assertAllColored(colors);
    assertColoringValid(signals, colors);

    long distinctColors = Arrays.stream(colors).distinct().count();
    assertEquals(2, distinctColors,
            "Expected exactly 2 phases, got: " + Arrays.toString(colors));

    assertNotEquals(colors[1], colors[2]); 

    assertEquals(colors[0], colors[1]);
}
}
