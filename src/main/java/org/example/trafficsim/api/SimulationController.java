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
