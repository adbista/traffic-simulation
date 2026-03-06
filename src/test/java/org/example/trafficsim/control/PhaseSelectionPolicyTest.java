package org.example.trafficsim.control;

import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.metrics.FlowEmaTracker;
import org.example.trafficsim.metrics.GreenStepTracker;
import org.example.trafficsim.model.*;
import org.example.trafficsim.signal.Phase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PhaseSelectionPolicy} scoring logic:
 * flow (EMA), queue depth, age anti-starvation, priority lane weighting,
 * and the {@link PhaseSelectionPolicyBuilder}.
 */
class PhaseSelectionPolicyTest {

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    /**
     * Two-position registry:
     *   pos 0 = NORTH lane 0
     *   pos 1 = SOUTH lane 0
     */
    private static LaneRegistry twoLaneRegistry() {
        int[][] byRoadAndLane = new int[4][];
        byRoadAndLane[Road.NORTH.ordinal()] = new int[]{0};
        byRoadAndLane[Road.WEST.ordinal()]  = new int[]{};
        byRoadAndLane[Road.SOUTH.ordinal()] = new int[]{1};
        byRoadAndLane[Road.EAST.ordinal()]  = new int[]{};
        return new LaneRegistry(
                byRoadAndLane,
                List.of(Road.NORTH, Road.SOUTH),
                List.of(0, 0)
        );
    }

    private static final PhaseTiming TIMING = new PhaseTiming(1, 60, 0, 0);

    private static Phase phaseNorth() {
        int[] masks = new int[]{MovementMask.STRAIGHT, 0};
        return new Phase("NORTH", TIMING, 0b01L, masks,
                List.of(new LaneSignal(Road.NORTH, 0, MovementMask.STRAIGHT, TrafficLightType.GENERIC)));
    }

    private static Phase phaseSouth() {
        int[] masks = new int[]{0, MovementMask.STRAIGHT};
        return new Phase("SOUTH", TIMING, 0b10L, masks,
                List.of(new LaneSignal(Road.SOUTH, 0, MovementMask.STRAIGHT, TrafficLightType.GENERIC)));
    }

    /** Adds vehicle and notifies the flow tracker with the returned posId. */
    private static int add(TrafficQueues queues, FlowEmaTracker flow, Vehicle v) {
        int posId = queues.addVehicle(v);
        flow.onArrival(posId);
        return posId;
    }

    private static Vehicle northVehicle(String id) {
        return new Vehicle(id, Road.NORTH, Road.SOUTH, 0, 0, 0);
    }

    private static Vehicle southVehicle(String id) {
        return new Vehicle(id, Road.SOUTH, Road.NORTH, 0, 0, 0);
    }

    private PhaseSelectionPolicy makePolicy(double flowW, double queueW, double ageW, long cap,
                                            FlowEmaTracker flow, GreenStepTracker green,
                                            Map<Integer, Double> weights, List<Phase> phases) {
        return new PhaseSelectionPolicy(flowW, queueW, ageW, cap, flow, green, weights, phases);
    }

    // -----------------------------------------------------------------------
    // Score: empty / skipped
    // -----------------------------------------------------------------------

