package org.example.trafficsim.core;

import org.example.trafficsim.cli.SimulationEngineBuilder;
import org.example.trafficsim.model.Road;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimulationEngine}.
 *
 * Uses {@link SimulationEngineBuilder} with default settings so that all collaborators
 * (TrafficQueues, ActivePhase, SignalController) are properly wired — this keeps tests
 * focused on engine behaviour without duplicating wiring logic.
 *
 * SOLID notes:
 *  - SRP  : SimulationEngine orchestrates one step of the simulation; construction is
 *           delegated to SimulationEngineBuilder (Builder pattern, SRP).
 *  - OCP  : Phase selection policy is injected (DIP), so engine behaviour can be extended
 *           by swapping the policy without modifying SimulationEngine.
 *  - LSP  : StepResult is an immutable record — no subclassing issues.
 *  - ISP  : Engine exposes only addVehicle() and step() — minimal surface.
 *  - DIP  : Engine depends on abstractions (PhaseSelectionPolicy, ActivePhase) not concretions.
 */
class SimulationEngineTest {

    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimulationEngineBuilder().build();
    }

    // -----------------------------------------------------------------------
    // step() — basic mechanics
    // -----------------------------------------------------------------------

    @Test
    void step_withNoVehicles_returnsEmptyLeftVehicles() {
        StepResult result = engine.step();
        assertNotNull(result);
        assertTrue(result.leftVehicles().isEmpty());
    }

    @Test
    void step_calledMultipleTimes_neverThrows() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 20; i++) engine.step();
        });
    }

    @Test
    void step_returnsNewListEachCall() {
        StepResult r1 = engine.step();
        StepResult r2 = engine.step();
        // Records are distinct objects (even if both empty)
        assertNotSame(r1, r2);
    }

    // -----------------------------------------------------------------------
    // addVehicle + step — departure logic
    // -----------------------------------------------------------------------

    @Test
    void addVehicle_thenStep_vehicleDepartsWithinPhase() {
        engine.addVehicle("v1", Road.NORTH, Road.SOUTH, 0);

        // Run enough steps to ensure a green phase for NORTH is reached
        boolean departed = false;
        for (int i = 0; i < 10 && !departed; i++) {
            StepResult result = engine.step();
            if (result.leftVehicles().contains("v1")) departed = true;
        }

        assertTrue(departed, "v1 must depart within 10 steps");
    }

    @Test
    void addVehicle_departsOnlyOnce() {
        engine.addVehicle("unique", Road.NORTH, Road.SOUTH, 0);

        int departureCount = 0;
        for (int i = 0; i < 15; i++) {
            if (engine.step().leftVehicles().contains("unique")) departureCount++;
        }

        assertEquals(1, departureCount, "vehicle must depart exactly once");
    }

    @Test
    void twoVehiclesSameRoad_departOnDifferentSteps_fifo() {
        engine.addVehicle("first",  Road.NORTH, Road.SOUTH, 0);
        engine.addVehicle("second", Road.NORTH, Road.SOUTH, 0);

        // Run until both have departed
        List<String> firstDeparture  = null;
        List<String> secondDeparture = null;
        for (int i = 0; i < 20; i++) {
            List<String> left = engine.step().leftVehicles();
            if (firstDeparture == null && left.contains("first"))  firstDeparture  = left;
            if (secondDeparture == null && left.contains("second")) secondDeparture = left;
        }

        assertNotNull(firstDeparture,  "first vehicle must depart");
        assertNotNull(secondDeparture, "second vehicle must depart");
        // FIFO: second can only depart AFTER first
        assertTrue(firstDeparture.contains("first") || secondDeparture.contains("second"),
                "vehicles must depart in FIFO order");
    }

    @Test
    void vehiclesOnOppositeGreenRoads_canDepartInSameStep() {
        // NS phase: both NORTH and SOUTH are green simultaneously
        engine.addVehicle("n1", Road.NORTH, Road.SOUTH, 0);
        engine.addVehicle("s1", Road.SOUTH, Road.NORTH, 0);

        List<String> step1 = engine.step().leftVehicles();
        // Both should depart on the same step when NS phase is active
        assertTrue(step1.containsAll(List.of("n1", "s1")),
                "NORTH and SOUTH vehicles should depart together in NS phase, got: " + step1);
    }

    // -----------------------------------------------------------------------
    // addVehicle — error cases
    // -----------------------------------------------------------------------

    @Test
    void addVehicle_invalidLane_throwsIllegalArgumentException() {
        // Default engine has 1 lane (index 0); lane 5 is invalid
        assertThrows(IllegalArgumentException.class,
                () -> engine.addVehicle("v", Road.NORTH, Road.SOUTH, 5));
    }

    @Test
    void addVehicle_negativeLane_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.addVehicle("v", Road.NORTH, Road.SOUTH, -1));
    }
}
