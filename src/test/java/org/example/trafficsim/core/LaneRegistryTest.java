package org.example.trafficsim.core;

import org.example.trafficsim.model.Road;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LaneRegistryTest {

    private LaneRegistry twoLane() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{1};
        m[Road.EAST.ordinal()]  = new int[]{};
        return new LaneRegistry(m, List.of(Road.NORTH, Road.SOUTH), List.of(0, 0));
    }

    @Test
    void totalPositions_returnsCount() {
        assertEquals(2, twoLane().totalPositions());
    }

    @Test
    void positionId_mapsCorrectly() {
        LaneRegistry reg = twoLane();
        assertEquals(0, reg.positionId(Road.NORTH, 0));
        assertEquals(1, reg.positionId(Road.SOUTH, 0));
    }

    @Test
    void roadOf_returnsCorrectRoad() {
        LaneRegistry reg = twoLane();
        assertEquals(Road.NORTH, reg.roadOf(0));
        assertEquals(Road.SOUTH, reg.roadOf(1));
    }

    @Test
    void laneOf_returnsCorrectLane() {
        assertEquals(0, twoLane().laneOf(0));
        assertEquals(0, twoLane().laneOf(1));
    }

    @Test
    void positionId_invalidLane_throws() {
        assertThrows(IllegalArgumentException.class, () -> twoLane().positionId(Road.NORTH, 5));
    }

    @Test
    void roadOf_invalidPositionId_throws() {
        assertThrows(IllegalArgumentException.class, () -> twoLane().roadOf(99));
    }

    @Test
    void lanesOf_north_returnsArray() {
        int[] lanes = twoLane().lanesOf(Road.NORTH);
        assertEquals(1, lanes.length);
        assertEquals(0, lanes[0]);
    }

    @Test
    void lanesOf_emptyRoad_returnsEmpty() {
        assertEquals(0, twoLane().lanesOf(Road.WEST).length);
    }

    @Test
    void constructor_mismatchedListSizes_throws() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{};
        m[Road.EAST.ordinal()]  = new int[]{};
        assertThrows(IllegalArgumentException.class,
                () -> new LaneRegistry(m, List.of(Road.NORTH), List.of(0, 0)));
    }

    @Test
    void multiLane_eachLaneMappedToUniqueId() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0, 1};
        m[Road.WEST.ordinal()]  = new int[]{};
        m[Road.SOUTH.ordinal()] = new int[]{};
        m[Road.EAST.ordinal()]  = new int[]{};
        LaneRegistry reg = new LaneRegistry(m, List.of(Road.NORTH, Road.NORTH), List.of(0, 1));
        assertEquals(0, reg.positionId(Road.NORTH, 0));
        assertEquals(1, reg.positionId(Road.NORTH, 1));
        assertEquals(Road.NORTH, reg.roadOf(0));
        assertEquals(Road.NORTH, reg.roadOf(1));
        assertEquals(0, reg.laneOf(0));
        assertEquals(1, reg.laneOf(1));
    }
}
