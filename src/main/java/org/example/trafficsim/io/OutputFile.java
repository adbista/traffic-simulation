package org.example.trafficsim.io;

import java.util.ArrayList;
import java.util.List;

public class OutputFile {
    public List<StepStatus> stepStatuses = new ArrayList<>();

    public static class StepStatus {
        public List<String> leftVehicles = new ArrayList<>();

        public StepStatus() {}

        public StepStatus(List<String> leftVehicles) {
            this.leftVehicles = leftVehicles;
        }
    }
}
