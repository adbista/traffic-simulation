package org.example.trafficsim.command;

import org.example.trafficsim.command.CommandDispatcher;
import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.core.StepResult;
import org.example.trafficsim.io.OutputFile;

import java.util.function.Consumer;

public class StreamingCommandDispatcher extends CommandDispatcher {

    public StreamingCommandDispatcher(SimulationEngine engine,
                                      Consumer<OutputFile.StepStatus> emitter,
                                      Runnable onStop) {
        // Parent still owns addVehicle; we pass a dummy OutputFile since we never aggregate
        super(engine, new OutputFile());

        // Override "step": emit immediately instead of collecting
        registerHandler("step", c -> {
            StepResult result = engine.step();
            emitter.accept(new OutputFile.StepStatus(result.leftVehicles()));
        });

        // New command: stop the streaming session
        registerHandler("stop", c -> onStop.run());
    }
}
