package org.example.trafficsim.cli;

import org.example.trafficsim.io.CommandDTO;
import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.OutputFile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimulationRunner}.
 *
 * SimulationRunner is the shared execution pipeline used by both the CLI and REST API.
 * Tests here verify the contract at the boundary: InputFile in → OutputFile out.
 *
 * SOLID notes:
 *  - SRP  : SimulationRunner has a single responsibility — execute the pipeline.
 *           Both Main and SimulationController delegate to it (no duplication).
 *  - DIP  : runner depends on InputFile (abstract IO), not on File/HTTP transport.
 */
class SimulationRunnerTest {

    // -----------------------------------------------------------------------
    // Empty / minimal input
    // -----------------------------------------------------------------------

    @Test
    void run_emptyCommands_returnsEmptyStepStatuses() {
        InputFile in = makeInput(List.of());
        OutputFile out = SimulationRunner.run(in);
        assertNotNull(out);
        assertTrue(out.stepStatuses.isEmpty());
    }

    @Test
    void run_stepsOnly_emitsOneStatusPerStep() {
        InputFile in = makeInput(List.of(step(), step(), step()));
        OutputFile out = SimulationRunner.run(in);
        assertEquals(3, out.stepStatuses.size());
    }

    @Test
    void run_stepsOnly_allLeftVehiclesEmpty() {
        InputFile in = makeInput(List.of(step(), step()));
        OutputFile out = SimulationRunner.run(in);
        out.stepStatuses.forEach(s -> assertTrue(s.leftVehicles.isEmpty()));
    }

    // -----------------------------------------------------------------------
    // Vehicle departure
    // -----------------------------------------------------------------------

    @Test
    void run_addVehicleThenStep_vehicleDepartsInFirstGreenStep() {
        List<CommandDTO> cmds = new ArrayList<>();
        cmds.add(addVehicle("car1", "north", "south", 0));
        cmds.add(step());
        cmds.add(step());
        cmds.add(step()); // enough steps for green phase

        InputFile in = makeInput(cmds);
        OutputFile out = SimulationRunner.run(in);

        boolean departed = out.stepStatuses.stream()
                .anyMatch(s -> s.leftVehicles.contains("car1"));
        assertTrue(departed, "car1 must depart within a few steps");
    }

    @Test
    void run_scenario01_mirrored() {
        List<CommandDTO> cmds = List.of(
                addVehicle("vehicle1", "south", "north", 0),
                addVehicle("vehicle2", "north", "south", 0),
                step(),
                step(),
                addVehicle("vehicle3", "west", "south", 0),
                addVehicle("vehicle4", "west", "south", 0),
                step(),
                step()
        );

        OutputFile out = SimulationRunner.run(makeInput(cmds));

        assertEquals(4, out.stepStatuses.size());
        assertEquals(List.of("vehicle1", "vehicle2"), out.stepStatuses.get(0).leftVehicles, "step 1");
        assertEquals(List.of(),                        out.stepStatuses.get(1).leftVehicles, "step 2");
        assertEquals(List.of("vehicle3"),              out.stepStatuses.get(2).leftVehicles, "step 3");
        assertEquals(List.of("vehicle4"),              out.stepStatuses.get(3).leftVehicles, "step 4");
    }

    // -----------------------------------------------------------------------
    // Error propagation
    // -----------------------------------------------------------------------

    @Test
    void run_unknownCommandType_throwsIllegalArgumentException() {
        List<CommandDTO> cmds = List.of(named("fly"));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationRunner.run(makeInput(cmds)));
    }

    @Test
    void run_invalidRoadName_throwsIllegalArgumentException() {
        List<CommandDTO> cmds = List.of(addVehicle("v1", "moon", "north", 0));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationRunner.run(makeInput(cmds)));
    }

    // -----------------------------------------------------------------------
    // Isolation — each run is independent
    // -----------------------------------------------------------------------

    @Test
    void run_calledTwice_eachRunHasFreshEngine() {
        InputFile in = makeInput(List.of(addVehicle("v1", "north", "south", 0), step()));

        OutputFile first  = SimulationRunner.run(in);
        OutputFile second = SimulationRunner.run(in);

        // Both runs must independently produce the same result (no shared mutable state)
        assertEquals(first.stepStatuses.get(0).leftVehicles,
                     second.stepStatuses.get(0).leftVehicles,
                     "Repeated runs with same input must produce identical output");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static InputFile makeInput(List<CommandDTO> commands) {
        InputFile in = new InputFile();
        in.commands = new ArrayList<>(commands);
        return in;
    }

    private static CommandDTO step() {
        CommandDTO c = new CommandDTO(); c.type = "step"; return c;
    }

    private static CommandDTO named(String type) {
        CommandDTO c = new CommandDTO(); c.type = type; return c;
    }

    private static CommandDTO addVehicle(String id, String start, String end, int lane) {
        CommandDTO c = new CommandDTO();
        c.type      = "addVehicle";
        c.vehicleId = id;
        c.startRoad = start;
        c.endRoad   = end;
        c.lane      = lane;
        return c;
    }
}
