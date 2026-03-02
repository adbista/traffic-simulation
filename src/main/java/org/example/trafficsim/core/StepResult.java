package org.example.trafficsim.core;

import java.util.List;

// Result of a single simulation step
// leftVehicles -ids of vehicles that passed through the intersection in this step
public record StepResult(List<String> leftVehicles) {}

