package org.example.trafficsim.signal;

import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SignalFactoryTest {

    // 4-road, 1-lane each, no explicit signal config -> all defaults
    private IntersectionConfig defaultFourRoadConfig() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{1};
        m[Road.SOUTH.ordinal()] = new int[]{2};
        m[Road.EAST.ordinal()]  = new int[]{3};
        LaneRegistry reg = new LaneRegistry(m,
                List.of(Road.NORTH, Road.WEST, Road.SOUTH, Road.EAST),
                List.of(0, 0, 0, 0));
        return new IntersectionConfig(null, reg, Map.of(), Map.of());
    }

    @Test
    void defaultConfig_oneSignalPerPosition() {
        List<LaneSignal> signals = SignalFactory.createSignals(defaultFourRoadConfig());
        assertEquals(4, signals.size());
    }

    @Test
    void defaultConfig_allSignalsGeneric() {
        List<LaneSignal> signals = SignalFactory.createSignals(defaultFourRoadConfig());
        assertTrue(signals.stream().allMatch(s -> s.trafficLightType() == TrafficLightType.GENERIC));
    }

    @Test
    void defaultConfig_fullMovementMaskOnEachSignal() {
        int expected = MovementMask.LEFT | MovementMask.STRAIGHT | MovementMask.RIGHT;
        List<LaneSignal> signals = SignalFactory.createSignals(defaultFourRoadConfig());
        assertTrue(signals.stream().allMatch(s -> s.movementMask() == expected));
    }

    @Test
    void defaultConfig_signalsSortedByRoadOrdinal() {
        List<LaneSignal> signals = SignalFactory.createSignals(defaultFourRoadConfig());
        for (int i = 1; i < signals.size(); i++) {
            assertTrue(signals.get(i - 1).road().ordinal() <= signals.get(i).road().ordinal());
        }
    }

    @Test
    void customConfig_protectedLeftSignal_separateFromGeneric() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0, 1};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{};
        m[Road.EAST.ordinal()]  = new int[]{};
        LaneRegistry reg = new LaneRegistry(m,
                List.of(Road.NORTH, Road.NORTH), List.of(0, 1));
        // pos 1 = NORTH lane 1 has a single PROTECTED LEFT signal
        Map<Integer, List<SignalGroupSpec>> specs = Map.of(
                1, List.of(new SignalGroupSpec(MovementMask.LEFT, TrafficLightType.PROTECTED))
        );
        IntersectionConfig config = new IntersectionConfig(null, reg, specs, Map.of());

        List<LaneSignal> signals = SignalFactory.createSignals(config);

        boolean hasProtectedLeft = signals.stream().anyMatch(
                s -> s.trafficLightType() == TrafficLightType.PROTECTED
                        && (s.movementMask() & MovementMask.LEFT) != 0);
        assertTrue(hasProtectedLeft);
    }

    @Test
    void customConfig_multipleGroupsOnSameLane_eachBecomesOwnSignal() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{};
        m[Road.EAST.ordinal()]  = new int[]{};
        LaneRegistry reg = new LaneRegistry(m, List.of(Road.NORTH), List.of(0));
        // Two distinct signal groups on the same lane
        Map<Integer, List<SignalGroupSpec>> specs = Map.of(0, List.of(
                new SignalGroupSpec(MovementMask.STRAIGHT, TrafficLightType.GENERIC),
                new SignalGroupSpec(MovementMask.LEFT, TrafficLightType.PROTECTED)
        ));
        IntersectionConfig config = new IntersectionConfig(null, reg, specs, Map.of());

        List<LaneSignal> signals = SignalFactory.createSignals(config);

        assertEquals(2, signals.size());
    }
}
