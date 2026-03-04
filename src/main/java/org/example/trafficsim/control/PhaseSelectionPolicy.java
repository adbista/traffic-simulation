package org.example.trafficsim.control;

import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.metrics.FlowProvider;
import org.example.trafficsim.metrics.GreenProvider;
import org.example.trafficsim.signal.Phase;

import java.util.*;


public final class PhaseSelectionPolicy {

    // Weight for incoming traffic rate. Higher -> prefer phases with high inflow.
    private final double flowW;

    // Weight for queue length. Higher -> clear long queues faster.
    private final double queueW;
    // Weight for waiting too long. Higher -> give green to neglected lanes sooner.
    private final double ageW;
    // Age limited so very old lanes don't dominate forever.
    private final long ageCapSteps;
    // Provides flow/intensity per lane
    private final FlowProvider flow;
    // Provides last time a lane had green
    private final GreenProvider green;
    private final Map<Integer, Double> weightsByPositionId;
    private final List<Phase> phases;

    public PhaseSelectionPolicy(double flowW, double queueW, double ageW, long ageCapSteps,
                                FlowProvider flow, GreenProvider green,
                                Map<Integer, Double> weights, List<Phase> phases) {
        this.flowW = flowW;
        this.queueW = queueW;
        this.ageW = ageW;
        this.ageCapSteps = ageCapSteps;
        this.flow = flow;
        this.green = green;
        this.weightsByPositionId = weights;
        this.phases = phases;
    }

    public PolicySelection select(TrafficQueues queues, long stepNo, Phase currentPhase) {

        ScoreByPhase best = null;
        ScoreByPhase current = null;

        for (Phase phase : phases) {
            double s = score(queues, stepNo, phase);
            ScoreByPhase result = new ScoreByPhase(phase, s);

            if (phase == currentPhase) {
                current = result;
            }
            if (best == null || s > best.score()) {
                best = result;
            }
        }

        return new PolicySelection(current, best);
    }

    public double score(TrafficQueues queues, long stepNo, Phase phase) {
        double score = 0.0;
        long tmp = phase.positionsMask();

        while (tmp != 0) {
            int posId = Long.numberOfTrailingZeros(tmp);
            tmp &= (tmp - 1);

            int q = queues.queueLength(posId);
            double flowVal = flow.intensity(posId);

            if (q == 0 && flowVal < 1e-9) continue;

            long rawAge = Math.max(0L, stepNo - green.lastGreenStep(posId));
            long cappedAge = Math.min(rawAge, ageCapSteps);

            double laneW = weightsByPositionId.getOrDefault(posId, 1.0); // lane priority factor
            score += laneW * (flowW * flowVal + queueW * q + ageW * cappedAge);
        }

        return score;
    }
}
