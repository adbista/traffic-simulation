package org.example.trafficsim.model;

/**
 * @param minGreenSteps minimum steps the phase must stay green before a switch can be requested
 * @param maxGreenSteps maximum steps the phase may stay green before a forced switch
 * @param yellowSteps   number of amber steps between green and all-red (0 = no amber)
 * @param redSteps      number of all-red clearance steps before the next green phase starts (0 = no clearance)
 */
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
}