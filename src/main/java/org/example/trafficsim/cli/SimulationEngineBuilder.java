package org.example.trafficsim.cli;

import org.example.trafficsim.control.MaxPressurePolicy;
import org.example.trafficsim.control.PhaseSelectionPolicy;
import org.example.trafficsim.control.SignalController;
import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.model.LaneDeclaration;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;
import org.example.trafficsim.signal.PhaseFactory;
import org.example.trafficsim.signal.PhaseSetGenerator;
import org.example.trafficsim.signal.PhaseTiming;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SimulationEngineBuilder {

    private List<LaneDeclaration> laneDeclarations;
    private PhaseTiming phaseTiming;

    private PhaseFactory phaseFactory;
    private PhaseSelectionPolicy policy;

    private double hysteresis = 0.10;
    private double starvationWeight = 0.25;

    public SimulationEngineBuilder laneDeclarations(List<LaneDeclaration> decls) {
        this.laneDeclarations = List.copyOf(decls);
        return this;
    }

    public SimulationEngineBuilder phaseTiming(PhaseTiming timing) {
        this.phaseTiming = timing;
        return this;
    }

    public SimulationEngineBuilder phaseFactory(PhaseFactory pf) {
        this.phaseFactory = pf;
        return this;
    }

    public SimulationEngineBuilder policy(PhaseSelectionPolicy policy) {
        this.policy = policy;
        return this;
    }

    public SimulationEngineBuilder switchHysteresis(double h) {
        this.hysteresis = h;
        return this;
    }

    public SimulationEngineBuilder starvationWeight(double w) {
        this.starvationWeight = w;
        return this;
    }

    public SimulationEngine build() {
        PhaseFactory effectiveFactory = resolvePhaseFactory();
        List<Phase> phases = effectiveFactory.create();

        if (phases.isEmpty()) {
            throw new IllegalArgumentException("PhaseFactory produced an empty phase list");
        }

        Map<Road, Integer> lanes = lanesFromDeclarations();

        Map<Road, Double> weights = new EnumMap<>(Road.class);
        for (Road r : Road.values()) {
            weights.put(r, 1.0);
        }

        PhaseSelectionPolicy effectivePolicy =
                (policy != null) ? policy : new MaxPressurePolicy(starvationWeight, weights);

        SignalController controller = new SignalController(phases, effectivePolicy, hysteresis);

        return new SimulationEngine(
                new TrafficQueues(lanes),
                new ActivePhase(phases.get(0)),
                controller
        );
    }

    private PhaseFactory resolvePhaseFactory() {
        if (phaseFactory != null) {
            return phaseFactory;
        }
        if (laneDeclarations != null && phaseTiming != null) {
            return new PhaseSetGenerator(laneDeclarations, phaseTiming);
        }
        // Default: 1 GENERAL lane per road, standard NS+EW phases.
        return PhaseSetGenerator.defaults();
    }

    private Map<Road, Integer> lanesFromDeclarations() {
        Map<Road, Integer> result = new EnumMap<>(Road.class);
        for (Road r : Road.values()) {
            result.put(r, 1);
        }

        if (laneDeclarations != null) {
            for (LaneDeclaration d : laneDeclarations) {
                result.merge(d.road(), d.index() + 1, Math::max);
            }
        }

        return result;
    }
}