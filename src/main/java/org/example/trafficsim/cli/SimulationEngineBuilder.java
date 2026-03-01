package org.example.trafficsim.cli;

import org.example.trafficsim.control.MaxPressurePolicy;
import org.example.trafficsim.control.PhaseSelectionPolicy;
import org.example.trafficsim.control.SignalController;
import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;
import org.example.trafficsim.signal.PhaseFactory;
import org.example.trafficsim.signal.PhaseSetGenerator;

import java.util.*;

// Builder for the simulation engine. Encapsulates configuration and construction of all
// engine components (queues, signal controller, active phase).
//
// Lane count per road is derived automatically from greenLaneIndices of the generated phases.
//
// DIP: phase selection policy is injected via policy() instead of being hardcoded.
//
// Minimal usage (default settings — 1 STRAIGHT lane per road, NS+EW phases):
//   SimulationEngine engine = new SimulationEngineBuilder().build();
public class SimulationEngineBuilder {

    private PhaseFactory phaseFactory = PhaseSetGenerator.defaults();
    // DIP: policy injected from outside; null = use default MaxPressurePolicy
    private PhaseSelectionPolicy policy = null;
    private double hysteresis = 0.10;
    private double pressureThreshold = 0.25;

    public SimulationEngineBuilder phaseFactory(PhaseFactory pf) {
        this.phaseFactory = pf;
        return this;
    }

    // DIP: injects a custom phase selection policy instead of hardcoding MaxPressurePolicy.
    public SimulationEngineBuilder policy(PhaseSelectionPolicy policy) {
        this.policy = policy;
        return this;
    }

    // Builds and returns a ready SimulationEngine.
    public SimulationEngine build() {
        List<Phase> phases = phaseFactory.create();
        Map<Road, Integer> lanes = deriveLanesFromPhases(phases);

        Map<Road, Double> weights = new EnumMap<>(Road.class);
        for (Road r : Road.values()) weights.put(r, 1.0);

        PhaseSelectionPolicy effectivePolicy = (policy != null)
                ? policy
                : new MaxPressurePolicy(pressureThreshold, weights);

        SignalController controller = new SignalController(
                phases,
                effectivePolicy,
                hysteresis
        );

        return new SimulationEngine(
                new TrafficQueues(lanes),
                new ActivePhase(phases.get(0)),
                controller
        );
    }

    // Derives lane count per road from greenLaneIndices of the generated phases.
    // For each road, takes max(index) + 1 across all phases.
    // Roads absent from all phases default to 1 lane.
    private static Map<Road, Integer> deriveLanesFromPhases(List<Phase> phases) {
        Map<Road, Integer> result = new EnumMap<>(Road.class);
        for (Road r : Road.values()) result.put(r, 1);
        for (Phase p : phases) {
            for (Map.Entry<Road, Set<Integer>> e : p.greenLaneIndices().entrySet()) {
                for (int idx : e.getValue()) {
                    result.merge(e.getKey(), idx + 1, Math::max);
                }
            }
        }
        return result;
    }
}

