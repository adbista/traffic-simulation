package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Format: {"movement": "LEFT", "type": "GENERIC", "trafficLightId": "t1"}
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovementDeclarationDTO {
    public String movement;
    public String type;
    public String trafficLightId;
}
