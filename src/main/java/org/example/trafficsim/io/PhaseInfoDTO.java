package org.example.trafficsim.io;

import java.util.List;

/**
 * Describes a single traffic phase: its id and the lanes whose signals go GREEN during it.
 * Sent once after session initialization so the frontend can map phase IDs to lights.
 */
public record PhaseInfoDTO(String id, List<LaneRef> lanes) {

    /** A (road, laneIndex) pair identifying a single inbound lane. */
    public record LaneRef(String road, int laneIndex) {}
}
