package org.example.trafficsim.control;

import org.example.trafficsim.metrics.FlowProvider;
import org.example.trafficsim.metrics.GreenProvider;
import org.example.trafficsim.signal.Phase;

import java.util.List;
import java.util.Map;

/** Builder for {@link PhaseSelectionPolicy} with sensible defaults. */
public final class PhaseSelectionPolicyBuilder {

    private double flowW = 10.0;
    private double queueW = 1.0;
    private double ageW = 0.01;
    private long ageCapSteps = 200L;
    private FlowProvider flowProvider;
    private GreenProvider greenProvider;
    private Map<Integer, Double> laneWeights;
    private List<Phase> phases;

    public PhaseSelectionPolicyBuilder flowWeight(double w) {
        this.flowW = w;
        return this;
    }

    public PhaseSelectionPolicyBuilder queueWeight(double w) {
        this.queueW = w;
        return this;
    }

    public PhaseSelectionPolicyBuilder ageWeight(double w) {
        this.ageW = w;
        return this;
    }

    public PhaseSelectionPolicyBuilder ageCapSteps(long cap) {
        this.ageCapSteps = cap;
        return this;
    }

    public PhaseSelectionPolicyBuilder flowProvider(FlowProvider fp) {
        this.flowProvider = fp;
        return this;
    }

    public PhaseSelectionPolicyBuilder greenProvider(GreenProvider gp) {
        this.greenProvider = gp;
        return this;
    }

    public PhaseSelectionPolicyBuilder laneWeights(Map<Integer, Double> weights) {
        this.laneWeights = weights;
        return this;
    }

    public PhaseSelectionPolicyBuilder phases(List<Phase> phases) {
        this.phases = phases;
        return this;
    }

    public PhaseSelectionPolicy build() {
        if (phases == null || phases.isEmpty()) {
            throw new IllegalStateException("phases must be set and non-empty");
        }
        if (laneWeights == null) {
            throw new IllegalStateException("laneWeights must be set");
        }
        if (flowProvider == null) {
            throw new IllegalStateException("flowProvider must be set");
        }
        if (greenProvider == null) {
            throw new IllegalStateException("greenProvider must be set");
        }
        return new PhaseSelectionPolicy(flowW, queueW, ageW, ageCapSteps,
                flowProvider, greenProvider, laneWeights, phases);
    }
}

