package org.example.trafficsim.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlowEmaTrackerTest {

    @Test
    void noArrivals_intensityStaysZero() {
        FlowEmaTracker tracker = new FlowEmaTracker(2, 0.10);

        tracker.advanceStep();
        tracker.advanceStep();
        tracker.advanceStep();

        assertEquals(0.0, tracker.intensity(0), 1e-9);
        assertEquals(0.0, tracker.intensity(1), 1e-9);
    }

    @Test
    void singleArrival_intensityEqualsArrivalTimesBeta() {
        FlowEmaTracker tracker = new FlowEmaTracker(2, 0.20);
        tracker.onArrival(0);

        tracker.advanceStep();

        // EMA = 0*(1-0.2) + 1*0.2 = 0.2
        assertEquals(0.20, tracker.intensity(0), 1e-9);
        assertEquals(0.0,  tracker.intensity(1), 1e-9);
    }

    @Test
    void multipleArrivalsInOneStep_intensityScalesWithCount() {
        FlowEmaTracker tracker = new FlowEmaTracker(2, 1.0); // beta=1: ema = arrivals exactly

        tracker.onArrival(0);
        tracker.onArrival(0);
        tracker.onArrival(0);
        tracker.advanceStep();

        assertEquals(3.0, tracker.intensity(0), 1e-9);
    }

    @Test
    void intensityDecaysWithoutArrivals() {
        FlowEmaTracker tracker = new FlowEmaTracker(2, 0.10);

        tracker.onArrival(0);
        tracker.advanceStep();
        double after1 = tracker.intensity(0); // = 0.10

        // Two more steps, no arrivals
        tracker.advanceStep();
        tracker.advanceStep();

        double expected = after1 * (1 - 0.10) * (1 - 0.10);
        assertEquals(expected, tracker.intensity(0), 1e-9);
    }

    @Test
    void arrivalsResetEachStep() {
        FlowEmaTracker tracker = new FlowEmaTracker(2, 1.0); // beta=1

        tracker.onArrival(0);
        tracker.onArrival(0);
        tracker.advanceStep(); // intensity = 2

        // Zero arrivals next step, beta=1: ema = 0
        tracker.advanceStep();

        assertEquals(0.0, tracker.intensity(0), 1e-9);
    }

    @Test
    void intensityAccumulatesAcrossMultipleSteps() {
        FlowEmaTracker tracker = new FlowEmaTracker(2, 0.50);

        // step 1: +1 arrival -> ema = 0*0.5 + 1*0.5 = 0.5
        tracker.onArrival(0);
        tracker.advanceStep();
        assertEquals(0.50, tracker.intensity(0), 1e-9);

        // step 2: +1 arrival -> ema = 0.5*0.5 + 1*0.5 = 0.75
        tracker.onArrival(0);
        tracker.advanceStep();
        assertEquals(0.75, tracker.intensity(0), 1e-9);

        // step 3: +0 arrivals -> ema = 0.75*0.5 + 0*0.5 = 0.375
        tracker.advanceStep();
        assertEquals(0.375, tracker.intensity(0), 1e-9);
    }

    @Test
    void arrivalsOnDifferentPositionsAreIndependent() {
        FlowEmaTracker tracker = new FlowEmaTracker(3, 1.0);

        tracker.onArrival(0);
        tracker.onArrival(0);
        tracker.onArrival(2);
        tracker.advanceStep();

        assertEquals(2.0, tracker.intensity(0), 1e-9);
        assertEquals(0.0, tracker.intensity(1), 1e-9);
        assertEquals(1.0, tracker.intensity(2), 1e-9);
    }
}