    @Test
    void score_emptyPhaseNoFlow_returnsZero() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);
        Phase north = phaseNorth();
        PhaseSelectionPolicy policy = makePolicy(10.0, 1.0, 0.01, 200L, flow, green,
                Map.of(0, 1.0, 1, 1.0), List.of(north, phaseSouth()));

        assertEquals(0.0, policy.score(queues, 1L, north), 1e-9);
    }

    @Test
    void score_zeroQueueZeroFlow_phaseIsSkipped() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);
        Phase north = phaseNorth();
        PhaseSelectionPolicy policy = makePolicy(10.0, 1.0, 0.01, 200L, flow, green,
                Map.of(0, 1.0, 1, 1.0), List.of(north, phaseSouth()));

        green.markGreenNow(0, 0); // even with large age, no vehicles → skip
        assertEquals(0.0, policy.score(queues, 100L, north), 1e-9);
    }

    // -----------------------------------------------------------------------
    // Score: queue component
    // -----------------------------------------------------------------------

    @Test
    void score_queueOnly_proportionalToLength() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1"));
        queues.addVehicle(northVehicle("v2"));
        queues.addVehicle(northVehicle("v3"));

        Phase north = phaseNorth();
        // flowW=0, queueW=1, ageW=0 → score = laneW * (0 + q + 0)
        PhaseSelectionPolicy policy = makePolicy(0.0, 1.0, 0.0, 200L, flow, green,
                Map.of(0, 1.0, 1, 1.0), List.of(north, phaseSouth()));

        assertEquals(3.0, policy.score(queues, 1L, north), 1e-9);
    }

    // -----------------------------------------------------------------------
    // Score: flow component (EMA)
    // -----------------------------------------------------------------------

    @Test
    void score_flowDominatesQueue_phaseWithHigherFlowWins() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        // beta=1 so intensity = arrivals in current step
        FlowEmaTracker flow = new FlowEmaTracker(2, 1.0);
        GreenStepTracker green = new GreenStepTracker(2);

        // Step 1: south gets 1 arrival → its EMA becomes 1 then decays to 0 next step with beta=1
        add(queues, flow, southVehicle("s1"));
        flow.advanceStep(); // south EMA=1; north EMA=0

        // Step 2: north gets 2 new arrivals; south has none
        add(queues, flow, northVehicle("v1"));
        add(queues, flow, northVehicle("v2"));
        flow.advanceStep(); // north EMA=2; south EMA=0 (0 arrivals × beta=1)

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        Map<Integer, Double> weights = Map.of(0, 1.0, 1, 1.0);
        // flowW=10, queueW=1, ageW=0
        PhaseSelectionPolicy policy = makePolicy(10.0, 1.0, 0.0, 1L, flow, green, weights,
                List.of(north, south));

        // North: 1 * (10*2 + 1*2 + 0) = 22.0
        // South: 1 * (10*0 + 1*1 + 0) = 1.0
        assertEquals(22.0, policy.score(queues, 2L, north), 1e-9);
        assertEquals(1.0,  policy.score(queues, 2L, south), 1e-9);
    }

    @Test
    void score_preemptiveGreen_zeroQueueWithFlowGetsScore() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 1.0);
        GreenStepTracker green = new GreenStepTracker(2);

        // Add a vehicle to north, record arrival, advance EMA, then drain the queue
        int posId = queues.addVehicle(northVehicle("v1"));
        flow.onArrival(posId);
        flow.advanceStep(); // north EMA=1.0
        queues.poll(0);     // q=0 now, flow=1.0

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        Map<Integer, Double> weights = Map.of(0, 1.0, 1, 1.0);
        PhaseSelectionPolicy policy = makePolicy(10.0, 1.0, 0.0, 200L, flow, green, weights,
                List.of(north, south));

        // North: q=0, flow=1.0 → NOT skipped; score = 1*(10*1 + 0 + ~0.01) > 0
        double scoreNorth = policy.score(queues, 1L, north);
        double scoreSouth = policy.score(queues, 1L, south); // q=0, flow=0 → skipped = 0

        assertTrue(scoreNorth > 0, "Phase with positive flow and empty queue should still score");
        assertEquals(0.0, scoreSouth, 1e-9);
    }

    // -----------------------------------------------------------------------
    // Score: queue tiebreaker
    // -----------------------------------------------------------------------

    @Test
    void score_queueBreaksTieWhenFlowEqual() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10); // no arrivals registered → flow=0
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1"));
        queues.addVehicle(northVehicle("v2"));
        queues.addVehicle(northVehicle("v3"));
        queues.addVehicle(southVehicle("s1"));

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        Map<Integer, Double> weights = Map.of(0, 1.0, 1, 1.0);
        PhaseSelectionPolicy policy = makePolicy(0.0, 1.0, 0.0, 200L, flow, green, weights,
                List.of(north, south));

        assertEquals(3.0, policy.score(queues, 1L, north), 1e-9);
        assertEquals(1.0, policy.score(queues, 1L, south), 1e-9);
    }

    // -----------------------------------------------------------------------
    // Score: age component
    // -----------------------------------------------------------------------

    @Test
    void score_ageAntiStarvation_olderPhaseGetsHigherScore() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1"));
        queues.addVehicle(southVehicle("s1"));

        green.markGreenNow(0, 90); // north last green at 90
        green.markGreenNow(1, 10); // south last green at 10

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        Map<Integer, Double> weights = Map.of(0, 1.0, 1, 1.0);
        // flowW=0, queueW=0, ageW=1 → only age counts
        PhaseSelectionPolicy policy = makePolicy(0.0, 0.0, 1.0, 200L, flow, green, weights,
                List.of(north, south));

        long stepNo = 100L;
        assertEquals(10.0, policy.score(queues, stepNo, north), 1e-9); // age=10
        assertEquals(90.0, policy.score(queues, stepNo, south), 1e-9); // age=90
    }

    @Test
    void score_ageCappedAtAgeCapSteps() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1"));
        green.markGreenNow(0, 0); // age = stepNo at evaluation

        Phase north = phaseNorth();
        Map<Integer, Double> weights = Map.of(0, 1.0, 1, 1.0);
        // flowW=0, queueW=0, ageW=1, cap=50
        PhaseSelectionPolicy policy = makePolicy(0.0, 0.0, 1.0, 50L, flow, green, weights,
                List.of(north, phaseSouth()));

        // age=200 → capped to 50
        assertEquals(50.0, policy.score(queues, 200L, north), 1e-9);
        // age=25 → not capped
        assertEquals(25.0, policy.score(queues, 25L,  north), 1e-9);
    }

    // -----------------------------------------------------------------------
    // select()
    // -----------------------------------------------------------------------

    @Test
    void select_returnsHigherScoringPhaseAsBest() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1"));
        queues.addVehicle(northVehicle("v2"));
        queues.addVehicle(northVehicle("v3"));
        queues.addVehicle(southVehicle("s1"));

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        Map<Integer, Double> weights = Map.of(0, 1.0, 1, 1.0);
        PhaseSelectionPolicy policy = makePolicy(0.0, 1.0, 0.0, 200L, flow, green, weights,
                List.of(north, south));

        PolicySelection result = policy.select(queues, 1L, south);

        assertEquals(north, result.bestPhaseScore().phase());
        assertEquals(south, result.activePhaseScore().phase());
        assertTrue(result.bestPhaseScore().score() > result.activePhaseScore().score());
    }

    @Test
    void select_currentPhaseIsBestWhenItHasHighestScore() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1"));

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        PhaseSelectionPolicy policy = makePolicy(10.0, 1.0, 0.01, 200L, flow, green,
                Map.of(0, 1.0, 1, 1.0), List.of(north, south));

        PolicySelection result = policy.select(queues, 1L, north);

        assertEquals(north, result.bestPhaseScore().phase());
        assertEquals(north, result.activePhaseScore().phase());
    }

    // -----------------------------------------------------------------------
    // Priority lane tests
    // -----------------------------------------------------------------------

    @Test
    void priority_highWeightPhaseBeatsLowWeightWithSameQueue() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1")); // pos 0: q=1
        queues.addVehicle(southVehicle("s1")); // pos 1: q=1

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        // North HIGH (4.0), South LOW (1.0)
        PhaseSelectionPolicy policy = makePolicy(0.0, 1.0, 0.0, 200L, flow, green,
                Map.of(0, 4.0, 1, 1.0), List.of(north, south));

        assertEquals(4.0, policy.score(queues, 1L, north), 1e-9);
        assertEquals(1.0, policy.score(queues, 1L, south), 1e-9);
    }

    @Test
    void priority_highWeightBeatsLowWeightEvenWithFewerVehicles() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        // North HIGH (4.0): 1 vehicle; South LOW (1.0): 3 vehicles
        queues.addVehicle(northVehicle("v1"));
        queues.addVehicle(southVehicle("s1"));
        queues.addVehicle(southVehicle("s2"));
        queues.addVehicle(southVehicle("s3"));

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        PhaseSelectionPolicy policy = makePolicy(0.0, 1.0, 0.0, 200L, flow, green,
                Map.of(0, 4.0, 1, 1.0), List.of(north, south));

        // North: 4.0*1 = 4.0; South: 1.0*3 = 3.0
        assertEquals(4.0, policy.score(queues, 1L, north), 1e-9);
        assertEquals(3.0, policy.score(queues, 1L, south), 1e-9);

        PolicySelection selection = policy.select(queues, 1L, south);
        assertEquals(north, selection.bestPhaseScore().phase());
    }

    @Test
    void priority_highWeightBoostsFlowScore() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 1.0); // beta=1
        GreenStepTracker green = new GreenStepTracker(2);

        // Step 1: south 5 arrivals, north 0 → south EMA=5, north EMA=0 at end of step 1
        add(queues, flow, southVehicle("s1")); add(queues, flow, southVehicle("s2"));
        add(queues, flow, southVehicle("s3")); add(queues, flow, southVehicle("s4"));
        add(queues, flow, southVehicle("s5"));
        flow.advanceStep(); // south EMA=5, north EMA=0

        // Step 2: north 3 arrivals, south 0 → north EMA=3, south EMA=0 (beta=1)
        add(queues, flow, northVehicle("v1"));
        add(queues, flow, northVehicle("v2"));
        add(queues, flow, northVehicle("v3"));
        flow.advanceStep(); // north EMA=3, south EMA=0

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        // North HIGH (4.0), South LOW (1.0); flowW=10, queueW=1, ageW=0
        PhaseSelectionPolicy policy = makePolicy(10.0, 1.0, 0.0, 200L, flow, green,
                Map.of(0, 4.0, 1, 1.0), List.of(north, south));

        // North: 4.0 * (10*3 + 1*3 + 0) = 4.0 * 33 = 132.0
        // South: 1.0 * (10*0 + 1*5 + 0) = 5.0
        assertEquals(132.0, policy.score(queues, 2L, north), 1e-9);
        assertEquals(5.0,   policy.score(queues, 2L, south), 1e-9);
    }

    @Test
    void priority_mediumWeightBetweenHighAndLow() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1")); // pos 0
        queues.addVehicle(southVehicle("s1")); // pos 1

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        // North HIGH (4.0), South MEDIUM (2.0)
        PhaseSelectionPolicy policy = makePolicy(0.0, 1.0, 0.0, 200L, flow, green,
                Map.of(0, 4.0, 1, 2.0), List.of(north, south));

        double scoreNorth = policy.score(queues, 1L, north); // 4.0
        double scoreSouth = policy.score(queues, 1L, south); // 2.0
        assertTrue(scoreNorth > scoreSouth);
        assertTrue(scoreSouth > 1.0, "MEDIUM priority (2.0) should score higher than LOW (1.0)");
    }

    @Test
    void priority_extremeStarvationEventuallyOverridesPriority() {
        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);

        queues.addVehicle(northVehicle("v1")); // pos 0 HIGH (4.0)
        queues.addVehicle(southVehicle("s1")); // pos 1 LOW  (1.0)

        Phase north = phaseNorth();
        Phase south = phaseSouth();
        Map<Integer, Double> weights = Map.of(0, 4.0, 1, 1.0);
        // flowW=0, queueW=1, ageW=2, cap=200 — age is significant
        PhaseSelectionPolicy policy = makePolicy(0.0, 1.0, 2.0, 200L, flow, green, weights,
                List.of(north, south));

        // Equal age=10 → north wins due to priority
        green.markGreenNow(0, 90);
        green.markGreenNow(1, 90);
        // North: 4.0*(1+2*10)=4.0*21=84; South: 1.0*(1+2*10)=21
        assertTrue(policy.score(queues, 100L, north) > policy.score(queues, 100L, south));

        // North just got green (age=10), south starved for 100 steps
        green.markGreenNow(0, 90); // north age=10 at step 100
        green.markGreenNow(1, 0);  // south age=100 at step 100
        // North: 4.0*(1 + 2*10) = 84; South: 1.0*(1 + 2*100) = 201
        double scoreNorth = policy.score(queues, 100L, north);
        double scoreSouth = policy.score(queues, 100L, south);
        assertTrue(scoreSouth > scoreNorth, "Extreme starvation should overcome priority weight");
    }

    // -----------------------------------------------------------------------
    // PhaseSelectionPolicyBuilder
    // -----------------------------------------------------------------------

    @Test
    void builder_buildsWithAllRequiredFields() {
        Phase north = phaseNorth();
        Phase south = phaseSouth();
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);
        Map<Integer, Double> weights = Map.of(0, 1.0, 1, 1.0);

        PhaseSelectionPolicy policy = new PhaseSelectionPolicyBuilder()
                .phases(List.of(north, south))
                .laneWeights(weights)
                .flowProvider(flow)
                .greenProvider(green)
                .build();

        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        queues.addVehicle(northVehicle("v1"));

        PolicySelection result = policy.select(queues, 1L, south);
        assertEquals(north, result.bestPhaseScore().phase());
    }

    @Test
    void builder_customWeightsOverrideDefaults() {
        Phase north = phaseNorth();
        Phase south = phaseSouth();
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);
        Map<Integer, Double> weights = Map.of(0, 2.0, 1, 3.0);

        PhaseSelectionPolicy policy = new PhaseSelectionPolicyBuilder()
                .flowWeight(5.0)
                .queueWeight(2.0)
                .ageWeight(0.0)
                .ageCapSteps(100L)
                .phases(List.of(north, south))
                .laneWeights(weights)
                .flowProvider(flow)
                .greenProvider(green)
                .build();

        LaneRegistry reg = twoLaneRegistry();
        TrafficQueues queues = new TrafficQueues(reg);
        queues.addVehicle(northVehicle("v1"));

        // North: 2.0 * (5*0 + 2*1 + 0) = 4.0
        assertEquals(4.0, policy.score(queues, 1L, north), 1e-9);
    }

    @Test
    void builder_throwsWhenPhasesNotSet() {
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);
        Map<Integer, Double> weights = Map.of(0, 1.0);
        assertThrows(IllegalStateException.class, () ->
                new PhaseSelectionPolicyBuilder()
                        .laneWeights(weights).flowProvider(flow).greenProvider(green).build());
    }

    @Test
    void builder_throwsWhenLaneWeightsNotSet() {
        Phase north = phaseNorth();
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        GreenStepTracker green = new GreenStepTracker(2);
        assertThrows(IllegalStateException.class, () ->
                new PhaseSelectionPolicyBuilder()
                        .phases(List.of(north)).flowProvider(flow).greenProvider(green).build());
    }

    @Test
    void builder_throwsWhenFlowProviderNotSet() {
        Phase north = phaseNorth();
        GreenStepTracker green = new GreenStepTracker(2);
        Map<Integer, Double> weights = Map.of(0, 1.0);
        assertThrows(IllegalStateException.class, () ->
                new PhaseSelectionPolicyBuilder()
                        .phases(List.of(north)).laneWeights(weights).greenProvider(green).build());
    }

    @Test
    void builder_throwsWhenGreenProviderNotSet() {
        Phase north = phaseNorth();
        FlowEmaTracker flow = new FlowEmaTracker(2, 0.10);
        Map<Integer, Double> weights = Map.of(0, 1.0);
        assertThrows(IllegalStateException.class, () ->
                new PhaseSelectionPolicyBuilder()
                        .phases(List.of(north)).laneWeights(weights).flowProvider(flow).build());
    }
}
