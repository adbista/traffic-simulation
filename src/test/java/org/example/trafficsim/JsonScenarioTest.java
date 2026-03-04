package org.example.trafficsim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trafficsim.io.OutputFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// Data-driven scenario tests.
//
// Each subdirectory under src/test/resources/scenarios/ is one test case.
// The directory must contain:
//   input.json           - commands fed to the simulation
//   expected-output.json - expected result (only leftVehicles per step is required;
//                          fields like signalState, queueLengths are optional and ignored in assertions)
//
// Adding a new test = creating a new directory with those two files, no code changes needed.
class JsonScenarioTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // scenario directory discovery

    static Stream<Arguments> scenarioPaths() throws Exception {
        URL url = JsonScenarioTest.class.getClassLoader().getResource("scenarios");
        assertNotNull(url, "test/resources/scenarios not found on classpath");
        File scenariosDir = new File(url.toURI());
        File[] dirs = scenariosDir.listFiles(File::isDirectory);
        assertNotNull(dirs, "No subdirectories in scenarios/");
        return Arrays.stream(dirs)
                .sorted()
                .map(dir -> Arguments.of(dir.getName(), dir));
    }

    // parametric test execution

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarioPaths")
    @DisplayName("JSON scenario")
    void scenario(String name, File scenarioDir) throws Exception {
        File inputFile    = new File(scenarioDir, "input.json");
        File expectedFile = new File(scenarioDir, "expected-output.json");

        assertTrue(inputFile.exists(),    name + ": input.json not found");
        assertTrue(expectedFile.exists(), name + ": expected-output.json not found");

        Path actualOut = Files.createTempFile("scenario-" + name, ".json");
        try {
            org.example.trafficsim.app.Main.main(
                    new String[]{inputFile.getAbsolutePath(), actualOut.toAbsolutePath().toString()});

            OutputFile actual   = MAPPER.readValue(actualOut.toFile(), OutputFile.class);
            Expected expected = MAPPER.readValue(expectedFile, Expected.class);

            assertSteps(name, expected.stepStatuses, actual.stepStatuses);
        } finally {
            Files.deleteIfExists(actualOut);
        }
    }

    // Compares only leftVehicles (order matters — same as returned by step())
    private void assertSteps(String scenario,
                              List<ExpectedStep> expected,
                              List<OutputFile.StepStatus> actual) {
        assertEquals(expected.size(), actual.size(),
                scenario + ": wrong number of steps (expected=" + expected.size() +
                " actual=" + actual.size() + ")");

        for (int i = 0; i < expected.size(); i++) {
            List<String> exp = expected.get(i).leftVehicles;
            List<String> act = actual.get(i).leftVehicles;
            assertEquals(exp, act,
                    scenario + ": step " + (i + 1) +
                    " — expected " + exp + ", got " + act);
        }
    }

    // Minimal DTO for reading expected-output.json.
    // Ignores unknown fields (signalState, queueLengths, etc.)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Expected {
        public List<ExpectedStep> stepStatuses;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExpectedStep {
        public List<String> leftVehicles = List.of();
    }
}
