package org.example.trafficsim.model;

/**
 * Represents a single signal group on a lane: a bitmask of movements that share
 * one traffic light (identified by the same trafficLightId in the JSON config).
 */
public record SignalGroupSpec(int movementMask, TrafficLightType type) {}
