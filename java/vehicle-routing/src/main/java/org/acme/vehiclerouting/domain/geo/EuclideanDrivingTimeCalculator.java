package org.acme.vehiclerouting.domain.geo;

import org.acme.vehiclerouting.domain.Location;

/**
 * Calculates the driving time (in seconds) between two locations using Euclidean distance.
 * This is suitable for benchmark instances with virtual coordinates (like Solomon VRPTW instances).
 * For Solomon instances, Euclidean distance is directly used as travel time in minutes.
 */
public final class EuclideanDrivingTimeCalculator implements DrivingTimeCalculator {

    private static final EuclideanDrivingTimeCalculator INSTANCE = new EuclideanDrivingTimeCalculator();

    public static synchronized EuclideanDrivingTimeCalculator getInstance() {
        return INSTANCE;
    }

    private EuclideanDrivingTimeCalculator() {
    }

    @Override
    public long calculateDrivingTime(Location from, Location to) {
        if (from.equals(to)) {
            return 0L;
        }

        // Calculate Euclidean distance (straight line distance)
        // Solomon instances use coordinates in virtual space: X (latitude), Y (longitude)
        double dx = from.getLatitude() - to.getLatitude();   // X difference
        double dy = from.getLongitude() - to.getLongitude(); // Y difference
        double distance = Math.sqrt(dx * dx + dy * dy);

        // For Solomon VRPTW instances, Euclidean distance is directly used as travel time in minutes
        // (rounded to nearest integer as per standard practice)
        long travelTimeMinutes = Math.round(distance);

        // Convert minutes to seconds
        return travelTimeMinutes * 60L;
    }
}