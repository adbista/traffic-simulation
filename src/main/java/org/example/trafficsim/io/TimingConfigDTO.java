package org.example.trafficsim.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimingConfigDTO {
    public int minGreen = 1;
    public int maxGreen = 5;
    public int yellow   = 0;
    public int red      = 1;
}
