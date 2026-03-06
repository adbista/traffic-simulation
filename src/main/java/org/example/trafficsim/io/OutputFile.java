package org.example.trafficsim.io;

import java.util.ArrayList;
import java.util.List;

public class OutputFile {
    public List<StepStatus> stepStatuses = new ArrayList<>();

    public static class StepStatus {
        public List<String> leftVehicles = new ArrayList<>();
        public String activePhaseId;
        public String phaseState;  // GREEN | YELLOW | RED

        public StepStatus() {}

        public StepStatus(List<String> leftVehicles) {
            this.leftVehicles = leftVehicles;
        }

        public StepStatus(List<String> leftVehicles, String activePhaseId, String phaseState) {
            this.leftVehicles = leftVehicles;
            this.activePhaseId = activePhaseId;
            this.phaseState = phaseState;
        }
    }
}
