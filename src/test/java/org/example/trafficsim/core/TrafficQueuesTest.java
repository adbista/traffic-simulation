package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrafficQueuesTest {

    private LaneRegistry twoLane() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{1};
        m[Road.EAST.ordinal()]  = new int[]{};
        return new LaneRegistry(m, List.of(Road.NORTH, Road.SOUTH), List.of(0, 0));
    }

    @Test
    void addVehicle_returnsPositionId() {
        TrafficQueues q = new TrafficQueues(twoLane());
        int posId = q.addVehicle(new Vehicle("v", Road.NORTH, Road.SOUTH, 0, 0, 0));
        assertEquals(0, posId);
    }

    @Test
    void addVehicle_updatesNonEmptyMask() {
        TrafficQueues q = new TrafficQueues(twoLane());
        q.addVehicle(new Vehicle("v", Road.NORTH, Road.SOUTH, 0, 0, 0));
        assertEquals(1L, q.nonEmptyPositionsMask());
    }

    @Test
    void queueLength_correctAfterMultipleAdds() {
        TrafficQueues q = new TrafficQueues(twoLane());
        q.addVehicle(new Vehicle("v1", Road.NORTH, Road.SOUTH, 0, 0, 0));
        q.addVehicle(new Vehicle("v2", Road.NORTH, Road.SOUTH, 0, 1, 0));
        assertEquals(2, q.queueLength(0));
        assertEquals(0, q.queueLength(1));
    }

    @Test
    void poll_removesHeadAndClearsMaskWhenEmpty() {
        TrafficQueues q = new TrafficQueues(twoLane());
        q.addVehicle(new Vehicle("v", Road.NORTH, Road.SOUTH, 0, 0, 0));
        Vehicle v = q.poll(0);
        assertEquals("v", v.id());
        assertEquals(0L, q.nonEmptyPositionsMask());
    }

    @Test
    void poll_maskKeptIfQueueNotEmpty() {
        TrafficQueues q = new TrafficQueues(twoLane());
        q.addVehicle(new Vehicle("v1", Road.NORTH, Road.SOUTH, 0, 0, 0));
        q.addVehicle(new Vehicle("v2", Road.NORTH, Road.SOUTH, 0, 1, 0));
        q.poll(0);
        assertEquals(1L, q.nonEmptyPositionsMask());
    }

    @Test
    void peek_doesNotRemoveVehicle() {
        TrafficQueues q = new TrafficQueues(twoLane());
        q.addVehicle(new Vehicle("v", Road.NORTH, Road.SOUTH, 0, 0, 0));
        q.peek(0);
        assertEquals(1, q.queueLength(0));
    }

    @Test
    void hasAnyVehicleInMask_trueOnlyForOccupiedBits() {
        TrafficQueues q = new TrafficQueues(twoLane());
        q.addVehicle(new Vehicle("v", Road.NORTH, Road.SOUTH, 0, 0, 0));
        assertTrue(q.hasAnyVehicleInMask(0b01L));
        assertFalse(q.hasAnyVehicleInMask(0b10L));
    }

    @Test
    void bothPositionsBitmaskSetWhenBothOccupied() {
        TrafficQueues q = new TrafficQueues(twoLane());
        q.addVehicle(new Vehicle("n", Road.NORTH, Road.SOUTH, 0, 0, 0));
        q.addVehicle(new Vehicle("s", Road.SOUTH, Road.NORTH, 0, 1, 0));
        assertEquals(0b11L, q.nonEmptyPositionsMask());
    }

    @Test
    void roadOf_returnsCorrectRoad() {
        TrafficQueues q = new TrafficQueues(twoLane());
        assertEquals(Road.NORTH, q.roadOf(0));
        assertEquals(Road.SOUTH, q.roadOf(1));
    }

    @Test
    void positionIdsOfRoad_returnsArray() {
        TrafficQueues q = new TrafficQueues(twoLane());
        int[] ids = q.positionIdsOfRoad(Road.NORTH);
        assertEquals(1, ids.length);
        assertEquals(0, ids[0]);
    }
}
