package org.example.trafficsim.cli;

import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.OutputFile;

public final class SimulationRunner {

    private SimulationRunner() {}

    public static OutputFile run(InputFile input) {
        InputReader.ParsedConfig config = InputReader.parseConfig(input);

        SimulationEngine engine = new SimulationEngineBuilder()
                .laneDeclarations(config.laneDeclarations())
                .phaseTiming(config.phaseTiming())
                .build();

        OutputFile output = new OutputFile();
        CommandDispatcher dispatcher = new CommandDispatcher(engine, output);
        input.commands.forEach(dispatcher::dispatch);

        return output;
    }
}