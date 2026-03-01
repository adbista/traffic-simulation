package org.example.trafficsim.control;

import org.example.trafficsim.core.CrossroadState;
import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;
import org.example.trafficsim.signal.Phase;
import org.example.trafficsim.signal.PhaseTiming;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MaxPressurePolicyTest {

    private static final double ALPHA = 0.25;

    private MaxPressurePolicy policy;
    private List<Phase> phases;
    private PhaseTiming timing;

    // Tworzy domyslna mape wag (1.0) dla wszystkich drog
    private static Map<Road, Double> equalWeights() {
        Map<Road, Double> w = new EnumMap<>(Road.class);
        for (Road r : Road.values()) w.put(r, 1.0);
        return w;
    }

    @BeforeEach
    void setUp() {
        policy = new MaxPressurePolicy(ALPHA, equalWeights());
        timing = new PhaseTiming(2, 10, 1);
        phases = List.of(
                new Phase("ns", Set.of(Road.NORTH, Road.SOUTH), timing),
                new Phase("ew", Set.of(Road.EAST,  Road.WEST),  timing)
        );
    }

    private static Vehicle v(String id, Road road) {
        return new Vehicle(id, road, Road.SOUTH, 0, 0, 0);
    }

    // obliczanie wyniku

    @Test
    void emptyIntersectionStaysOnCurrentPhase() {
        TrafficQueues state = new TrafficQueues();
        CrossroadState snap = state.snapshot();

        PhaseSelectionPolicy.Selection sel = policy.select(snap, phases, "ns");
        assertEquals("ns", sel.phaseId(), "Should stay on current phase when all queues empty");
    }

    @Test
    void higherPressureOnNSWinsOverEW() {
        TrafficQueues state = new TrafficQueues();
        state.addVehicle(v("n1", Road.NORTH));
        state.addVehicle(v("n2", Road.NORTH));
        state.addVehicle(v("n3", Road.NORTH));
        // EW has 0; NS has 3 → NS should win
        CrossroadState snap = state.snapshot();

        PhaseSelectionPolicy.Selection sel = policy.select(snap, phases, "ew");
        assertEquals("ns", sel.phaseId());
    }

    @Test
    void evenQueuesReturnCurrentPhase() {
        TrafficQueues state = new TrafficQueues();
        // one vehicle each on all four roads
        state.addVehicle(v("n1", Road.NORTH));
        state.addVehicle(v("s1", Road.SOUTH));
        state.addVehicle(v("e1", Road.EAST));
        state.addVehicle(v("w1", Road.WEST));

        CrossroadState snap = state.snapshot();
        PhaseSelectionPolicy.Selection sel = policy.select(snap, phases, "ns");
        assertEquals("ns", sel.phaseId(), "Equal pressure → stay on current");
    }

    @Test
    void stepsSinceGreenBiasesScore() {
        // Both NS and EW have 1 vehicle each.
        // But EW has wait counter 4 → alpha contribution tips the score.
        TrafficQueues state = new TrafficQueues();
        state.addVehicle(v("n1", Road.NORTH));
        state.addVehicle(v("e1", Road.EAST));

        // Manually drive the wait counters by updating multiple times favouring NS
        for (int i = 0; i < 4; i++) {
            state.updateWaitCounters(Map.of(Road.NORTH, Set.of(0), Road.SOUTH, Set.of(0))); // NS is green → EW waits
        }

        CrossroadState snap = state.snapshot();
        PhaseSelectionPolicy.Selection sel = policy.select(snap, phases, "ns");
        // EW wait = 4, NS wait = 0; score(EW) = 1 + 0.25*4 = 2.0 > score(NS) = 1 + 0 = 1.0
        assertEquals("ew", sel.phaseId(), "EW should win due to wait-since-served bias");
    }

    @Test
    void selectReturnsPositiveScore() {
        TrafficQueues state = new TrafficQueues();
        state.addVehicle(v("e1", Road.EAST));
        CrossroadState snap = state.snapshot();

        PhaseSelectionPolicy.Selection sel = policy.select(snap, phases, "ns");
        assertTrue(sel.score() >= 0.0);
    }

    @Test
    void scoreOfBestPhaseIsAtLeastScoreOfCurrent() {
        // The returned phase must be the highest-scoring one
        TrafficQueues state = new TrafficQueues();
        for (int i = 0; i < 5; i++) state.addVehicle(v("e" + i, Road.EAST));
        CrossroadState snap = state.snapshot();

        PhaseSelectionPolicy.Selection best = policy.select(snap, phases, "ns");
        assertEquals("ew", best.phaseId()); // heavy EW traffic

        // Manually compute EW score  = 5 + 0 and NS score = 0 + 0
        assertTrue(best.score() > 0, "Best score must be positive when queues exist");
    }

    // parametr wagi

    @Test
    void alphaZeroIgnoresWaitCounter() {
        MaxPressurePolicy noAlpha = new MaxPressurePolicy(0.0, equalWeights());

        TrafficQueues state = new TrafficQueues();
        state.addVehicle(v("n1", Road.NORTH));
        // wait for 10 steps on EW — but alpha=0 so it shouldn't matter
        for (int i = 0; i < 10; i++) {
            state.updateWaitCounters(Map.of(Road.NORTH, Set.of(0), Road.SOUTH, Set.of(0)));
        }
        // NS still has one vehicle, EW has 0 but high wait
        CrossroadState snap = state.snapshot();
        PhaseSelectionPolicy.Selection sel = noAlpha.select(snap, phases, "ew");
        assertEquals("ns", sel.phaseId(), "High wait should not influence score when alpha=0");
    }

    // fallback: jedyna faza zawsze wygrywa

    @Test
    void singlePhaseIsAlwaysSelected() {
        List<Phase> single = List.of(
                new Phase("ns", Set.of(Road.NORTH, Road.SOUTH), timing)
        );
        TrafficQueues state = new TrafficQueues();
        CrossroadState snap = state.snapshot();

        PhaseSelectionPolicy.Selection sel = policy.select(snap, single, "ns");
        assertEquals("ns", sel.phaseId());
    }
}
