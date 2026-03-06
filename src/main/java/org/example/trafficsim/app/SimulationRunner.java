package org.example.trafficsim.app;

import org.example.trafficsim.command.CommandDispatcher;
import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.core.SimulationEngineBuilder;
import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.InputReader;
import org.example.trafficsim.io.OutputFile;

public final class SimulationRunner {

    private SimulationRunner() {}

    public static OutputFile run(InputFile input) {
        IntersectionConfig config = InputReader.parseConfig(input);

        SimulationEngine engine = new SimulationEngineBuilder(config)
                .build();

        OutputFile output = new OutputFile();
        CommandDispatcher dispatcher = new CommandDispatcher(engine, output);
        if (input.commands != null) {
            input.commands.forEach(dispatcher::dispatch);
        }

        return output;
    }
}