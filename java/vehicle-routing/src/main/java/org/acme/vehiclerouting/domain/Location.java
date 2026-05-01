package org.acme.vehiclerouting.domain;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GeoLocation.class, name = "GeoLocation"),
        @JsonSubTypes.Type(value = CartesianLocation.class, name = "CartesianLocation")
})
public abstract class Location {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private final long id = ID_COUNTER.incrementAndGet();
    private double latitude;
    private double longitude;

    @JsonIgnore
    private Map<Location, Long> drivingTimeSeconds;

    protected Location() {
    }

    protected Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Location fromArray(double[] coords) {
        if (coords.length != 2) {
            throw new IllegalArgumentException("Coordinates array must have exactly 2 elements");
        }
        return new GeoLocation(coords[0], coords[1]);
    }

    public double[] toArray() {
        return new double[]{latitude, longitude};
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Double.compare(location.latitude, latitude) == 0 && Double.compare(location.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Map<Location, Long> getDrivingTimeSeconds() {
        return drivingTimeSeconds;
    }

    public void setDrivingTimeSeconds(Map<Location, Long> drivingTimeSeconds) {
        this.drivingTimeSeconds = drivingTimeSeconds;
    }

    public long getDrivingTimeTo(Location location) {
        return drivingTimeSeconds.get(location);
    }

    @Override
    public String toString() {
        return latitude + "," + longitude;
    }
}