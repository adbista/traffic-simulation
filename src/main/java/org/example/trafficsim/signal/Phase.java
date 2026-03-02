package org.example.trafficsim.signal;

import org.example.trafficsim.model.LaneSignal;
import org.example.trafficsim.model.Road;
import java.util.Map;
import java.util.Set;

public record Phase(
        String id,
        Set<Road> greenRoads,
        PhaseTiming timing,
        Map<Road, Set<Integer>> greenLaneIndices,
        Set<LaneSignal> greenSignals
) {

    // Returns which lanes are green for road r (lane mode).
    public Set<Integer> greenLanesFor(Road r) {
        return greenLaneIndices.getOrDefault(r, Set.of());
    }
    public boolean activatesRoad(Road r) {
        return greenRoads.contains(r);
    }
}
