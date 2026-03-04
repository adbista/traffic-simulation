package org.example.trafficsim.config;

import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.model.SignalGroupSpec;
import org.example.trafficsim.model.PhaseTiming;

import java.util.List;
import java.util.Map;

// Parsed configuration for a single intersection simulation run
public record IntersectionConfig(
        PhaseTiming phaseTiming,
        LaneRegistry laneRegistry,
        Map<Integer, List<SignalGroupSpec>> movementSignalsByPosition,
        Map<Integer, Double> laneWeights
) {}