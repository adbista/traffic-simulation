package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;

import java.util.*;

// Stores vehicle queues at the intersection.
// Vehicles are held in queues per road, per lane. Defaults to 1 lane per road.
// Each simulation step allows one vehicle per lane to depart.
public class TrafficQueues {

    private final Map<Road, List<Queue<Vehicle>>> laneQueues = new EnumMap<>(Road.class);
    private final Map<Road, Integer> lanesPerRoad;
    // stepsSinceGreen holds the wait counter per lane (index = lane number)
    private final Map<Road, List<Integer>> stepsSinceGreen = new EnumMap<>(Road.class);

    public TrafficQueues(Map<Road, Integer> lanesPerRoad) {
        this.lanesPerRoad = new EnumMap<>(lanesPerRoad);
        for (Road r : Road.values()) {
            int lanes = this.lanesPerRoad.getOrDefault(r, 1);
            List<Queue<Vehicle>> roadLanes = new ArrayList<>(lanes);
            for (int i = 0; i < lanes; i++) roadLanes.add(new ArrayDeque<>());
            laneQueues.put(r, roadLanes);
            stepsSinceGreen.put(r, new ArrayList<>(Collections.nCopies(lanes, 0)));
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

    // Removes and returns the first vehicle from every lane of the given road (FIFO)
    public List<Vehicle> pollLane(Road r) {
        List<Queue<Vehicle>> lanes = laneQueues.get(r);
        List<Vehicle> result = new ArrayList<>(lanes.size());
        for (Queue<Vehicle> lane : lanes) {
            Vehicle v = lane.poll();
            if (v != null) result.add(v);
        }
        return result;
    }

    // Removes and returns a vehicle from the specific lane. Returns null if the lane is empty.
    // Use this when the phase activates only selected lanes for a road (turn-based mode).
    public Vehicle pollSpecificLane(Road r, int laneIdx) {
        List<Queue<Vehicle>> lanes = laneQueues.get(r);
        if (laneIdx >= lanes.size()) return null; // phase references a lane that does not exist
        return lanes.get(laneIdx).poll();
    }

    // Peeks the first vehicle from the specific lane without removing it.
    // Returns null if the lane is empty.
    public Vehicle peekSpecificLane(Road r, int laneIdx) {
        List<Queue<Vehicle>> lanes = laneQueues.get(r);
        if (laneIdx >= lanes.size()) return null;
        return lanes.get(laneIdx).peek();
    }

    // Returns the total number of vehicles waiting across all lanes of the given road
    public int queueLen(Road r) {
        return laneQueues.get(r).stream().mapToInt(Collection::size).sum();
    }

    // Returns the maximum wait counter across all lanes of the given road.
    // Used by MaxPressurePolicy to assess lane urgency.
    public int stepsSinceGreen(Road r) {
        return stepsSinceGreen.get(r).stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    // Returns the wait counter for the specific lane of the given road.
    public int stepsSinceGreen(Road r, int laneIdx) {
        List<Integer> counters = stepsSinceGreen.get(r);
        if (laneIdx < 0 || laneIdx >= counters.size()) return 0;
        return counters.get(laneIdx);
    }

    // Updates wait counters per lane.
    // greenLaneIndices: road -> set of green lanes this step.
    // Green lanes are reset to 0; all others increment by 1.
    public void updateWaitCounters(Map<Road, Set<Integer>> greenLaneIndices) {
        for (Road r : Road.values()) {
            Set<Integer> greenLanes = greenLaneIndices.getOrDefault(r, Set.of());
            List<Integer> counters = stepsSinceGreen.get(r);
            for (int i = 0; i < counters.size(); i++) {
                if (greenLanes.contains(i)) counters.set(i, 0);
                else counters.set(i, counters.get(i) + 1);
            }
        } // used by the signal control algorithm to know which roads have been waiting the longest
    }

    public CrossroadState snapshot() {
        Map<Road, List<Integer>> queueSnap = new EnumMap<>(Road.class);
        for (Map.Entry<Road, List<Queue<Vehicle>>> e : laneQueues.entrySet()) {
            List<Integer> lengths = new ArrayList<>(e.getValue().size());
            for (Queue<Vehicle> lane : e.getValue()) lengths.add(lane.size());
            queueSnap.put(e.getKey(), Collections.unmodifiableList(lengths));
        }
        Map<Road, List<Integer>> stepsSnapshot = new EnumMap<>(Road.class);
        stepsSinceGreen.forEach((r, counters) -> stepsSnapshot.put(r, List.copyOf(counters)));
        return new CrossroadState(
                Collections.unmodifiableMap(queueSnap),
                Collections.unmodifiableMap(stepsSnapshot)
        );
    }
}
