package org.example.trafficsim.metrics;

/**
 * Tracks exponential moving average (EMA) of vehicle arrivals per position.
 * Implements {@link FlowProvider} so policies can read intensity without
 * depending on the concrete tracker.
 */
public final class FlowEmaTracker implements FlowProvider {

    private final int[] arrivalsThisStep;
    private final double[] intensityEma;
    private final double beta;

    public FlowEmaTracker(int totalPositions, double beta) {
        this.beta = beta;
        this.arrivalsThisStep = new int[totalPositions];
        this.intensityEma = new double[totalPositions];
    }

    /** Call once per vehicle arrival on the given position. */
    public void onArrival(int positionId) {
        arrivalsThisStep[positionId]++;
    }

    /** Call once at the start of each simulation step to advance the EMA. */
    public void advanceStep() {
        for (int i = 0; i < intensityEma.length; i++) {
            intensityEma[i] = intensityEma[i] * (1.0 - beta) + arrivalsThisStep[i] * beta;
            arrivalsThisStep[i] = 0;
        }
    }

    @Override
    public double intensity(int positionId) {
        return intensityEma[positionId];
    }
}
