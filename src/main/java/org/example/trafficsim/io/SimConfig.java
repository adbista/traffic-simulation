package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Opcjonalny blok konfiguracyjny na poczatku pliku wejsciowego.
//
// Format z deklaracjami pasow:
//   { "config": { "laneDeclarations": [
//       { "road": "north", "lane": 0, "movements": ["STRAIGHT", "RIGHT"] },
//       { "road": "north", "lane": 1, "movements": ["LEFT"] }
//   ] } }
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimConfig {

    // Jawne deklaracje pasow z ruchami. Jesli brak, uzywany jest domyslny 1 pas
    public List<LaneConfigDTO> laneDeclarations;
}
