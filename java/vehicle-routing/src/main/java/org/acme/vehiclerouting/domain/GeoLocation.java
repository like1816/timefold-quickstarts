package org.acme.vehiclerouting.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public class GeoLocation extends Location {

    public GeoLocation() {
    }

    public GeoLocation(double latitude, double longitude) {
        super(latitude, longitude);
    }
}