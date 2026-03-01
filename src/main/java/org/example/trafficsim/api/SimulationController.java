package org.example.trafficsim.api;

import jakarta.validation.Valid;
import org.example.trafficsim.cli.SimulationRunner;
import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.OutputFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for the traffic simulation.
 *
 * POST /v1/simulate
 *   Request  — same JSON format as input.json (commands list, optional config block)
 *   Response — same JSON format as expected-output.json (stepStatuses list)
 *
 * HTTP status codes:
 *   200 OK          — simulation completed successfully
 *   400 Bad Request — invalid JSON, constraint violations, or unknown/malformed command
 *   405 Method Not Allowed — any HTTP method other than POST
 */
@RestController
@RequestMapping("/v1")
public class SimulationController {

    @PostMapping(
            value = "/simulate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OutputFile> simulate(@Valid @RequestBody InputFile request) {
        OutputFile output = SimulationRunner.run(request);
        return ResponseEntity.ok(output);
    }
}
