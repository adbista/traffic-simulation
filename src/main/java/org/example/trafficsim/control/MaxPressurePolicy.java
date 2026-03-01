package org.example.trafficsim.control;

import org.example.trafficsim.core.CrossroadState;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.signal.Phase;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Max-Pressure algorithm: selects the phase whose lanes have the most waiting traffic.
public class MaxPressurePolicy implements PhaseSelectionPolicy {
    // anti-starvation coefficient: prevents a road from waiting indefinitely
    private final double alpha;
    private final Map<Road, Double> weights;

    public MaxPressurePolicy(double alpha, Map<Road, Double> weights) {
        this.alpha = alpha;
        this.weights = new EnumMap<>(weights);
    }

    @Override
    public Selection select(CrossroadState snapshot, List<Phase> phases, String currentPhaseId) {
        String bestId = currentPhaseId;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Phase p : phases) {
            double score = 0.0;
            for (Road r : p.greenRoads()) {
                double w = weights.getOrDefault(r, 1.0);
                Set<Integer> greenLanes = p.greenLanesFor(r);
                Set<Integer> toScore = greenLanes.isEmpty() ? Set.of(0) : greenLanes;
                for (int laneIdx : toScore) {
                    score += w * (snapshot.queueLength(r, laneIdx) + alpha * snapshot.stepsSinceGreen(r, laneIdx));
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestId = p.id();
            }
        }
        return new Selection(bestId, bestScore);
    }
}