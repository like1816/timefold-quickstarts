package org.acme.vehiclerouting.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public class CartesianLocation extends Location {

    public CartesianLocation() {
    }

    public CartesianLocation(double x, double y) {
        super(x, y);
    }
}