package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;

import java.util.ArrayDeque;
import java.util.Queue;


public class TrafficQueues {

    private final LaneRegistry laneRegistry;

    private final Queue<Vehicle>[] queues;
    private long nonEmptyPositionsMask;

    public TrafficQueues(LaneRegistry laneRegistry) {
        this.laneRegistry = laneRegistry;
        int total = laneRegistry.totalPositions();
        this.queues = new Queue[total];
        for (int i = 0; i < total; i++) {
            queues[i] = new ArrayDeque<>();
        }
        this.nonEmptyPositionsMask = 0;
    }

    /** Adds the vehicle to the appropriate queue and returns its positionId. */
    public int addVehicle(Vehicle v) {
        int posId = laneRegistry.positionId(v.startRoad(), v.lane());
        queues[posId].add(v);
        nonEmptyPositionsMask |= (1L << posId);
        return posId;
    }

    /** Removes and returns the head vehicle at positionId, updating the bitmask. */
    public Vehicle poll(int positionId) {
        Vehicle v = queues[positionId].poll();
        if (queues[positionId].isEmpty()) {
            nonEmptyPositionsMask &= ~(1L << positionId);
        }
        return v;
    }

    /** Returns the head vehicle at positionId without removing it, or null. */
    public Vehicle peek(int positionId) {
        return queues[positionId].peek();
    }

    public int queueLength(int positionId) {
        return queues[positionId].size();
    }

    public long nonEmptyPositionsMask() {
        return nonEmptyPositionsMask;
    }

    // Returns true if there is at least one vehicle in any of the positions indicated by the mask.
    public boolean hasAnyVehicleInMask(long mask) {
        return (mask & nonEmptyPositionsMask) != 0;
    }

    public Road roadOf(int positionId) {
        return laneRegistry.roadOf(positionId);
    }

    public int[] positionIdsOfRoad(Road road) {
        return laneRegistry.lanesOf(road);
    }

}
