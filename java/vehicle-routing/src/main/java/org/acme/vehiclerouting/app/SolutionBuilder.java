package org.acme.vehiclerouting.app;

import java.io.InputStream;

import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.util.SolomonParser;

public class SolutionBuilder {

    public static VehicleRoutePlan buildFromSolomon(String instanceName) {
        String resourcePath = "/input/problems/" + instanceName + ".txt";
        InputStream inputStream = SolutionBuilder.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalArgumentException("Cannot find instance file: " + resourcePath);
        }

        try {
            return SolomonParser.parse(instanceName, inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Solomon instance: " + instanceName, e);
        }
    }

    public static VehicleRoutePlan buildFromSolomonFile(String instanceName, InputStream inputStream) {
        try {
            return SolomonParser.parse(instanceName, inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Solomon instance: " + instanceName, e);
        }
    }
}