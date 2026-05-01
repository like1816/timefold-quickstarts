package org.acme.vehiclerouting.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.acme.vehiclerouting.domain.CartesianLocation;
import org.acme.vehiclerouting.domain.Location;
import org.acme.vehiclerouting.domain.Vehicle;
import org.acme.vehiclerouting.domain.VehicleRoutePlan;
import org.acme.vehiclerouting.domain.Visit;
import org.acme.vehiclerouting.domain.geo.DrivingTimeCalculator;

/**
 * Parses Solomon VRPTW benchmark files (txt format) into a VehicleRoutePlan.
 */
public class SolomonParser {

    public static class SolomonInstance {
        public String name;
        public int vehicleCount;
        public int vehicleCapacity;
        public List<Customer> customers;
    }

    public static class Customer {
        public int id;
        public double x;
        public double y;
        public int demand;
        public int readyTime;
        public int dueDate;
        public int serviceTime;

        public boolean isDepot() {
            return id == 0;
        }
    }

    public static VehicleRoutePlan parse(String instanceName, InputStream inputStream) throws IOException {
        SolomonInstance instance = parseSolomonFile(inputStream);
        return buildVehicleRoutePlan(instanceName, instance);
    }

    private static SolomonInstance parseSolomonFile(InputStream inputStream) throws IOException {
        SolomonInstance instance = new SolomonInstance();
        instance.customers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                instance.name = line;
                break;
            }

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("VEHICLE")) break;
            }

            reader.readLine();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                instance.vehicleCount = Integer.parseInt(parts[0]);
                instance.vehicleCapacity = Integer.parseInt(parts[1]);
                break;
            }

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("CUSTOMER")) break;
            }

            reader.readLine();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 7) continue;

                Customer customer = new Customer();
                customer.id = Integer.parseInt(parts[0]);
                customer.x = Double.parseDouble(parts[1]);
                customer.y = Double.parseDouble(parts[2]);
                customer.demand = Integer.parseInt(parts[3]);
                customer.readyTime = Integer.parseInt(parts[4]);
                customer.dueDate = Integer.parseInt(parts[5]);
                customer.serviceTime = Integer.parseInt(parts[6]);
                instance.customers.add(customer);
            }
        }

        return instance;
    }

    private static VehicleRoutePlan buildVehicleRoutePlan(String instanceName, SolomonInstance instance) {
        LocalDate today = LocalDate.now();
        LocalDateTime baseDateTime = today.atTime(0, 0);

        Customer depotCustomer = instance.customers.stream()
                .filter(Customer::isDepot)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Depot (customer 0) not found"));

        CartesianLocation depotLocation = new CartesianLocation(depotCustomer.x, depotCustomer.y);

        List<Vehicle> vehicles = new ArrayList<>(instance.vehicleCount);
        LocalDateTime departureTime = baseDateTime.plusMinutes(depotCustomer.readyTime);
        for (int i = 0; i < instance.vehicleCount; i++) {
            Vehicle vehicle = new Vehicle(
                    "v" + (i + 1),
                    instance.vehicleCapacity,
                    depotLocation,
                    departureTime
            );
            vehicles.add(vehicle);
        }

        List<Visit> visits = instance.customers.stream()
                .filter(c -> !c.isDepot())
                .map(customer -> new Visit(
                        String.valueOf(customer.id),
                        "Customer " + customer.id,
                        new CartesianLocation(customer.x, customer.y),
                        customer.demand,
                        baseDateTime.plusMinutes(customer.readyTime),
                        baseDateTime.plusMinutes(customer.dueDate).plusMinutes(customer.serviceTime),
                        Duration.ofMinutes(customer.serviceTime)
                ))
                .collect(Collectors.toList());

        List<Location> allLocations = Stream.concat(
                vehicles.stream().map(Vehicle::getHomeLocation),
                visits.stream().map(Visit::getLocation)
        ).collect(Collectors.toList());

        double minLat = allLocations.stream().mapToDouble(Location::getLatitude).min().getAsDouble();
        double maxLat = allLocations.stream().mapToDouble(Location::getLatitude).max().getAsDouble();
        double minLon = allLocations.stream().mapToDouble(Location::getLongitude).min().getAsDouble();
        double maxLon = allLocations.stream().mapToDouble(Location::getLongitude).max().getAsDouble();

        CartesianLocation southWest = new CartesianLocation(minLat, minLon);
        CartesianLocation northEast = new CartesianLocation(maxLat, maxLon);

        LocalDateTime startDateTime = baseDateTime.plusMinutes(depotCustomer.readyTime);
        LocalDateTime endDateTime = baseDateTime.plusMinutes(
                instance.customers.stream().mapToInt(c -> c.dueDate).max().getAsInt()
        );

        VehicleRoutePlan plan = new VehicleRoutePlan(
                instanceName,
                southWest,
                northEast,
                startDateTime,
                endDateTime,
                vehicles,
                visits
        );

        DrivingTimeCalculator.getAppropriateCalculator(allLocations).initDrivingTimeMaps(allLocations);

        return plan;
    }
}
