package org.example.trafficsim.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trafficsim.io.OutputFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for POST /v1/simulate.
 *
 * Covers:
 *  - All 8 data-driven scenarios (reading from src/test/resources/scenarios/)
 *  - Happy-path structural checks
 *  - Complete error-handler coverage: 400 (malformed JSON, @NotNull/@NotBlank violations,
 *    IllegalArgumentException), 405 Method Not Allowed
 *  - Response body shape for every error: must contain an {@code "error"} field
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SimulationServerTest {

    @Autowired TestRestTemplate rest;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // All 8 scenarios via REST — mirrors JsonScenarioTest but over HTTP
    // -----------------------------------------------------------------------

    static Stream<Arguments> scenarioPaths() throws Exception {
        URL url = SimulationServerTest.class.getClassLoader().getResource("scenarios");
        assertNotNull(url, "test/resources/scenarios not found on classpath");
        File[] dirs = new File(url.toURI()).listFiles(File::isDirectory);
        assertNotNull(dirs);
        return Arrays.stream(dirs)
                .sorted()
                .map(dir -> Arguments.of(dir.getName(), dir));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarioPaths")
    @DisplayName("REST scenario")
    void allScenarios_viaRest(String name, File scenarioDir) throws Exception {
        String inputJson   = Files.readString(new File(scenarioDir, "input.json").toPath());
        File   expectedFile = new File(scenarioDir, "expected-output.json");

        ResponseEntity<OutputFile> response = post(inputJson, OutputFile.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), name + ": unexpected status");

        OutputFile actual   = response.getBody();
        Expected   expected = MAPPER.readValue(expectedFile, Expected.class);
        assertNotNull(actual, name + ": null body");

        assertEquals(expected.stepStatuses.size(), actual.stepStatuses.size(),
                name + ": step count mismatch");

        for (int i = 0; i < expected.stepStatuses.size(); i++) {
            List<String> exp = expected.stepStatuses.get(i).leftVehicles;
            List<String> act = actual.stepStatuses.get(i).leftVehicles;
            assertEquals(exp, act, name + ": step " + (i + 1) + " leftVehicles mismatch");
        }
    }

    // -----------------------------------------------------------------------
    // Happy-path structural checks
    // -----------------------------------------------------------------------

    @Test
    void postSimulate_returnsCorrectStepResults() {
        // Mirrors scenario 01-recruitment-example
        String body = """
                {
                  "commands": [
                    {"type": "addVehicle", "vehicleId": "v1", "startRoad": "south", "endRoad": "north"},
                    {"type": "addVehicle", "vehicleId": "v2", "startRoad": "north", "endRoad": "south"},
                    {"type": "step"},
                    {"type": "step"},
                    {"type": "addVehicle", "vehicleId": "v3", "startRoad": "west", "endRoad": "south"},
                    {"type": "addVehicle", "vehicleId": "v4", "startRoad": "west", "endRoad": "south"},
                    {"type": "step"},
                    {"type": "step"}
                  ]
                }
                """;

        ResponseEntity<OutputFile> response = post(body, OutputFile.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getHeaders().getContentType())
                .isCompatibleWith(MediaType.APPLICATION_JSON));

        OutputFile out = response.getBody();
        assertNotNull(out);
        assertEquals(4, out.stepStatuses.size());
        assertVehicles(out, 0, List.of("v1", "v2"));
        assertVehicles(out, 1, List.of());
        assertVehicles(out, 2, List.of("v3"));
        assertVehicles(out, 3, List.of("v4"));
    }

    @Test
    void postSimulate_emptyCommandList_returnsEmptyStepList() {
        ResponseEntity<OutputFile> response = post("{\"commands\":[]}", OutputFile.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().stepStatuses.size());
    }

    @Test
    void postSimulate_stepsOnly_returnsEmptyLeftVehiclesEachStep() {
        ResponseEntity<OutputFile> response = post("""
                {"commands":[{"type":"step"},{"type":"step"}]}
                """, OutputFile.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        OutputFile out = response.getBody();
        assertNotNull(out);
        assertEquals(2, out.stepStatuses.size());
        out.stepStatuses.forEach(s -> assertTrue(s.leftVehicles.isEmpty()));
    }

    // -----------------------------------------------------------------------
    // Error handler: 405 Method Not Allowed
    // -----------------------------------------------------------------------

    @Test
    void getRequest_returns405() {
        ResponseEntity<String> r = rest.exchange("/v1/simulate", HttpMethod.GET, null, String.class);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, r.getStatusCode());
    }

    // -----------------------------------------------------------------------
    // Error handler: HttpMessageNotReadableException → 400 (malformed JSON)
    // -----------------------------------------------------------------------

    @Test
    void invalidJson_returns400_withErrorField() throws Exception {
        ResponseEntity<String> r = post("{ not valid json }", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertHasErrorField(r.getBody());
    }

    // -----------------------------------------------------------------------
    // Error handler: MethodArgumentNotValidException → 400 (Bean Validation)
    // -----------------------------------------------------------------------

    @Test
    void nullCommands_returns400_withErrorField() throws Exception {
        // commands field is missing entirely → @NotNull triggers
        ResponseEntity<String> r = post("{}", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertHasErrorField(r.getBody());
    }

    @Test
    void blankCommandType_returns400_withErrorField() throws Exception {
        // type is blank → @NotBlank on CommandDTO.type triggers
        ResponseEntity<String> r = post("""
                {"commands":[{"type":"  "}]}
                """, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertHasErrorField(r.getBody());
    }

    // -----------------------------------------------------------------------
    // Error handler: IllegalArgumentException → 400 (unknown command / bad road)
    // -----------------------------------------------------------------------

    @Test
    void unknownCommandType_returns400_withErrorField() throws Exception {
        ResponseEntity<String> r = post("""
                {"commands":[{"type":"unknownCommand"}]}
                """, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertHasErrorField(r.getBody());
    }

    @Test
    void invalidRoadName_returns400_withErrorField() throws Exception {
        ResponseEntity<String> r = post("""
                {"commands":[{"type":"addVehicle","vehicleId":"x","startRoad":"mars","endRoad":"north"}]}
                """, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertHasErrorField(r.getBody());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private <T> ResponseEntity<T> post(String jsonBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/v1/simulate", HttpMethod.POST,
                new HttpEntity<>(jsonBody, headers), responseType);
    }

    private static void assertVehicles(OutputFile out, int step, List<String> expected) {
        assertEquals(expected, out.stepStatuses.get(step).leftVehicles,
                "step " + (step + 1) + " leftVehicles mismatch");
    }

    private static void assertHasErrorField(String body) throws Exception {
        assertNotNull(body, "Response body must not be null on error");
        JsonNode node = MAPPER.readTree(body);
        assertTrue(node.has("error"), "Error response must contain 'error' field, got: " + body);
        assertFalse(node.get("error").asText().isBlank(), "'error' field must not be blank");
    }

    // -----------------------------------------------------------------------
    // Minimal DTOs for reading expected-output.json in the parameterized test
    // -----------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Expected {
        public List<ExpectedStep> stepStatuses;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExpectedStep {
        public List<String> leftVehicles = List.of();
    }
}

