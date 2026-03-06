package org.example.trafficsim.app;

import org.example.trafficsim.io.InputFile;
import org.example.trafficsim.io.InputReader;
import org.example.trafficsim.io.OutputFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

// Entry point. Reads the input JSON, runs the simulation, writes the output JSON.
// Usage:  ./gradlew run --args="input.json output.json"
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: <input.json> <output.json>");
            System.exit(2);
        }

        InputFile in = InputReader.read(new File(args[0]));
        OutputFile out = SimulationRunner.run(in);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(args[1]), out);
    }
}
