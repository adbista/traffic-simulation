package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// DTO do deserializacji deklaracji pasa z JSON.
//
// Format: { "road": "north", "lane": 0, "movements": ["STRAIGHT", "RIGHT"] }
//
// Dostepne ruchy: STRAIGHT, RIGHT, LEFT, PERMISSIVE_LEFT
// Jesli movements nie podano, uzyty jest domyslny zestaw GENERAL
// (STRAIGHT + PERMISSIVE_LEFT + RIGHT).
@JsonIgnoreProperties(ignoreUnknown = true)
public class LaneConfigDTO {
    public String road;
    public int lane;
    public List<String> movements;
}
