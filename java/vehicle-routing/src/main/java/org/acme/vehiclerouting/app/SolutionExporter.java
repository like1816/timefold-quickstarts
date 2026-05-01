package org.acme.vehiclerouting.app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;

public class SolutionExporter {

    private static final String AUTHORS = "Chris Lee";

    public static String export(VehicleRoutePlan solution) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String date = String.format("%02d.%02d.%04d %02d:%02d:%02d",
                today.getDayOfMonth(), today.getMonthValue(), today.getYear(),
                now.getHour(), now.getMinute(), now.getSecond());

        writer.println("Instance name : " + solution.getName());
        writer.println("Authors       : " + AUTHORS);
        writer.println("Date          : " + date);
        writer.println("Solution");

        List<Vehicle> vehiclesWithVisits = solution.getVehicles().stream()
                .filter(v -> !v.getVisits().isEmpty())
                .toList();

        for (int routeIndex = 0; routeIndex < vehiclesWithVisits.size(); routeIndex++) {
            Vehicle vehicle = vehiclesWithVisits.get(routeIndex);
            List<Visit> visits = vehicle.getVisits();

            writer.print("Route  " + (routeIndex + 1) + " :");
            for (Visit visit : visits) {
                writer.print(" " + visit.getId());
            }
            writer.println();
        }

        writer.flush();
        return stringWriter.toString();
    }

    public static void exportToFile(VehicleRoutePlan solution, String filePath) {
        String content = export(solution);
        try (java.io.FileWriter fileWriter = new java.io.FileWriter(filePath)) {
            fileWriter.write(content);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write solution to file: " + filePath, e);
        }
    }

    public static void print(VehicleRoutePlan solution) {
        System.out.println(export(solution));
    }
}