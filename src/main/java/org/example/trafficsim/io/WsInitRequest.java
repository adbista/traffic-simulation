package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class WsInitRequest {
    // Optional lane / phase configuration.
    public SimConfig config;
}
