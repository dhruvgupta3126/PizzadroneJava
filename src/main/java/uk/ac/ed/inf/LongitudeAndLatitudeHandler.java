package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;

import static uk.ac.ed.inf.ilp.constant.SystemConstants.DRONE_IS_CLOSE_DISTANCE;
import static uk.ac.ed.inf.ilp.constant.SystemConstants.DRONE_MOVE_DISTANCE;

// Define a class to handle operations related to longitude and latitude, implementing the LngLatHandling interface
public class LongitudeAndLatitudeHandler implements LngLatHandling {
    // Constructor for the handler, currently empty as no initial setup is required
    public LongitudeAndLatitudeHandler() {
    }
    // Override the distanceTo method from the LngLatHandling interface
    @Override
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        // Calculate and return the Euclidean distance between two points
        return Math.sqrt(Math.pow(endPosition.lng() - startPosition.lng(), 2) + Math.pow(startPosition.lat() - endPosition.lat(), 2));
    }

    // Override the isCloseTo method from the LngLatHandling interface
    @Override
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        // Determine if the distance between two positions is less than the defined close distance for drones
        return distanceTo(startPosition, otherPosition) < DRONE_IS_CLOSE_DISTANCE;
    }

    // Override the isInRegion method from the LngLatHandling interface
    @Override
    public boolean isInRegion(LngLat position, NamedRegion region) {
        // Extract vertices of the region and check if the given position is inside the region
        LngLat[] vertices = region.vertices();
        int n = vertices.length;
        return LineUtilities.checkInside(vertices, n, position) == 1; // Return true if inside, false otherwise
    }

    // Override the nextPosition method from the LngLatHandling interface
    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        // Handle special angle cases and verify angle is within the valid range
        if (angle == 999) {
            return startPosition; // Return the same position if angle is 999
        } else if (0 > angle || angle > 360) {
            throw new IllegalArgumentException("Error: Angle must be in the range of 0 to 360 degrees. ");
        } else if (angle % 22.5 != 0) {
            throw new IllegalArgumentException("Error: Angle should be a multiple of 22.5 degrees. Check and correct the angle.");
        }

        // Calculate and return the new position after moving a DRONE_MOVE_DISTANCE in the specified angle from the start position
        return new LngLat(startPosition.lng() + DRONE_MOVE_DISTANCE * Math.cos(Math.toRadians(angle)), startPosition.lat() + DRONE_MOVE_DISTANCE * Math.sin(Math.toRadians(angle)));
    }
}
