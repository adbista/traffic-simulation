package org.example.trafficsim.metrics;

/** Read-only view of arrival intensity per position, used by phase-selection policies. */
public interface FlowProvider {
    double intensity(int positionId);
}
