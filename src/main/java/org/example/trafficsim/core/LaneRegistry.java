package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;

import java.util.List;

/** Maps road+lane indices to integer position IDs used throughout the simulation. */
public final class LaneRegistry {

    private final int[][] positionIdByRoadAndLane;
    private final List<Road> roadByPositionId;
    private final List<Integer> laneByPositionId;

    public LaneRegistry(
            int[][] positionIdByRoadAndLane,
            List<Road> roadByPositionId,
            List<Integer> laneByPositionId
    ) {
        if (roadByPositionId.size() != laneByPositionId.size()) {
            throw new IllegalArgumentException("roadByPositionId and laneByPositionId must have same size");
        }

        this.positionIdByRoadAndLane = positionIdByRoadAndLane;
        this.roadByPositionId = roadByPositionId;
        this.laneByPositionId = laneByPositionId;
    }

    public int totalPositions() {
        return roadByPositionId.size();
    }

    public int positionId(Road road, int lane) {
        int[] lanes = positionIdByRoadAndLane[road.ordinal()];
        if (lane < 0 || lane >= lanes.length) {
            throw new IllegalArgumentException("Lane " + lane + " does not exist on road " + road);
        }
        return lanes[lane];
    }

    public Road roadOf(int positionId) {
        validatePositionId(positionId);
        return roadByPositionId.get(positionId);
    }

    public int laneOf(int positionId) {
        validatePositionId(positionId);
        return laneByPositionId.get(positionId);
    }

    private void validatePositionId(int positionId) {
        if (positionId < 0 || positionId >= roadByPositionId.size()) {
            throw new IllegalArgumentException("Invalid positionId: " + positionId);
        }
    }

    public int[] lanesOf(Road road) {
        return positionIdByRoadAndLane[road.ordinal()];
    }
}