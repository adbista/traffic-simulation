package org.example.trafficsim.signal;
import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.MovementMask;
import org.example.trafficsim.model.PhaseTiming;
import org.example.trafficsim.signal.algorithm.DsaturColoring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.example.trafficsim.config.SimDefaults.DEFAULT_TIMING;

/**
 * Builds traffic phases from an {@link IntersectionConfig} using DSATUR graph
 * coloring on the conflict graph of {@link LaneSignal}s.
 *
 * <p>Each color produced by the coloring algorithm corresponds to one phase;
 * signals assigned the same color do not conflict and can be active together.
 */
public class PhaseFactory  {

    public PhaseFactory() {}

    public List<Phase> create(IntersectionConfig config) {
        
        LaneRegistry registry = config.laneRegistry();
        PhaseTiming timing = config.phaseTiming() != null
                ? config.phaseTiming()
                : DEFAULT_TIMING;

        List<LaneSignal> signals = SignalFactory.createSignals(config);

        int[] colors = new DsaturColoring().color(signals, ConflictMovements::signalsConflict);

        // Group signals by color (= phase)
        Map<Integer, List<LaneSignal>> byColor = new HashMap<>();
        for (int i = 0; i < signals.size(); i++) {
            byColor.computeIfAbsent(colors[i], k -> new ArrayList<>()).add(signals.get(i));
        }

        List<Phase> phases = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<Integer, List<LaneSignal>> entry : byColor.entrySet()) {
            List<LaneSignal> group = entry.getValue();
            if (group.isEmpty()) continue;
            phases.add(toPhase("p" + idx, group, registry, timing));
            idx++;
        }

        if (phases.isEmpty()) {
            throw new IllegalStateException(
                    "PhaseFactory did not produce any phases - check config"
            );
        }

        return phases;
    }

    private Phase toPhase(
            String id,
            List<LaneSignal> group,
            LaneRegistry registry,
            PhaseTiming timing
    ) {
        int total = registry.totalPositions();
        int[] allowedMovementMask = new int[total];
        long positionsMask = 0;

        for (LaneSignal s : group) {
            int posId = registry.positionId(s.road(), s.laneIndex());
            allowedMovementMask[posId] |= s.movementMask();
            positionsMask |= (1L << posId);
        }

        return new Phase(id, timing, positionsMask, allowedMovementMask, group);
    }
}