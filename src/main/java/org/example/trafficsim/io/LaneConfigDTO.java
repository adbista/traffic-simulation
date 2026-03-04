package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;


// Format: { "road": "north", "lane": 0, "priority": "HIGH", "movements": [{"movement": "STRAIGHT", "type": "GENERIC", "trafficLightId": "t1"}, ...] }
@JsonIgnoreProperties(ignoreUnknown = true)
public class LaneConfigDTO {
    public String road;
    public int lane;
    /** Optional priority: HIGH (weight 4.0), MEDIUM (weight 2.0), LOW or absent (weight 1.0). */
    public String priority;
    /** Each entry declares one movement with its signal type and which traffic light it belongs to. */
    public List<MovementDeclarationDTO> movements;
}
