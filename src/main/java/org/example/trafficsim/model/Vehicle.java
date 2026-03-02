package org.example.trafficsim.model;

public record Vehicle(
        String id,
        Road startRoad,
        Road endRoad, // not used currently
        long createdAtStep, // not used currently
        long seqNum, // global insertion order used for deterministic output sorting
        int lane // 0-indexed lane on startRoad
) {}
