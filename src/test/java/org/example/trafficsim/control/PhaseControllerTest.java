package org.example.trafficsim.control;

import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.metrics.FlowEmaTracker;
import org.example.trafficsim.metrics.GreenStepTracker;
import org.example.trafficsim.model.*;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhaseControllerTest {

    private static final PhaseTiming TIMING = new PhaseTiming(1, 5, 0, 1);

    private LaneRegistry twoLaneReg() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{1};
        m[Road.EAST.ordinal()]  = new int[]{};
        return new LaneRegistry(m, List.of(Road.NORTH, Road.SOUTH), List.of(0, 0));
    }

    private Phase phaseNorth() {
        int[] masks = new int[]{MovementMask.STRAIGHT, 0};
        return new Phase("NORTH", TIMING, 0b01L, masks,
                List.of(new LaneSignal(Road.NORTH, 0, MovementMask.STRAIGHT, TrafficLightType.GENERIC)));
    }

    private Phase phaseSouth() {
        int[] masks = new int[]{0, MovementMask.STRAIGHT};
        return new Phase("SOUTH", TIMING, 0b10L, masks,
                List.of(new LaneSignal(Road.SOUTH, 0, MovementMask.STRAIGHT, TrafficLightType.GENERIC)));
    }

    private PhaseController makeController(List<Phase> phases) {
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);
        PhaseSelectionPolicy policy = new PhaseSelectionPolicyBuilder()
                .phases(phases)
                .laneWeights(Map.of(0, 1.0, 1, 1.0))
                .flowProvider(flow)
                .greenProvider(green)
                .build();
        return new PhaseController(phases, policy, 0.10);
    }

    @Test
    void noSwitch_beforeMinGreen() {
        // timeInState starts at 0, minGreen=1, so controller returns early
        Phase north = phaseNorth();
        PhaseController ctrl = makeController(List.of(north, phaseSouth()));
        TrafficQueues queues = new TrafficQueues(twoLaneReg());
        queues.addVehicle(new Vehicle("v", Road.SOUTH, Road.NORTH, 0, 0, 0));
        ActivePhase active = new ActivePhase(north);

        ctrl.managePhaseSwitch(queues, 1L, active);

        assertTrue(active.isGreen());
        assertEquals("NORTH", active.current().id());
    }

    @Test
    void noSwitch_whenPhaseIsNotGreen() {
        Phase north = phaseNorth();
        Phase south = phaseSouth();
        PhaseController ctrl = makeController(List.of(north, south));
        TrafficQueues queues = new TrafficQueues(twoLaneReg());
        ActivePhase active = new ActivePhase(north);
        active.requestNextPhase(south); // leaves GREEN

        ctrl.managePhaseSwitch(queues, 5L, active);

        assertFalse(active.isGreen());
        assertEquals("NORTH", active.current().id()); // pending but not yet committed
    }

    @Test
    void forcedSwitch_roundRobin_atMaxGreen() {
        Phase north = phaseNorth();
        PhaseController ctrl = makeController(List.of(north, phaseSouth()));
        TrafficQueues queues = new TrafficQueues(twoLaneReg());
        queues.addVehicle(new Vehicle("v", Road.NORTH, Road.SOUTH, 0, 0, 0));
        ActivePhase active = new ActivePhase(north);
        for (int i = 0; i < 5; i++) active.manageLightState(); // timeInState = 5 = maxGreen

        ctrl.managePhaseSwitch(queues, 10L, active);

        assertFalse(active.isGreen()); // switch triggered -> RED clearance phase
    }

    @Test
    void gapOut_switchesWhenCurrentPhaseIsEmpty() {
        Phase north = phaseNorth();
        PhaseController ctrl = makeController(List.of(north, phaseSouth()));
        TrafficQueues queues = new TrafficQueues(twoLaneReg());
        queues.addVehicle(new Vehicle("v", Road.SOUTH, Road.NORTH, 0, 0, 0)); // only SOUTH
        ActivePhase active = new ActivePhase(north);
        active.manageLightState(); // timeInState=1 >= minGreen=1

        ctrl.managePhaseSwitch(queues, 5L, active);

        assertFalse(active.isGreen()); // gap-out switch triggered
    }

    @Test
    void scoreBasedSwitch_highScorePhaseWins() {
        Phase north = phaseNorth();
        Phase south = phaseSouth();
        PhaseController ctrl = makeController(List.of(north, south));
        TrafficQueues queues = new TrafficQueues(twoLaneReg());
        queues.addVehicle(new Vehicle("n", Road.NORTH, Road.SOUTH, 0, 0, 0)); // 1 on north
        for (int i = 0; i < 10; i++) {
            queues.addVehicle(new Vehicle("s" + i, Road.SOUTH, Road.NORTH, 0, i, 0)); // 10 on south
        }
        ActivePhase active = new ActivePhase(north);
        active.manageLightState(); // meet minGreen

        ctrl.managePhaseSwitch(queues, 10L, active);

        assertFalse(active.isGreen()); // south scored much higher than north
    }

    @Test
    void hysteresis_preventsSwitch_whenScoresAreSimilar() {
        Phase north = phaseNorth();
        Phase south = phaseSouth();
        PhaseController ctrl = makeController(List.of(north, south));
        TrafficQueues queues = new TrafficQueues(twoLaneReg());
        queues.addVehicle(new Vehicle("n", Road.NORTH, Road.SOUTH, 0, 0, 0));
        queues.addVehicle(new Vehicle("s", Road.SOUTH, Road.NORTH, 0, 1, 0)); // same queue depth
        ActivePhase active = new ActivePhase(north);
        active.manageLightState();

        ctrl.managePhaseSwitch(queues, 5L, active);

        // Scores are equal -> no switch (hysteresis blocks marginal improvement)
        assertTrue(active.isGreen());
    }
}
