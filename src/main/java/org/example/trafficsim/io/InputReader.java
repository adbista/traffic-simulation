package org.example.trafficsim.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.core.LaneRegistry;
import org.example.trafficsim.model.Movement;
import org.example.trafficsim.model.MovementMask;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.SignalGroupSpec;
import org.example.trafficsim.model.TrafficLightType;
import org.example.trafficsim.model.PhaseTiming;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.example.trafficsim.config.SimDefaults.DEFAULT_MOVEMENTS;
import static org.example.trafficsim.config.SimDefaults.DEFAULT_TIMING;

public final class InputReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InputReader() {}

    public static InputFile read(File file) throws IOException {
        return MAPPER.readValue(file, InputFile.class);
    }

    public static IntersectionConfig parseConfig(InputFile in) {
        if (in == null) {
            throw new IllegalArgumentException("Input file must not be null");
        }
        return parseConfig(in.config);
    }

    public static IntersectionConfig parseConfig(SimConfig config) {
        PhaseTiming timing = parseTiming(config);

        int[] laneCounts = extractLaneCounts(config);
        LaneRegistry lines = buildLaneRegistry(laneCounts);

        Map<Integer, List<SignalGroupSpec>> movementSignalsByPosition =
                extractMovementSignals(config, lines);

        Map<Integer, Double> laneWeights = extractLaneWeights(config, lines);

        return new IntersectionConfig(
                timing,
                lines,
                movementSignalsByPosition,
                laneWeights
        );
    }

    private static PhaseTiming parseTiming(SimConfig config) {
        if (config != null && config.timing != null) {
            var t = config.timing;
            return new PhaseTiming(t.minGreen, t.maxGreen, t.yellow, t.red);
        }
        return DEFAULT_TIMING;
    }

    private static int[] extractLaneCounts(SimConfig config) {
        int[] laneCounts = new int[Road.values().length];
        Arrays.fill(laneCounts, 1);

        if (config == null || config.laneDeclarations == null) {
            return laneCounts;
        }

        for (LaneConfigDTO dto : config.laneDeclarations) {
            if (dto.lane < 0) {
                throw new IllegalArgumentException("Lane index cannot be negative: " + dto.lane);
            }

            Road road = Road.fromString(dto.road);
            laneCounts[road.ordinal()] = Math.max(laneCounts[road.ordinal()], dto.lane + 1);
        }

        return laneCounts;
    }

    private static LaneRegistry buildLaneRegistry(int[] laneCounts) {
        Road[] roads = Road.values();

        int[][] ids = new int[roads.length][];
        List<Road> roadById = new ArrayList<>();
        List<Integer> laneById = new ArrayList<>();

        int id = 0;
        for (Road road : roads) {
            int lanes = laneCounts[road.ordinal()];
            ids[road.ordinal()] = new int[lanes];

            for (int lane = 0; lane < lanes; lane++) {
                ids[road.ordinal()][lane] = id;
                roadById.add(road);
                laneById.add(lane);
                id++;
            }
        }

        return new LaneRegistry(ids, roadById, laneById);
    }

    private static Map<Integer, List<SignalGroupSpec>> extractMovementSignals(
            SimConfig config,
            LaneRegistry lines
    ) {
        if (config == null || config.laneDeclarations == null) {
            return Map.of();
        }

        Map<Integer, List<SignalGroupSpec>> result = new HashMap<>();

        for (LaneConfigDTO dto : config.laneDeclarations) {
            Road road = Road.fromString(dto.road);
            int lane = dto.lane;
            int positionId = lines.positionId(road, lane);

            List<SignalGroupSpec> signals = parseMovementSignals(dto);

            result.put(positionId, signals);
        }

        return result;
    }

    private static List<SignalGroupSpec> parseMovementSignals(LaneConfigDTO dto) {
        if (dto.movements == null || dto.movements.isEmpty()) {
            int mask = 0;
            for (Movement m : DEFAULT_MOVEMENTS) {
                mask |= MovementMask.bit(m);
            }
            return List.of(new SignalGroupSpec(mask, TrafficLightType.GENERIC));
        }

        // Group by trafficLightId, preserving insertion order
        Map<String, Integer> maskByGroup = new LinkedHashMap<>();
        Map<String, TrafficLightType> typeByGroup = new LinkedHashMap<>();

        for (MovementDeclarationDTO entry : dto.movements) {
            if (entry.movement == null || entry.movement.isBlank()) {
                throw new IllegalArgumentException(
                        "Movement name must not be blank on lane " + dto.road + ":" + dto.lane);
            }
            if (entry.trafficLightId == null || entry.trafficLightId.isBlank()) {
                throw new IllegalArgumentException(
                        "trafficLightId must not be blank on lane " + dto.road + ":" + dto.lane);
            }
            Movement movement = Movement.fromString(entry.movement);
            TrafficLightType type = (entry.type == null)
                    ? TrafficLightType.GENERIC
                    : TrafficLightType.valueOf(entry.type.trim().toUpperCase());

            maskByGroup.merge(entry.trafficLightId, MovementMask.bit(movement), (a, b) -> a | b);
            typeByGroup.putIfAbsent(entry.trafficLightId, type);
        }

        List<SignalGroupSpec> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : maskByGroup.entrySet()) {
            result.add(new SignalGroupSpec(e.getValue(), typeByGroup.get(e.getKey())));
        }
        return result;
    }

    private static Map<Integer, Double> extractLaneWeights(SimConfig config, LaneRegistry lines) {
        Map<Integer, Double> weights = new HashMap<>();
        int total = lines.totalPositions();
        for (int i = 0; i < total; i++) {
            weights.put(i, 1.0);
        }
        if (config == null || config.laneDeclarations == null) {
            return weights;
        }
        for (LaneConfigDTO dto : config.laneDeclarations) {
            if (dto.priority == null || dto.priority.isBlank()) {
                continue;
            }
            Road road = Road.fromString(dto.road);
            int positionId = lines.positionId(road, dto.lane);
            double weight = switch (dto.priority.trim().toUpperCase()) {
                case "HIGH"   -> 4.0;
                case "MEDIUM" -> 2.0;
                case "LOW"    -> 1.0;
                default -> throw new IllegalArgumentException(
                        "Unknown priority '" + dto.priority + "' on lane " + dto.road + ":" + dto.lane
                        + ". Expected HIGH, MEDIUM or LOW.");
            };
            weights.put(positionId, weight);
        }
        return weights;
    }

}