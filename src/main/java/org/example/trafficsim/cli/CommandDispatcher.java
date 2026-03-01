package org.example.trafficsim.cli;

import org.example.trafficsim.core.SimulationEngine;
import org.example.trafficsim.core.StepResult;
import org.example.trafficsim.io.CommandDTO;
import org.example.trafficsim.io.OutputFile;
import org.example.trafficsim.model.Road;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;


// Translates CommandDTO objects into SimulationEngine calls
// and appends step results to OutputFile.
// Extracted from Main so that Main only handles wiring and I/O.
//
// OCP: new command types can be registered via registerHandler() without modifying this class.
public class CommandDispatcher {

    private final SimulationEngine engine;
    private final OutputFile output;

    // Handler registry: command type -> action.
    // "addVehicle" and "step" are registered by default.
    private final Map<String, Consumer<CommandDTO>> handlers = new LinkedHashMap<>();

    public CommandDispatcher(SimulationEngine engine, OutputFile output) {
        this.engine = engine;
        this.output = output;
        registerHandler("addVehicle", this::addVehicle);
        registerHandler("step",       c -> step());
    }


    public void registerHandler(String type, Consumer<CommandDTO> handler) {
        handlers.put(type, handler);
    }

    public void dispatch(CommandDTO command) {
        Consumer<CommandDTO> handler = handlers.get(command.type);
        if (handler == null)
            throw new IllegalArgumentException("Unknown command type: " + command.type);
        handler.accept(command);
    }

    // private helpers

    private void addVehicle(CommandDTO c) {
        engine.addVehicle(
                c.vehicleId,
                Road.fromString(c.startRoad),
                Road.fromString(c.endRoad),
                c.lane != null ? c.lane : 0
        );
    }

    private void step() {
        StepResult result = engine.step();
        output.stepStatuses.add(new OutputFile.StepStatus(result.leftVehicles()));
    }
}
