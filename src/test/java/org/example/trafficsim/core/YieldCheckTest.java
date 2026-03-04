package org.example.trafficsim.core;

import org.example.trafficsim.model.*;
import org.example.trafficsim.signal.Phase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class YieldCheckTest {

    private static final PhaseTiming TIMING = new PhaseTiming(1, 10, 0, 1);

    // Registry: NORTH=0, WEST=1, SOUTH=2, EAST=3
    private LaneRegistry fourLaneReg() {
        int[][] m = new int[4][];
        m[Road.NORTH.ordinal()] = new int[]{0};
        m[Road.WEST.ordinal()]  = new int[]{1};
        m[Road.SOUTH.ordinal()] = new int[]{2};
        m[Road.EAST.ordinal()]  = new int[]{3};
        return new LaneRegistry(m,
                List.of(Road.NORTH, Road.WEST, Road.SOUTH, Road.EAST),
                List.of(0, 0, 0, 0));
    }

    // Phase with NORTH+SOUTH both GENERIC (LEFT | STRAIGHT)
    private Phase northSouthGenericPhase() {
        int[] masks = new int[]{
                MovementMask.LEFT | MovementMask.STRAIGHT,
                0,
                MovementMask.LEFT | MovementMask.STRAIGHT,
                0};
        return new Phase("NS", TIMING, 0b0101L, masks, List.of(
                new LaneSignal(Road.NORTH, 0, MovementMask.LEFT | MovementMask.STRAIGHT, TrafficLightType.GENERIC),
                new LaneSignal(Road.SOUTH, 0, MovementMask.LEFT | MovementMask.STRAIGHT, TrafficLightType.GENERIC)
        ));
    }

    // Phase with NORTH having PROTECTED LEFT only
    private Phase northProtectedLeftPhase() {
        int[] masks = new int[]{MovementMask.LEFT, 0, 0, 0};
        return new Phase("NL", TIMING, 0b0001L, masks, List.of(
                new LaneSignal(Road.NORTH, 0, MovementMask.LEFT, TrafficLightType.PROTECTED)
        ));
    }

    @Test
    void protected_leftTurn_alwaysAllowed_evenWithOpposingVehicle() {
        TrafficQueues q = new TrafficQueues(fourLaneReg());
        q.addVehicle(new Vehicle("opp", Road.SOUTH, Road.NORTH, 0, 0, 0)); // STRAIGHT from south
        Vehicle leftTurner = new Vehicle("lt", Road.NORTH, Road.EAST, 0, 1, 0); // LEFT from north

        assertTrue(YieldCheck.check(leftTurner, northProtectedLeftPhase(), q));
    }

    @Test
    void generic_leftTurn_blockedByOppositeGoingStraight() {
        TrafficQueues q = new TrafficQueues(fourLaneReg());
        q.addVehicle(new Vehicle("opp", Road.SOUTH, Road.NORTH, 0, 0, 0)); // STRAIGHT
        Vehicle leftTurner = new Vehicle("lt", Road.NORTH, Road.EAST, 0, 1, 0); // LEFT

        assertFalse(YieldCheck.check(leftTurner, northSouthGenericPhase(), q));
    }

    @Test
    void generic_leftTurn_allowedWhenNoOpposingVehicles() {
        TrafficQueues q = new TrafficQueues(fourLaneReg());
        Vehicle leftTurner = new Vehicle("lt", Road.NORTH, Road.EAST, 0, 0, 0);

        assertTrue(YieldCheck.check(leftTurner, northSouthGenericPhase(), q));
    }

    @Test
    void generic_leftTurn_allowedWhenOppositeGoesLeft() {
        TrafficQueues q = new TrafficQueues(fourLaneReg());
        // SOUTH going LEFT (to WEST) - not STRAIGHT or RIGHT, should not block
        q.addVehicle(new Vehicle("opp", Road.SOUTH, Road.WEST, 0, 0, 0));
        Vehicle leftTurner = new Vehicle("lt", Road.NORTH, Road.EAST, 0, 1, 0);

        assertTrue(YieldCheck.check(leftTurner, northSouthGenericPhase(), q));
    }

    @Test
    void generic_leftTurn_blockedByOppositeGoingRight() {
        TrafficQueues q = new TrafficQueues(fourLaneReg());
        // SOUTH going RIGHT (to EAST) - should block LEFT from NORTH
        q.addVehicle(new Vehicle("opp", Road.SOUTH, Road.EAST, 0, 0, 0));
        Vehicle leftTurner = new Vehicle("lt", Road.NORTH, Road.EAST, 0, 1, 0);

        assertFalse(YieldCheck.check(leftTurner, northSouthGenericPhase(), q));
    }

    @Test
    void straight_movement_notSubjectToYield() {
        TrafficQueues q = new TrafficQueues(fourLaneReg());
        q.addVehicle(new Vehicle("opp", Road.SOUTH, Road.NORTH, 0, 0, 0));
        Vehicle straight = new Vehicle("s", Road.NORTH, Road.SOUTH, 0, 1, 0);

        assertTrue(YieldCheck.check(straight, northSouthGenericPhase(), q));
    }
}
