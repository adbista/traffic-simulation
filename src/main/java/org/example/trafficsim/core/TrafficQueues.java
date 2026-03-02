package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;

import java.util.*;

// Vehicles are held in queues per road, per lane. Defaults to 1 lane per road.
public class TrafficQueues {

    private final Map<Road, List<Queue<Vehicle>>> laneQueues = new EnumMap<>(Road.class);
    private final Map<Road, Integer> lanesPerRoad;

    // For each lane, stores the simulation step when that lane last had green.
    private final Map<Road, List<Long>> lastGreenStep = new EnumMap<>(Road.class);

    public TrafficQueues(Map<Road, Integer> lanesPerRoad) {
        this.lanesPerRoad = new EnumMap<>(Road.class);
        this.lanesPerRoad.putAll(lanesPerRoad);

        for (Road r : Road.values()) {
            int lanes = this.lanesPerRoad.getOrDefault(r, 1);

            List<Queue<Vehicle>> roadLanes = new ArrayList<>(lanes);
            for (int i = 0; i < lanes; i++) {
                roadLanes.add(new ArrayDeque<>());
            }
            laneQueues.put(r, roadLanes);

            lastGreenStep.put(r, new ArrayList<>(Collections.nCopies(lanes, 0L)));
        }
    }
    public TrafficQueues() {
        this(new EnumMap<>(Road.class));
    }
    public void addVehicle(Vehicle v) {
        List<Queue<Vehicle>> lanes = laneQueues.get(v.startRoad());
        int laneIdx = v.lane();
        if (laneIdx < 0 || laneIdx >= lanes.size()) {
            throw new IllegalArgumentException(
                "Vehicle lane " + laneIdx + " out of bounds for road " + v.startRoad() +
                " (has " + lanes.size() + " lanes)"
            );
        }
        lanes.get(laneIdx).add(v);
    }

    public Vehicle pollSpecificLane(Road r, int laneIdx) {
        List<Queue<Vehicle>> lanes = laneQueues.get(r);
        if (laneIdx >= lanes.size()) return null;
        return lanes.get(laneIdx).poll();
    }

    // Marks that the given lane had green at currentStep.
    public void markGreenNow(Road r, int laneIdx, long currentStep) {
        List<Long> steps = lastGreenStep.get(r);
        if (steps == null || laneIdx < 0 || laneIdx >= steps.size()) {
            return;
        }
        steps.set(laneIdx, currentStep);
    }

    public int queueLength(Road r, int laneIdx) {
        List<Queue<Vehicle>> lanes = laneQueues.get(r);
        if (lanes == null || laneIdx < 0 || laneIdx >= lanes.size()) {
            return 0;
        }
        return lanes.get(laneIdx).size();
    }

    // Returns the number of steps since the given lane last had green.
    // how many steps have passed since the last opportunity to travel
    public int stepsSinceGreen(Road r, int laneIdx, long currentStep) {
        List<Long> steps = lastGreenStep.get(r);
        if (steps == null || laneIdx < 0 || laneIdx >= steps.size()) {
            return 0;
        }

        long diff = currentStep - steps.get(laneIdx);
        return diff > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) diff;
    }

}
