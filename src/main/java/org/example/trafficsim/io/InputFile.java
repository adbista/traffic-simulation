package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InputFile {
    // for 01-recruitment-scenario config needs to be optional
    public SimConfig config;

    @NotNull(message = "commands must not be null")
    @Valid
    public List<CommandDTO> commands;
}