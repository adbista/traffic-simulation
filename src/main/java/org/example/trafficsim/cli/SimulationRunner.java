package org.example.trafficsim.cli;

import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.OutputFile;
import org.example.trafficsim.signal.PhaseSetGenerator;

// Encapsulates the core simulation pipeline:
//   parse config -> build engine -> dispatch commands -> return output.
//
// Used by both the CLI entry-point (Main) and the REST API (SimulationController)
// so that the business logic lives in exactly one place.
public final class SimulationRunner {

    private SimulationRunner() {}

    /**
     * Runs the simulation described by {@code input} and returns the result.
     *
     * @throws IllegalArgumentException if any command has an unknown type or invalid road name
     */
    public static OutputFile run(InputFile input) {
        InputReader.ParsedConfig config = InputReader.parseConfig(input);
        PhaseSetGenerator generator = new PhaseSetGenerator(
                config.laneDeclarations(), config.phaseTiming());

        SimulationEngine engine = new SimulationEngineBuilder()
                .phaseFactory(generator)
                .build();

        OutputFile output = new OutputFile();
        CommandDispatcher dispatcher = new CommandDispatcher(engine, output);
        input.commands.forEach(dispatcher::dispatch);

        return output;
    }
}
