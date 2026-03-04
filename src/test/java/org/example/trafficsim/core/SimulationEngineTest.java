package org.example.trafficsim.core;

import org.example.trafficsim.config.IntersectionConfig;
import org.example.trafficsim.io.InputReader;
import org.example.trafficsim.io.SimConfig;
import org.example.trafficsim.model.Road;
import org.example.trafficsim.model.Vehicle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {

    private SimulationEngine defaultEngine() {
        IntersectionConfig config = InputReader.parseConfig((SimConfig) null);
        return new SimulationEngineBuilder(config).build();
    }

    @Test
    void step_emptyQueues_returnsNoVehicles() {
        StepResult result = defaultEngine().step();
        assertTrue(result.leftVehicles().isEmpty());
    }

    @Test
    void vehicle_departsWhenGreenPhaseIsActive() {
        SimulationEngine engine = defaultEngine();
        engine.addVehicle("v1", Road.NORTH, Road.SOUTH, 0);
        engine.addVehicle("v2", Road.SOUTH, Road.NORTH, 0);
        // Default first phase covers NORTH+SOUTH; both should depart within a few steps
        List<String> departed = new ArrayList<>();
        for (int i = 0; i < 5; i++) departed.addAll(engine.step().leftVehicles());
        assertTrue(departed.contains("v1"));
        assertTrue(departed.contains("v2"));
    }

    @Test
    void allVehiclesEventuallyLeave_multipleDirections() {
        SimulationEngine engine = defaultEngine();
        engine.addVehicle("n1", Road.NORTH, Road.SOUTH, 0);
        engine.addVehicle("s1", Road.SOUTH, Road.NORTH, 0);
        engine.addVehicle("e1", Road.EAST,  Road.WEST,  0);
        engine.addVehicle("w1", Road.WEST,  Road.EAST,  0);

        List<String> all = new ArrayList<>();
        for (int i = 0; i < 30; i++) all.addAll(engine.step().leftVehicles());

        assertTrue(all.containsAll(List.of("n1", "s1", "e1", "w1")));
    }

    @Test
    void sameLane_vehiclesDepartInArrivalOrder() {
        SimulationEngine engine = defaultEngine();
        engine.addVehicle("first",  Road.NORTH, Road.SOUTH, 0);
        engine.addVehicle("second", Road.NORTH, Road.SOUTH, 0);
        // Only the head of the queue departs each step
        List<String> step1 = engine.step().leftVehicles();
        assertFalse(step1.isEmpty());
        assertEquals("first", step1.get(0));
    }

    @Test
    void step_incrementsVehicleCount_acrossMultipleCalls() {
        SimulationEngine engine = defaultEngine();
        engine.addVehicle("a", Road.NORTH, Road.SOUTH, 0);
        engine.addVehicle("b", Road.NORTH, Road.SOUTH, 0);

        List<String> all = new ArrayList<>();
        for (int i = 0; i < 20; i++) all.addAll(engine.step().leftVehicles());

        assertTrue(all.contains("a"));
        assertTrue(all.contains("b"));
    }

    @Test
    void addVehicle_toInvalidLane_throws() {
        SimulationEngine engine = defaultEngine();
        assertThrows(IllegalArgumentException.class,
                () -> engine.addVehicle("v", Road.NORTH, Road.SOUTH, 99));
    }
}
