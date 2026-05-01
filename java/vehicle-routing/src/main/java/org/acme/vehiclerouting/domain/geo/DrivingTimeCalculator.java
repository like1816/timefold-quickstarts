package org.acme.vehiclerouting.domain.geo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.acme.vehiclerouting.domain.CartesianLocation;
import org.acme.vehiclerouting.domain.GeoLocation;
import org.acme.vehiclerouting.domain.Location;

public interface DrivingTimeCalculator {

    long calculateDrivingTime(Location from, Location to);

    default Map<Location, Map<Location, Long>> calculateBulkDrivingTime(
            Collection<Location> fromLocations,
            Collection<Location> toLocations) {
        List<Location> fromList = new ArrayList<>(fromLocations);
        List<Location> toList = new ArrayList<>(toLocations);

        Map<Location, Map<Location, Long>> result = new LinkedHashMap<>();
        for (Location from : fromList) {
            Map<Location, Long> innerMap = new LinkedHashMap<>();
            for (Location to : toList) {
                innerMap.put(to, calculateDrivingTime(from, to));
            }
            result.put(from, innerMap);
        }

        return result;
    }

    default void initDrivingTimeMaps(Collection<Location> locations) {
        Map<Location, Map<Location, Long>> drivingTimeMatrix = calculateBulkDrivingTime(locations, locations);
        locations.forEach(location -> location.setDrivingTimeSeconds(drivingTimeMatrix.get(location)));
    }

    static DrivingTimeCalculator getAppropriateCalculator(Collection<Location> locations) {
        if (locations.isEmpty()) {
            return HaversineDrivingTimeCalculator.getInstance();
        }

        Location firstLocation = locations.iterator().next();
        if (firstLocation instanceof CartesianLocation) {
            return EuclideanDrivingTimeCalculator.getInstance();
        } else if (firstLocation instanceof GeoLocation) {
            return HaversineDrivingTimeCalculator.getInstance();
        }

        boolean hasGeoLocation = locations.stream().anyMatch(l -> l instanceof GeoLocation);
        boolean hasCartesianLocation = locations.stream().anyMatch(l -> l instanceof CartesianLocation);

        if (hasGeoLocation && hasCartesianLocation) {
            throw new IllegalStateException("Mixed location types (GeoLocation and CartesianLocation) are not supported.");
        } else if (hasCartesianLocation) {
            return EuclideanDrivingTimeCalculator.getInstance();
        } else {
            return HaversineDrivingTimeCalculator.getInstance();
        }
    }
}