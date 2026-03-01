package org.example.trafficsim.signal;

import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.Road;
import java.util.Map;
import java.util.Set;

// Immutable description of a single signal phase.
//
// Two modes:
//
// 1. Signal mode (greenSignals non-empty) — created by PhaseSetGenerator.
//    Phase described at the level of atomic signals (road, lane, movement).
//    SimulationEngine checks whether a vehicle's movement matches the active signal.
//    greenRoads and greenLaneIndices are derived — computed from greenSignals.
//
// 2. Lane mode (greenSignals empty, greenLaneIndices used) — legacy/test mode.
//    Any vehicle on a green lane is allowed without movement filtering.
//    Used by tests that create Phase manually (e.g. TurnPhaseTest, SimulationEngineTest).
//
// greenLanesFor(r):
//   empty Set  = all lanes of road r have green ("whole road" mode)
//   non-empty  = only the specified lane indices have green
public record Phase(
        String id,
        Set<Road> greenRoads,
        PhaseTiming timing,
        Map<Road, Set<Integer>> greenLaneIndices,
        Set<LaneSignal> greenSignals
) {
    // Legacy constructor: no signals, whole-road or lane-map mode
    public Phase(String id, Set<Road> greenRoads, PhaseTiming timing,
                 Map<Road, Set<Integer>> greenLaneIndices) {
        this(id, greenRoads, timing, greenLaneIndices, Set.of());
    }

    // Legacy constructor: no signals, no greenLaneIndices (all lanes green)
    public Phase(String id, Set<Road> greenRoads, PhaseTiming timing) {
        this(id, greenRoads, timing, Map.of(), Set.of());
    }

    // Returns which lanes are green for road r (lane mode).
    // Empty set = all lanes of that road are green.
    public Set<Integer> greenLanesFor(Road r) {
        return greenLaneIndices.getOrDefault(r, Set.of());
    }
}
