package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;

import java.util.List;
import java.util.Map;

// queueLengthsPerLane   - number of vehicles queued per road, per lane
// stepsSinceGreenPerLane - steps elapsed since each lane last had a green signal
public record CrossroadState(
        Map<Road, List<Integer>> queueLengthsPerLane,
        Map<Road, List<Integer>> stepsSinceGreenPerLane
) {
    // Number of vehicles waiting in a specific lane.
    public int queueLength(Road r, int laneIdx) {
        List<Integer> lanes = queueLengthsPerLane.getOrDefault(r, List.of());
        return laneIdx < lanes.size() ? lanes.get(laneIdx) : 0;
    }

    // Returns the wait counter for a specific lane of the given road.
    public int stepsSinceGreen(Road r, int laneIdx) {
        List<Integer> lanes = stepsSinceGreenPerLane.getOrDefault(r, List.of(0));
        return laneIdx < lanes.size() ? lanes.get(laneIdx) : 0;
    }
}
