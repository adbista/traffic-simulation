package org.example.trafficsim.signal;

import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhaseFactoryTest {

    private static final PhaseTiming TIMING = new PhaseTiming(1, 5, 0, 1);

    private IntersectionConfig fourRoadConfig() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{1};
        m[Road.SOUTH.ordinal()] = new int[]{2};
        m[Road.EAST.ordinal()]  = new int[]{3};
        LaneRegistry reg = new LaneRegistry(m,
                List.of(Road.NORTH, Road.WEST, Road.SOUTH, Road.EAST),
                List.of(0, 0, 0, 0));
        return new IntersectionConfig(TIMING, reg, Map.of(), Map.of());
    }

    @Test
    void defaultFourRoad_producesTwoPhases() {
        List<Phase> phases = new PhaseFactory().create(fourRoadConfig());
        assertEquals(2, phases.size());
    }

    @Test
    void phases_positionMasksDoNotOverlap() {
        List<Phase> phases = new PhaseFactory().create(fourRoadConfig());
        for (int i = 0; i < phases.size(); i++) {
            for (int j = i + 1; j < phases.size(); j++) {
                assertEquals(0L, phases.get(i).positionsMask() & phases.get(j).positionsMask(),
                        "Phases " + i + " and " + j + " share a position");
            }
        }
    }

    @Test
    void phases_togetherCoverAllPositions() {
        IntersectionConfig config = fourRoadConfig();
        List<Phase> phases = new PhaseFactory().create(config);
        long combined = 0L;
        for (Phase p : phases) combined |= p.positionsMask();
        long expected = (1L << config.laneRegistry().totalPositions()) - 1L;
        assertEquals(expected, combined);
    }

    @Test
    void phases_sortedByMinRoadOrdinalInGroup() {
        List<Phase> phases = new PhaseFactory().create(fourRoadConfig());
        // After DSATUR: phase 0 must contain NORTH (position 0, lowest road ordinal)
        assertTrue((phases.get(0).positionsMask() & 0b0001L) != 0);
    }

    @Test
    void phaseTimingAssignedFromConfig() {
        List<Phase> phases = new PhaseFactory().create(fourRoadConfig());
        for (Phase p : phases) {
            assertEquals(TIMING.minGreenSteps(), p.timing().minGreenSteps());
            assertEquals(TIMING.maxGreenSteps(), p.timing().maxGreenSteps());
        }
    }

    @Test
    void protectedLeft_conflictsWithOppositeStrategic_getsOwnPhase() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0, 1};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{2};
        m[Road.EAST.ordinal()]  = new int[]{};
        LaneRegistry reg = new LaneRegistry(m,
                List.of(Road.NORTH, Road.NORTH, Road.SOUTH),
                List.of(0, 1, 0));
        // NORTH lane 1: PROTECTED LEFT only (conflicts with SOUTH anything except left)
        Map<Integer, List<SignalGroupSpec>> specs = Map.of(
                1, List.of(new SignalGroupSpec(MovementMask.LEFT, TrafficLightType.PROTECTED))
        );
        IntersectionConfig config = new IntersectionConfig(TIMING, reg, specs, Map.of());

        List<Phase> phases = new PhaseFactory().create(config);

        // PROTECTED LEFT from NORTH can't share a phase with SOUTH STRAIGHT/RIGHT
        assertTrue(phases.size() >= 2);
    }

    @Test
    void emptyConfig_throws() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{};
        m[Road.EAST.ordinal()]  = new int[]{};
        LaneRegistry reg = new LaneRegistry(m, List.of(), List.of());
        IntersectionConfig config = new IntersectionConfig(TIMING, reg, Map.of(), Map.of());
        assertThrows(IllegalStateException.class, () -> new PhaseFactory().create(config));
    }
}
