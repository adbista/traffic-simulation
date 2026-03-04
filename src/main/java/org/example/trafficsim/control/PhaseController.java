package org.example.trafficsim.control;

import org.example.trafficsim.core.TrafficQueues;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decides when and to which phase to switch the signal.
 * Called once per simulation step, after vehicles have already departed.
 */
public class PhaseController {

    private final List<Phase> phases;
    private final PhaseSelectionPolicy policy;
    /** Prevents rapid phase switching when scores are very close. */
    private final double switchHysteresis;
    private Map<Phase, Integer> phasesToIndices;

    public PhaseController(List<Phase> phases, PhaseSelectionPolicy policy,
                           double switchHysteresis) {
        this.phases = phases; // list for round robin fallback
        this.policy = policy;
        this.switchHysteresis = switchHysteresis;

        // we need also a map for index lookup by Phase
        AtomicInteger index = new AtomicInteger();
        this.phasesToIndices = new HashMap<>();
        this.phases.stream().forEach(p -> this.phasesToIndices.put(p, index.getAndIncrement()));

    }

    /**
     * Checks whether the current phase should end and a new one begin.
     * Only acts while GREEN - no decisions during yellow or all-red.
     */
    public void managePhaseSwitch(TrafficQueues queues, long stepNo, ActivePhase active) {
        if (!active.isGreen()) return;

        Phase cur  = active.current();
        int tGreen = active.timeInState();
        int minG   = cur.timing().minGreenSteps();
        int maxG   = cur.timing().maxGreenSteps();

        // 1) too early to switch
        if (tGreen < minG) return;

        // 2) max green time reached - force a switch
        if (tGreen >= maxG) {
            roundRobinNextPhase(active);
            return;
        }

        // 3) gap-out: no vehicles waiting on any position of the current phase.
        // Jump directly to the phase with the highest waiting-vehicle score so
        // that empty intermediate phases are not wastefully visited one-by-one.
        if (!queues.hasAnyVehicleInMask(cur.positionsMask())) {
            if (!trySwitchToBestPhase(queues, stepNo, active, cur, false)) {
                roundRobinNextPhase(active);
            }
            return;
        }

        // 4) switch if another phase scores notably better (with hysteresis guard)
        trySwitchToBestPhase(queues, stepNo, active, cur, true);
    }

    /** Advances to the next phase in round-robin order. */
    private void roundRobinNextPhase(ActivePhase active) {
        Integer curIdx = phasesToIndices.get(active.current());
        if (curIdx == null) {
            throw new IllegalStateException(
                    "Current phase not registered: " + active.current().id());
        }
        active.requestNextPhase(phases.get((curIdx + 1) % phases.size()));
    }

    private boolean trySwitchToBestPhase(TrafficQueues queues, long stepNo,
                                         ActivePhase active, Phase cur,
                                         boolean useHysteresis) {
        var selection = policy.select(queues, stepNo, cur);
        var current = selection.activePhaseScore();
        var best = selection.bestPhaseScore();

        if (best.phase() == cur) return false;

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
