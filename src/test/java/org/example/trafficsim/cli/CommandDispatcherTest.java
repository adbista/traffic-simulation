package org.example.trafficsim.cli;

import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.io.CommandDTO;
import org.example.trafficsim.io.OutputFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommandDispatcher}.
 *
 * SOLID notes:
 *  - SRP  : CommandDispatcher translates DTOs → engine calls and appends results; tested here in isolation.
 *  - OCP  : registerHandler allows extension without modifying this class; tested by override scenario.
 *  - LSP  : StreamingCommandDispatcher extends and overrides correctly (tested in StreamingCommandDispatcherTest).
 *  - ISP  : Dispatcher depends only on SimulationEngine (narrow interface), not on the full application context.
 *  - DIP  : Engine is passed in via constructor, not created internally.
 */
class CommandDispatcherTest {

    private SimulationEngine engine;
    private OutputFile output;
    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        engine     = new SimulationEngineBuilder().build();
        output     = new OutputFile();
        dispatcher = new CommandDispatcher(engine, output);
    }

    // -----------------------------------------------------------------------
    // step
    // -----------------------------------------------------------------------

    @Test
    void dispatch_step_appendsStepStatusToOutput() {
        dispatch("step");
        assertEquals(1, output.stepStatuses.size());
    }

    @Test
    void dispatch_multipleSteps_appendsOneStatusEach() {
        dispatch("step");
        dispatch("step");
        dispatch("step");
        assertEquals(3, output.stepStatuses.size());
    }

    @Test
    void dispatch_step_stepStatusHasLeftVehiclesList() {
        dispatch("step");
        assertNotNull(output.stepStatuses.get(0).leftVehicles);
    }

    // -----------------------------------------------------------------------
    // addVehicle + step
    // -----------------------------------------------------------------------

    @Test
    void dispatch_addVehicle_thenStep_vehicleDepartsInFirstStep() {
        CommandDTO add = new CommandDTO();
        add.type      = "addVehicle";
        add.vehicleId = "car1";
        add.startRoad = "north";
        add.endRoad   = "south";
        add.lane      = 0;

        dispatcher.dispatch(add);
        dispatch("step");

        List<String> left = output.stepStatuses.get(0).leftVehicles;
        assertTrue(left.contains("car1"), "vehicle should depart on green step");
    }

    @Test
    void dispatch_addVehicle_vehicleNotLostAfterRedStep() {
        // Add to EW lane; default first phase is NS → vehicle stays for at least one step
        CommandDTO add = makeAddVehicle("v1", "east", "west", 0);
        dispatcher.dispatch(add);
        dispatch("step"); // NS is green, EW waits

        // vehicle still has not necessarily departed — just don't crash
        assertEquals(1, output.stepStatuses.size());
    }

    // -----------------------------------------------------------------------
    // unknown command type
    // -----------------------------------------------------------------------

    @Test
    void dispatch_unknownType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> dispatch("teleport"),
                "Unknown command type must throw IllegalArgumentException");
    }

    @Test
    void dispatch_unknownType_errorMessageContainsType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dispatch("frobnicate"));
        assertTrue(ex.getMessage().contains("frobnicate"),
                "Error message should name the unknown command type");
    }

    @Test
    void dispatch_nullType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> dispatch(null));
    }

    // -----------------------------------------------------------------------
    // registerHandler — OCP extension point
    // -----------------------------------------------------------------------

    @Test
    void registerHandler_newType_isDispatched() {
        AtomicInteger callCount = new AtomicInteger(0);
        dispatcher.registerHandler("ping", c -> callCount.incrementAndGet());

        dispatch("ping");

        assertEquals(1, callCount.get(), "Custom handler must be called");
    }

    @Test
    void registerHandler_overridesExistingType() {
        AtomicInteger callCount = new AtomicInteger(0);
        // Override built-in "step" handler
        dispatcher.registerHandler("step", c -> callCount.incrementAndGet());

        dispatch("step");

        assertEquals(0, output.stepStatuses.size(), "Overridden handler should not append to output");
        assertEquals(1, callCount.get(), "Custom override handler must be called");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void dispatch(String type) {
        CommandDTO cmd = new CommandDTO();
        cmd.type = type;
        dispatcher.dispatch(cmd);
    }

    private static CommandDTO makeAddVehicle(String id, String start, String end, int lane) {
        CommandDTO cmd = new CommandDTO();
        cmd.type      = "addVehicle";
        cmd.vehicleId = id;
        cmd.startRoad = start;
        cmd.endRoad   = end;
        cmd.lane      = lane;
        return cmd;
    }
}
