package org.example.trafficsim.control;

import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PhaseController {
    private final List<Phase> phases;
    private final PhaseSelectionPolicy policy;
    private final double switchHysteresis;
    private final Map<Phase, Integer> phasesToIndices;

    public PhaseController(List<Phase> phases, PhaseSelectionPolicy policy, double switchHysteresis) {
        this.phases = phases;
        this.policy = policy;
        this.switchHysteresis = switchHysteresis;

        AtomicInteger index = new AtomicInteger();
        this.phasesToIndices = new HashMap<>();
        this.phases.forEach(p -> this.phasesToIndices.put(p, index.getAndIncrement()));
    }

    public List<Phase> getPhases() { return phases; }

    public void managePhaseSwitch(TrafficQueues queues, long stepNo, ActivePhase active) {
        if (!active.isGreen()) {
            return;
        }

        Phase cur = active.current();
        int tGreen = active.timeInState();
        int minG = cur.timing().minGreenSteps();
        int maxG = cur.timing().maxGreenSteps();
        if (tGreen < minG) {
            return;
        }

        if (tGreen >= maxG) {
            roundRobinNextPhase(stepNo, active, "max-green");
            return;
        }

        if (!queues.hasAnyVehicleInMask(cur.positionsMask())) {
            boolean switched = trySwitchToBestPhase(queues, stepNo, active, cur, false, "gap-out");
            if (!switched) {
                roundRobinNextPhase(stepNo, active, "gap-out fallback");
            }
            return;
        }

        boolean switched = trySwitchToBestPhase(queues, stepNo, active, cur, true, "best+hysteresis");
        if (!switched) {
            roundRobinNextPhase(stepNo, active, "no-better");
        }
    }

    private void roundRobinNextPhase(long stepNo, ActivePhase active, String reason) {
        Integer curIdx = phasesToIndices.get(active.current());
        if (curIdx == null) {
            throw new IllegalStateException("Current phase not registered: " + active.current().id());
        }
        Phase next = phases.get((curIdx + 1) % phases.size());

        active.requestNextPhase(next);
    }

    private boolean trySwitchToBestPhase(TrafficQueues queues, long stepNo,
                                         ActivePhase active, Phase cur,
                                         boolean useHysteresis,
                                         String reason) {
        var selection = policy.select(queues, stepNo, cur);
        var current = selection.activePhaseScore();
        var best = selection.bestPhaseScore();

        if (best.phase() == cur) {
            return false;
        }

        double threshold = useHysteresis
                ? current.score() * (1.0 + switchHysteresis)
                : current.score();

        if (best.score() > threshold) {
            active.requestNextPhase(best.phase());
            return true;
        }

        return false;
    }

}