package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TrafficQueuesTest {

    private TrafficQueues queues;

    // Helper to create a vehicle quickly (lane 0)
    private static Vehicle v(String id, Road road) {
        return new Vehicle(id, road, Road.SOUTH, 0, 0, 0);
    }

    private static Vehicle vInLane(String id, Road road, int lane) {
        return new Vehicle(id, road, Road.SOUTH, 0, 0, lane);
    }

    @BeforeEach
    void setUp() {
        queues = new TrafficQueues();
    }

    @Test
    void vehicleWithLaneOutOfBoundsThrowsException() {
        Map<Road, Integer> lanes = new EnumMap<>(Road.class);
        for (Road r : Road.values()) lanes.put(r, 1);
        lanes.put(Road.NORTH, 2); // NORTH has 2 lanes (0 and 1)

        TrafficQueues mq = new TrafficQueues(lanes);
        
        // lane=99 is out of bounds (valid: 0-1)
        assertThrows(IllegalArgumentException.class, 
            () -> mq.addVehicle(vInLane("v", Road.NORTH, 99)),
            "Should throw when lane index >= number of lanes");
        
        // lane=-1 is also invalid
        assertThrows(IllegalArgumentException.class,
            () -> mq.addVehicle(vInLane("v2", Road.NORTH, -1)),
            "Should throw when lane index < 0");
    }

}
