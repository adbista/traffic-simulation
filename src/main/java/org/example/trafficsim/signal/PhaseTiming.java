package org.example.trafficsim.signal;

// Timing configuration for a single signal phase (values in simulation steps).
// Standard cycle: GREEN (minGreen–maxGreen) → YELLOW (yellowSteps) → ALL_RED (allRedSteps) → GREEN
public record PhaseTiming(
        int minGreenSteps,  // minimum steps before a switch is allowed
        int maxGreenSteps,  // maximum green steps; switch is forced afterwards
        int yellowSteps,    // steps spent yellow (vehicles slow down, no new departures)
        int allRedSteps     // steps with all-red before the next green starts
) {
    // Short constructor: yellowSteps defaults to 0 (no explicit yellow; backwards compatibility).
    // Set yellowSteps > 0 explicitly for a realistic yellow phase.
    public PhaseTiming(int minGreenSteps, int maxGreenSteps, int allRedSteps) {
        this(minGreenSteps, maxGreenSteps, 0, allRedSteps);
    }
}