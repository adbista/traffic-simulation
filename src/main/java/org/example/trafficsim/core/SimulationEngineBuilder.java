package org.example.trafficsim.core;

import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.control.PhaseController;
import org.example.trafficsim.control.PhaseSelectionPolicy;
import org.example.trafficsim.control.PhaseSelectionPolicyBuilder;
import org.example.trafficsim.metrics.FlowEmaTracker;
import org.example.trafficsim.metrics.GreenStepTracker;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;
import org.example.trafficsim.signal.PhaseFactory;

import java.util.List;

/** Builds a fully-wired {@link SimulationEngine} from an {@link IntersectionConfig}. */
public class SimulationEngineBuilder {

    private final IntersectionConfig config;

    private PhaseFactory phaseFactory;
    private PhaseSelectionPolicy policy;

    private double hysteresis = 0.10;

    public SimulationEngineBuilder(IntersectionConfig config){
        this.config = config;
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

    public SimulationEngine build() {

        List<Phase> phases = phaseFactory != null ? phaseFactory.create(config) : new PhaseFactory().create(config);

        if (phases.isEmpty()) {
            throw new IllegalArgumentException("PhaseFactory produced an empty phase list");
        }


        int totalPositions = config.laneRegistry().totalPositions();
        FlowEmaTracker flowTracker = new FlowEmaTracker(totalPositions, 0.10);
        GreenStepTracker greenTracker = new GreenStepTracker(totalPositions);

        PhaseSelectionPolicy effectivePolicy =
                (policy != null) ? policy : new PhaseSelectionPolicyBuilder()
                        .phases(phases)
                        .laneWeights(config.laneWeights())
                        .flowProvider(flowTracker)
                        .greenProvider(greenTracker)
                        .build();

        return new SimulationEngine(
                new TrafficQueues(config.laneRegistry()),
                new ActivePhase(phases.get(0)),
                new PhaseController(phases, effectivePolicy, hysteresis),
                flowTracker,
                greenTracker
        );
    }


}
