package org.example.trafficsim.control;

import org.example.trafficsim.core.CrossroadState;
import org.example.trafficsim.signal.ActivePhase;
import org.example.trafficsim.signal.Phase;

import java.util.List;
import java.util.Set;

// Decides when and to which phase to switch the signal.
// Called once per simulation step, after vehicles have already departed.
public class SignalController {
    private final List<Phase> phases;
    private final PhaseSelectionPolicy policy;
    // prevents rapid phase switching when scores are very close
    private final double switchHysteresis;

    public SignalController(List<Phase> phases, PhaseSelectionPolicy policy, double switchHysteresis) {
        this.phases = phases;
        this.policy = policy;
        this.switchHysteresis = switchHysteresis;
    }

    // Checks whether the current phase should end and a new one begin.
    // Only acts while GREEN — no decisions during yellow or all-red.
    public void maybeRequestSwitch(CrossroadState snap, ActivePhase active) {
        // only make decisions while GREEN
        if (!active.isGreen()) return;

        Phase cur = active.current();
        int tGreen = active.timeInState();
        int minG = cur.timing().minGreenSteps();
        int maxG = cur.timing().maxGreenSteps();

        // 1) too early to switch
        if (tGreen < minG) return;

        // 2) max green time reached — force a switch
        if (tGreen >= maxG) {
            advanceToNextPhase(active);
            return;
        }

        // 3) gap-out: all green lanes are empty, no point holding the phase
        boolean anyQueueOnGreen = cur.greenRoads().stream().anyMatch(r -> {
            Set<Integer> lanes = cur.greenLanesFor(r);
            Set<Integer> toCheck = lanes.isEmpty() ? Set.of(0) : lanes;
            return toCheck.stream().anyMatch(lane -> snap.queueLength(r, lane) > 0);
        });
        if (!anyQueueOnGreen) {
            advanceToNextPhase(active);
            return;
        }

        // 4) switch if another phase scores notably better
        var currentScore = scoreOf(snap, cur);
        var best = policy.select(snap, phases, cur.id());
        if (!best.phaseId().equals(cur.id()) && best.score() > currentScore * (1.0 + switchHysteresis)) {
            active.requestSwitchTo(best.phaseId());
        }
    }

    // Advances to the next phase in round-robin order.
    private void advanceToNextPhase(ActivePhase active) {
        int curIdx = phases.indexOf(active.current());
        active.requestSwitchTo(phases.get((curIdx + 1) % phases.size()).id());
    }

    // Returns the score for the given phase (used for hysteresis comparison)
    private double scoreOf(CrossroadState snap, Phase phase) {
        var sel = policy.select(snap, List.of(phase), phase.id());
        return sel.score();
    }

    // Looks up a Phase by id — called by ActivePhase when switching
    public Phase resolve(String id) {
        return phases.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown phase id: " + id));
    }
}
