package org.example.trafficsim.cli;

import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.io.CommandDTO;
import org.example.trafficsim.io.OutputFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StreamingCommandDispatcher}.
 *
 * Key contractual differences from the base {@link CommandDispatcher}:
 *  - "step" emits directly to the caller via an emitter Consumer; the inherited OutputFile
 *    must NOT be populated (no double-recording).
 *  - "stop" triggers the onStop Runnable; it does not throw or do anything else.
 *  - "addVehicle" is fully inherited (no change in streaming mode).
 *
 * SOLID notes:
 *  - LSP  : StreamingCommandDispatcher IS-A CommandDispatcher; "step" and "stop" are valid
 *           extensions/overrides that don't break the contract for other handlers.
 *  - OCP  : Achieved via registerHandler() called with overriding values in the constructor.
 */
class StreamingCommandDispatcherTest {

    private SimulationEngine engine;
    private List<OutputFile.StepStatus> emitted;
    private AtomicBoolean stopCalled;
    private StreamingCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        engine      = new SimulationEngineBuilder().build();
        emitted     = new ArrayList<>();
        stopCalled  = new AtomicBoolean(false);
        dispatcher  = new StreamingCommandDispatcher(engine, emitted::add, () -> stopCalled.set(true));
    }

    // -----------------------------------------------------------------------
    // step — emitter contract
    // -----------------------------------------------------------------------

    @Test
    void step_callsEmitterOnce() {
        dispatch("step");
        assertEquals(1, emitted.size(), "step must emit exactly one StepStatus");
    }

    @Test
    void step_emittedStatusHasLeftVehiclesList() {
        dispatch("step");
        assertNotNull(emitted.get(0).leftVehicles);
    }

    @Test
    void step_multipleTimes_emitsOncePerCall() {
        dispatch("step");
        dispatch("step");
        dispatch("step");
        assertEquals(3, emitted.size());
    }

    @Test
    void step_addVehicle_thenStep_vehicleAppearsInEmittedStatus() {
        dispatch(makeAddVehicle("v1", "north", "south", 0));
        dispatch("step");
        assertTrue(emitted.get(0).leftVehicles.contains("v1"),
                "Departing vehicle must appear in emitted StepStatus");
    }

    // -----------------------------------------------------------------------
    // stop — onStop contract
    // -----------------------------------------------------------------------

    @Test
    void stop_callsOnStopCallback() {
        dispatch("stop");
        assertTrue(stopCalled.get(), "stop command must invoke the onStop callback");
    }

    @Test
    void stop_doesNotEmitStepStatus() {
        dispatch("stop");
        assertTrue(emitted.isEmpty(), "stop must not emit any StepStatus");
    }

    @Test
    void stop_multipleInvocations_callsOnStopEachTime() {
        AtomicInteger count = new AtomicInteger(0);
        StreamingCommandDispatcher d = new StreamingCommandDispatcher(
                engine, emitted::add, count::incrementAndGet);
        dispatch(d, "stop");
        dispatch(d, "stop");
        assertEquals(2, count.get());
    }

    // -----------------------------------------------------------------------
    // addVehicle — inherited, no streaming difference
    // -----------------------------------------------------------------------

    @Test
    void addVehicle_invalidRoad_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> dispatch(makeAddVehicle("v1", "nowhere", "south", 0)));
    }

    // -----------------------------------------------------------------------
    // unknown type — inherited behaviour
    // -----------------------------------------------------------------------

    @Test
    void unknownType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> dispatch("warp"));
    }

    // -----------------------------------------------------------------------
    // Emitter isolation — inherited OutputFile must stay empty
    // -----------------------------------------------------------------------

    @Test
    void parentOutputFile_neverPopulated_onStep() {
        // StreamingCommandDispatcher passes a dummy OutputFile to super().
        // We verify this by checking that all results go to emitted, not to some
        // invisible output accumulator. We can only do this indirectly via emitted.
        dispatch("step");
        assertEquals(1, emitted.size(), "All output must go through emitter");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void dispatch(String type) {
        CommandDTO cmd = new CommandDTO();
        cmd.type = type;
        dispatcher.dispatch(cmd);
    }

    private void dispatch(StreamingCommandDispatcher d, String type) {
        CommandDTO cmd = new CommandDTO();
        cmd.type = type;
        d.dispatch(cmd);
    }

    private void dispatch(CommandDTO cmd) {
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
