package org.example.trafficsim.signal;

// Timing configuration for a single signal phase (values in simulation steps).
// Standard cycle: GREEN (minGreen–maxGreen) → YELLOW (yellowSteps) → RED (redSteps) → GREEN
public record PhaseTiming(
        int minGreenSteps,
        int maxGreenSteps,
        int yellowSteps,
        int redSteps
) {
    public PhaseTiming {
        if (minGreenSteps < 0) {
            throw new IllegalArgumentException("minGreenSteps cannot be negative");
        }
        if (maxGreenSteps < minGreenSteps) {
            throw new IllegalArgumentException("maxGreenSteps cannot be smaller than minGreenSteps");
        }
        if (yellowSteps < 0) {
            throw new IllegalArgumentException("yellowSteps cannot be negative");
        }
        if (redSteps < 0) {
            throw new IllegalArgumentException("redSteps cannot be negative");
        }
    }

    public PhaseTiming(int minGreenSteps, int maxGreenSteps, int allRedSteps) {
        this(minGreenSteps, maxGreenSteps, 0, allRedSteps);
    }
}