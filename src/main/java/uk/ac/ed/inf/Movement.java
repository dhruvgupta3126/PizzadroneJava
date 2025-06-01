package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

public final class Movement {
    private final LngLat start;
    private final LngLat end;
    private double angle;
    private String orderNumber;

    public Movement(LngLat start, double angle, LngLat end, String orderNumber) {
        this.start = start;
        this.angle = angle;
        this.end = end;
        this.orderNumber = orderNumber;
    }

    public LngLat getStart() {
        return start;
    }

    public double getAngle() {
        return angle;
    }

    public LngLat getEnd() {
        return end;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }
}
