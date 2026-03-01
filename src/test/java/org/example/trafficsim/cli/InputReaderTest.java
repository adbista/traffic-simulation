package org.example.trafficsim.cli;

import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.LaneConfigDTO;
import org.example.trafficsim.io.SimConfig;
import org.example.trafficsim.signal.PhaseTiming;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InputReader#parseConfig(SimConfig)} and
 * {@link InputReader#parseConfig(InputFile)}.
 *
 * SOLID notes:
 *  - SRP  : InputReader has one job — transform raw IO objects into a ParsedConfig.
 *           These tests verify that boundary, not engine behaviour.
 *  - OCP  : Adding a new config variant requires only a new overload / branch in InputReader
 *           and a new test here; existing tests are unaffected.
 */
class InputReaderTest {

    // -----------------------------------------------------------------------
    // parseConfig(SimConfig) — null / empty → defaults
    // -----------------------------------------------------------------------

    @Test
    void parseConfig_nullSimConfig_usesDefaultTiming() {
        InputReader.ParsedConfig cfg = InputReader.parseConfig((SimConfig) null);
        // Default STANDARD_TIMING constructed as PhaseTiming(1, 5, 1): min=1, max=5, allRed=1
        PhaseTiming t = cfg.phaseTiming();
        assertEquals(1, t.minGreenSteps(), "default minGreen");
        assertEquals(5, t.maxGreenSteps(), "default maxGreen");
        assertEquals(1, t.allRedSteps(),   "default allRed");
    }

    @Test
    void parseConfig_nullSimConfig_producesFourLaneDeclarations() {
        InputReader.ParsedConfig cfg = InputReader.parseConfig((SimConfig) null);
        // One default lane per road (4 roads)
        assertEquals(4, cfg.laneDeclarations().size(), "should have 1 declaration per road");
    }

    @Test
    void parseConfig_simConfigWithNullLaneDeclarations_usesDefaults() {
        SimConfig config = new SimConfig();
        config.laneDeclarations = null;

        InputReader.ParsedConfig cfg = InputReader.parseConfig(config);
        PhaseTiming t = cfg.phaseTiming();
        assertEquals(5, t.maxGreenSteps(), "null laneDeclarations → standard timing");
    }

    // -----------------------------------------------------------------------
    // parseConfig(SimConfig) — explicit declarations → turn timing
    // -----------------------------------------------------------------------

    @Test
    void parseConfig_withLaneDeclarations_usesTurnTiming() {
        SimConfig config = new SimConfig();
        config.laneDeclarations = List.of(northStraightLane());

        InputReader.ParsedConfig cfg = InputReader.parseConfig(config);
        // TURN_TIMING is (1, 2, 1)
        PhaseTiming t = cfg.phaseTiming();
        assertEquals(2, t.maxGreenSteps(), "explicit laneDeclarations → turn timing maxGreen=2");
    }

    @Test
    void parseConfig_withLaneDeclarations_parsesMovements() {
        SimConfig config = new SimConfig();
        LaneConfigDTO north = northStraightLane();
        north.movements = List.of("STRAIGHT");
        config.laneDeclarations = List.of(north);

        InputReader.ParsedConfig cfg = InputReader.parseConfig(config);
        // All 4 roads should have a declaration (explicit + filled-in defaults)
        assertEquals(4, cfg.laneDeclarations().size());
    }

    @Test
    void parseConfig_withLaneDeclarations_missingRoadsGetDefault() {
        // Only declare north; south, east, west should be auto-filled
        SimConfig config = new SimConfig();
        config.laneDeclarations = List.of(northStraightLane());

        InputReader.ParsedConfig cfg = InputReader.parseConfig(config);
        assertEquals(4, cfg.laneDeclarations().size(),
                "Undeclared roads should be filled in with default lanes");
    }

    // -----------------------------------------------------------------------
    // parseConfig(InputFile) → delegates to parseConfig(SimConfig)
    // -----------------------------------------------------------------------

    @Test
    void parseConfig_inputFileWithNullConfig_usesDefaults() {
        InputFile in = new InputFile();
        in.config = null;
        in.commands = List.of();

        InputReader.ParsedConfig cfg = InputReader.parseConfig(in);
        assertEquals(5, cfg.phaseTiming().maxGreenSteps(), "null config in InputFile → standard timing");
    }

    @Test
    void parseConfig_inputFileWithConfig_usesConfigTiming() {
        SimConfig config = new SimConfig();
        config.laneDeclarations = List.of(northStraightLane());

        InputFile in = new InputFile();
        in.config   = config;
        in.commands = List.of();

        InputReader.ParsedConfig cfg = InputReader.parseConfig(in);
        assertEquals(2, cfg.phaseTiming().maxGreenSteps(), "InputFile with config → turn timing");
    }

    @Test
    void parseConfig_inputFile_and_simConfig_overload_returnEquivalentResult() {
        SimConfig config = new SimConfig();
        config.laneDeclarations = null;

        InputFile in = new InputFile();
        in.config = config;
        in.commands = List.of();

        InputReader.ParsedConfig fromFile  = InputReader.parseConfig(in);
        InputReader.ParsedConfig fromConfig = InputReader.parseConfig(config);

        assertEquals(fromFile.phaseTiming().maxGreenSteps(),
                fromConfig.phaseTiming().maxGreenSteps(),
                "Both overloads must produce equivalent result for same input");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static LaneConfigDTO northStraightLane() {
        LaneConfigDTO dto = new LaneConfigDTO();
        dto.road      = "north";
        dto.lane      = 0;
        dto.movements = List.of("STRAIGHT");
        return dto;
    }
}
