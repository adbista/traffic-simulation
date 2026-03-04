package org.example.trafficsim.model;

// Represents all signalled movements on a particular lane of a road, encoded as a bitmask.
// GENERIC movements on the same lane are merged into one mask;
// each PROTECTED movement gets its own LaneSignal with a single-bit mask.
// Example: LaneSignal(NORTH, 0, MovementMask.LEFT, PROTECTED)
//          LaneSignal(NORTH, 1, MovementMask.STRAIGHT | MovementMask.RIGHT, GENERIC)
public record LaneSignal(Road road, int laneIndex, int movementMask, TrafficLightType trafficLightType) { }
