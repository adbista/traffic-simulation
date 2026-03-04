package org.example.trafficsim.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trafficsim.io.OutputFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for POST /v1/simulate.
 *
 * Uses the full Spring application context so that the real simulation logic is exercised.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    @Autowired
    MockMvc mockMvc;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // happy path

    @Test
    @DisplayName("scenario-01: default config, 4 steps, expected vehicles leave in order")
    void scenario01DefaultConfig() throws Exception {
        // Corresponds to the recruitment-example scenario (no config block and  default lanes)
        String body = """
                {
                  "commands": [
                    {"type": "addVehicle", "vehicleId": "vehicle1", "startRoad": "south", "endRoad": "north"},
                    {"type": "addVehicle", "vehicleId": "vehicle2", "startRoad": "north", "endRoad": "south"},
                    {"type": "step"},
                    {"type": "step"},
                    {"type": "addVehicle", "vehicleId": "vehicle3", "startRoad": "west", "endRoad": "south"},
                    {"type": "addVehicle", "vehicleId": "vehicle4", "startRoad": "west", "endRoad": "south"},
                    {"type": "step"},
                    {"type": "step"}
                  ]
                }
                """;

        MvcResult result = mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        OutputFile output = MAPPER.readValue(result.getResponse().getContentAsString(), OutputFile.class);

        assertThat(output.stepStatuses).hasSize(4);
        assertThat(output.stepStatuses.get(0).leftVehicles).containsExactly("vehicle1", "vehicle2");
        assertThat(output.stepStatuses.get(1).leftVehicles).isEmpty();
        assertThat(output.stepStatuses.get(2).leftVehicles).containsExactly("vehicle3");
        assertThat(output.stepStatuses.get(3).leftVehicles).containsExactly("vehicle4");
    }

    @Test
    @DisplayName("with explicit laneDeclarations using new movements format")
    void simulateWithMovementsFormat() throws Exception {
        // Scenario with explicit config: NORTH straight, SOUTH straight — they share a phase
        String body = """
                {
                  "config": {
                    "laneDeclarations": [
                      {"road": "north", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "south", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "east",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "west",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]}
                    ]
                  },
                  "commands": [
                    {"type": "addVehicle", "vehicleId": "vN", "startRoad": "north", "endRoad": "south"},
                    {"type": "addVehicle", "vehicleId": "vS", "startRoad": "south", "endRoad": "north"},
                    {"type": "step"}
                  ]
                }
                """;

        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepStatuses").isArray())
                .andExpect(jsonPath("$.stepStatuses.length()").value(1));
    }

    @Test
    @DisplayName("PROTECTED left turn does not conflict with opposite straight in same phase")
    void simulateWithProtectedLeftInConfig() throws Exception {
        String body = """
                {
                  "config": {
                    "timing": {"minGreen": 1, "maxGreen": 2, "yellow": 0, "red": 1},
                    "laneDeclarations": [
                      {"road": "north", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "north", "lane": 1, "movements": [{"movement": "LEFT", "type": "PROTECTED", "trafficLightId": "t1"}]},
                      {"road": "south", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "east",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]},
                      {"road": "west",  "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "RIGHT", "type": "GENERIC", "trafficLightId": "t1"}, {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}]}
                    ]
                  },
                  "commands": [
                    {"type": "addVehicle", "vehicleId": "v1-north-prot-left", "startRoad": "north", "endRoad": "east", "lane": 1},
                    {"type": "addVehicle", "vehicleId": "v2-south-straight",   "startRoad": "south", "endRoad": "north", "lane": 0},
                    {"type": "step"},
                    {"type": "step"},
                    {"type": "step"},
                    {"type": "step"}
                  ]
                }
                """;

        MvcResult result = mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        OutputFile output = MAPPER.readValue(result.getResponse().getContentAsString(), OutputFile.class);

        // Both vehicles should depart across 4 steps (exact order determined by phase assignment)
        long departedCount = output.stepStatuses.stream()
                .flatMap(s -> s.leftVehicles.stream())
                .count();
        assertThat(departedCount).isEqualTo(2);
    }

    // ── validation / error cases ───────────────────────────────────────────────

    @Test
    @DisplayName("null commands field returns 400")
    void nullCommandsReturns400() throws Exception {
        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commands\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("missing commands field returns 400")
    void missingCommandsReturns400() throws Exception {
        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("malformed JSON returns 400")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("unknown road name in addVehicle returns 400")
    void unknownRoadNameReturns400() throws Exception {
        String body = """
                {
                  "commands": [
                    {"type": "addVehicle", "vehicleId": "v1", "startRoad": "nowhere", "endRoad": "north"}
                  ]
                }
                """;

        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("unknown command type returns 400")
    void unknownCommandTypeReturns400() throws Exception {
        String body = """
                {
                  "commands": [
                    {"type": "flyVehicle", "vehicleId": "v1"}
                  ]
                }
                """;

        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("blank type field in command returns 400 (Bean Validation)")
    void blankCommandTypeReturns400() throws Exception {
        String body = """
                {
                  "commands": [
                    {"type": "", "vehicleId": "v1"}
                  ]
                }
                """;

        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("unknown road name in laneDeclarations config returns 400")
    void unknownRoadInConfigReturns400() throws Exception {
        String body = """
                {
                  "config": {
                    "laneDeclarations": [
                      {"road": "diagonal", "lane": 0, "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}]}
                    ]
                  },
                  "commands": [{"type": "step"}]
                }
                """;

        mockMvc.perform(post("/v1/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
