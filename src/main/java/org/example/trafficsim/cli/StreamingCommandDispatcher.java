package org.example.trafficsim.cli;

import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.core.StepResult;
import org.example.trafficsim.io.OutputFile;

import java.util.function.Consumer;

/**
 * Extends CommandDispatcher for streaming use-cases (WebSocket).
 *
 * Differences from the base class:
 *  - "step" handler emits each {@link OutputFile.StepStatus} directly to the caller
 *    via an {@code emitter} callback instead of collecting into an OutputFile.
 *  - "stop" handler is registered and calls an {@code onStop} callback so the transport
 *    layer (e.g. WebSocket session) can decide how to close the connection.
 *
 * addVehicle (and any other handlers registered on the base class) are inherited unchanged.
 *
 * Design note: {@code engine} is captured from the constructor parameter (effectively final)
 * rather than the private parent field, so no change to CommandDispatcher visibility is needed.
 * {@code registerHandler} is called after {@code super()} to override the default "step" handler
 * and add the new "stop" handler — this is the standard OCP extension point already present on
 * the base class.
 */
public class StreamingCommandDispatcher extends CommandDispatcher {

    /**
     * @param engine  the simulation engine (shared with the parent via super())
     * @param emitter called once per "step" command with the resulting step status
     * @param onStop  called when the "stop" command is dispatched; the caller is responsible
     *                for closing the underlying channel
     */
    public StreamingCommandDispatcher(SimulationEngine engine,
                                      Consumer<OutputFile.StepStatus> emitter,
                                      Runnable onStop) {
        // Parent still owns addVehicle; we pass a dummy OutputFile since we never aggregate.
        super(engine, new OutputFile());

        // Override "step": emit immediately instead of collecting.
        registerHandler("step", c -> {
            StepResult result = engine.step();
            emitter.accept(new OutputFile.StepStatus(result.leftVehicles()));
        });

        // New command: stop the streaming session.
        registerHandler("stop", c -> onStop.run());
    }
}
