package org.example.trafficsim.metrics;

/** Read-only view of the last green step per position, used by phase-selection policies. */
public interface GreenProvider {
    long lastGreenStep(int positionId);
}
