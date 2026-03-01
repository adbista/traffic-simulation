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
        queues = new TrafficQueues(); // single lane per road
    }

    // podstawowa kolejka pojazdow

    @Test
    void addAndPollVehicle() {
        queues.addVehicle(v("v1", Road.NORTH));
        List<Vehicle> polled = queues.pollLane(Road.NORTH);
        assertEquals(1, polled.size());
        assertEquals("v1", polled.get(0).id());
    }

    @Test
    void pollFromEmptyLaneReturnsEmpty() {
        List<Vehicle> polled = queues.pollLane(Road.NORTH);
        assertTrue(polled.isEmpty());
    }

    @Test
    void queueLenCountsAllVehicles() {
        queues.addVehicle(v("v1", Road.NORTH));
        queues.addVehicle(v("v2", Road.NORTH));
        assertEquals(2, queues.queueLen(Road.NORTH));
    }

    @Test
    void pollReducesQueueLen() {
        queues.addVehicle(v("v1", Road.NORTH));
        queues.addVehicle(v("v2", Road.NORTH));
        queues.pollLane(Road.NORTH);
        assertEquals(1, queues.queueLen(Road.NORTH));
    }

    // kolejnosc wg kolejki FIFO

    @Test
    void vehiclesDepartFifoWithinLane() {
        queues.addVehicle(v("first",  Road.NORTH));
        queues.addVehicle(v("second", Road.NORTH));

        List<Vehicle> polled = queues.pollLane(Road.NORTH);
        assertEquals("first", polled.get(0).id()); // FIFO: first added, first polled
    }

    // wiele pasow

    @Test
    void multiLanePollsOneVehiclePerLane() {
        Map<Road, Integer> lanes = new EnumMap<>(Road.class);
        for (Road r : Road.values()) lanes.put(r, 1);
        lanes.put(Road.NORTH, 2); // 2 lanes on north

        TrafficQueues mq = new TrafficQueues(lanes);
        mq.addVehicle(vInLane("lane0", Road.NORTH, 0));
        mq.addVehicle(vInLane("lane1", Road.NORTH, 1));

        List<Vehicle> polled = mq.pollLane(Road.NORTH);
        assertEquals(2, polled.size());
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

    // liczniki oczekiwania

    @Test
    void stepsSinceGreenStartsAtZero() {
        assertEquals(0, queues.stepsSinceGreen(Road.NORTH));
    }

    @Test
    void greenRoadsGetResetOthersIncrement() {
        queues.updateWaitCounters(Map.of(Road.NORTH, Set.of(0), Road.SOUTH, Set.of(0)));
        assertEquals(0, queues.stepsSinceGreen(Road.NORTH));
        assertEquals(0, queues.stepsSinceGreen(Road.SOUTH));
        assertEquals(1, queues.stepsSinceGreen(Road.EAST));
        assertEquals(1, queues.stepsSinceGreen(Road.WEST));
    }

    @Test
    void waitCounterAccumulatesOverMultipleSteps() {
        queues.updateWaitCounters(Map.of(Road.NORTH, Set.of(0), Road.SOUTH, Set.of(0)));
        queues.updateWaitCounters(Map.of(Road.NORTH, Set.of(0), Road.SOUTH, Set.of(0)));
        assertEquals(2, queues.stepsSinceGreen(Road.EAST));
    }

    @Test
    void waitCounterResetsWhenRoadTurnsGreen() {
        queues.updateWaitCounters(Map.of(Road.NORTH, Set.of(0), Road.SOUTH, Set.of(0)));
        queues.updateWaitCounters(Map.of(Road.NORTH, Set.of(0), Road.SOUTH, Set.of(0)));
        queues.updateWaitCounters(Map.of(Road.EAST, Set.of(0), Road.WEST, Set.of(0))); // EW turns green
        assertEquals(0, queues.stepsSinceGreen(Road.EAST));
        assertEquals(1, queues.stepsSinceGreen(Road.NORTH)); // now waiting
    }

}
