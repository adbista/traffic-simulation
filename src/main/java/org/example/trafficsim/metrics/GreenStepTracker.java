package org.example.trafficsim.metrics;

/**
 * Tracks the last step each position held a green light and computes capped age.
 * Implements {@link GreenProvider} so policies can read green history without
 * depending on the concrete tracker.
 */
public final class GreenStepTracker implements GreenProvider {

    private final int[] lastGreenStep;

    public GreenStepTracker(int totalPositions) {
        this.lastGreenStep = new int[totalPositions];
    }

    /** Call each time a position goes green in a given step. */
    public void markGreenNow(int positionId, int stepNo) {
        lastGreenStep[positionId] = stepNo;
    }

    @Override
    public long lastGreenStep(int positionId) {
        return lastGreenStep[positionId];
    }

    /** Returns how long the position has been waiting for green, capped at {@code capSteps}. */
    public long age(long stepNo, int positionId, long capSteps) {
        long age = Math.max(0L, stepNo - lastGreenStep[positionId]);
        return Math.min(age, capSteps);
    }
}
