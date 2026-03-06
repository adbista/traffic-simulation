package org.example.trafficsim.signal;

import org.example.trafficsim.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActivePhaseTest {

    private static final PhaseTiming NO_YELLOW    = new PhaseTiming(1, 10, 0, 1);
    private static final PhaseTiming WITH_YELLOW  = new PhaseTiming(1, 10, 2, 1);

    private Phase makePhase(String id, PhaseTiming timing) {
        return new Phase(id, timing, 0b01L, new int[]{MovementMask.STRAIGHT},
                List.of(new LaneSignal(Road.NORTH, 0, MovementMask.STRAIGHT, TrafficLightType.GENERIC)));
    }

    @Test
    void constructor_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ActivePhase(null));
    }

    @Test
    void initialState_isGreen() {
        assertTrue(new ActivePhase(makePhase("p", NO_YELLOW)).isGreen());
    }

    @Test
    void requestNextPhase_sameId_ignored() {
        Phase p = makePhase("p", NO_YELLOW);
        ActivePhase ap = new ActivePhase(p);
        ap.requestNextPhase(p);
        assertTrue(ap.isGreen());
        assertEquals("p", ap.current().id());
    }

    @Test
    void requestNextPhase_withYellow_leavesGreen() {
        Phase p0 = makePhase("p0", WITH_YELLOW);
        Phase p1 = makePhase("p1", WITH_YELLOW);
        ActivePhase ap = new ActivePhase(p0);
        ap.requestNextPhase(p1);
        assertFalse(ap.isGreen());
    }

    @Test
    void requestNextPhase_noYellow_switchesAfterOneRedStep() {
        Phase p0 = makePhase("p0", NO_YELLOW);
        Phase p1 = makePhase("p1", NO_YELLOW);
        ActivePhase ap = new ActivePhase(p0);
        ap.requestNextPhase(p1);
        ap.manageLightState(); // 1 red step -> green p1
        assertTrue(ap.isGreen());
        assertEquals("p1", ap.current().id());
    }

    @Test
    void requestNextPhase_withYellow_fullCycleEndsOnNewPhase() {
        Phase p0 = makePhase("p0", WITH_YELLOW); // 2 yellow, 1 red
        Phase p1 = makePhase("p1", WITH_YELLOW);
        ActivePhase ap = new ActivePhase(p0);
        ap.requestNextPhase(p1);
        ap.manageLightState(); // yellow step 1
        ap.manageLightState(); // yellow step 2 -> RED
        ap.manageLightState(); // red step 1 -> GREEN p1
        assertTrue(ap.isGreen());
        assertEquals("p1", ap.current().id());
    }

    @Test
    void requestNextPhase_ignoredWhenNotGreen() {
        Phase p0 = makePhase("p0", WITH_YELLOW);
        Phase p1 = makePhase("p1", WITH_YELLOW);
        Phase p2 = makePhase("p2", WITH_YELLOW);
        ActivePhase ap = new ActivePhase(p0);
        ap.requestNextPhase(p1); // now YELLOW
        ap.requestNextPhase(p2); // ignored
        ap.manageLightState();
        ap.manageLightState();
        ap.manageLightState();
        assertEquals("p1", ap.current().id());
    }

    @Test
    void timeInState_incrementsOnEachManageCall() {
        ActivePhase ap = new ActivePhase(makePhase("p", NO_YELLOW));
        ap.manageLightState();
        ap.manageLightState();
        assertEquals(2, ap.timeInState());
    }

    @Test
    void timeInState_resetsAfterPhaseSwitch() {
        Phase p0 = makePhase("p0", NO_YELLOW);
        Phase p1 = makePhase("p1", NO_YELLOW);
        ActivePhase ap = new ActivePhase(p0);
        ap.manageLightState(); // timeInState = 1
        ap.requestNextPhase(p1); // timeInState reset to 0
        ap.manageLightState();   // RED step -> green p1, timeInState = 0
        assertEquals(0, ap.timeInState());
    }
}
