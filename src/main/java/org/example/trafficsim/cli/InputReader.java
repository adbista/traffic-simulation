package org.example.trafficsim.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.LaneConfigDTO;
import org.example.trafficsim.io.SimConfig;
import org.example.trafficsim.model.LaneDeclaration;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.signal.PhaseTiming;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Reads the input JSON file and transforms the configuration into a ParsedConfig.
//
// Handles two configuration variants:
//   1. "laneDeclarations" — explicit lane declarations with movements or a lane type.
//      Phases are generated dynamically via PhaseSetGenerator. Timing: (1,2,1).
//   2. No configuration — 1 GENERAL lane (STRAIGHT+PERMISSIVE_LEFT+RIGHT) per road.
//      Produces standard NS and EW phases. Timing: (1,5,1).
public class InputReader {

    private static final PhaseTiming STANDARD_TIMING = new PhaseTiming(1, 5, 1);
    private static final PhaseTiming TURN_TIMING     = new PhaseTiming(1, 2, 1);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static InputFile read(File file) throws IOException {
        return MAPPER.readValue(file, InputFile.class);
    }

    public static ParsedConfig parseConfig(InputFile in) {
        return parseConfig(in.config);
    }

    public static ParsedConfig parseConfig(SimConfig config) {
        if (config != null && config.laneDeclarations != null) {
            List<LaneDeclaration> declarations = parseLaneDeclarations(config.laneDeclarations);
            return new ParsedConfig(handleLines(declarations), TURN_TIMING);
        }
        return new ParsedConfig(defaultLines(), STANDARD_TIMING);
    }

    private static List<LaneDeclaration> parseLaneDeclarations(List<LaneConfigDTO> dtos) {
        return dtos.stream().map(dto -> {
            Road road = Road.fromString(dto.road);
            Set<Movement> movements;
            if (dto.movements != null && !dto.movements.isEmpty()) {
                movements = dto.movements.stream()
                        .map(Movement::fromString)
                        .collect(Collectors.toUnmodifiableSet());
            } else {
                movements = Set.of(Movement.STRAIGHT, Movement.PERMISSIVE_LEFT, Movement.RIGHT);
            }
            return new LaneDeclaration(road, dto.lane, movements);
        }).toList();
    }

    // Fills in declarations: roads without explicit declarations receive 1 STRAIGHT lane.
    private static List<LaneDeclaration> handleLines(List<LaneDeclaration> explicit) {
        List<LaneDeclaration> result = new ArrayList<>(explicit);
        for (Road r : Road.values()) {
            boolean declared = explicit.stream().anyMatch(d -> d.road() == r);
            if (!declared) result.add(LaneDeclaration.defaultLane(r, 0));
        }
        return result;
    }

    // 1 STRAIGHT lane per road — default configuration when no declarations are provided.
    private static List<LaneDeclaration> defaultLines() {
        List<LaneDeclaration> lines = new ArrayList<>();
        for (Road r : Road.values()) lines.add(LaneDeclaration.defaultLane(r, 0));
        return lines;
    }

    public record ParsedConfig(List<LaneDeclaration> laneDeclarations, PhaseTiming phaseTiming) {}

    private InputReader() {}
}
