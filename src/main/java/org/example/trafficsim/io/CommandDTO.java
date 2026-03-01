package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandDTO {
    @NotBlank(message = "command type must not be blank")
    public String type;
    public String vehicleId;
    public String startRoad;
    public String endRoad;
    public Integer lane;
}
