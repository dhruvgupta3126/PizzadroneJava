package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

// Define a class to perform various geometric calculations related to lines and points
public class LineUtilities {
    // Declare variables to hold two points defining a line
    public LngLat point1, point2;

    // Constructor to initialize a LineUtilities object with two points
    public LineUtilities(LngLat point1, LngLat point2) {
        this.point1 = point1;
        this.point2 = point2;
    }

    // Method to check if a given point is on the line segment defined by line1
    static int checkIsPointOnLine(LineUtilities line1, LngLat point) {
        // Determine the bounding box for the line segment
        double lngMin = Math.min(line1.point1.lng(), line1.point2.lng());
        double lngMax = Math.max(line1.point1.lng(), line1.point2.lng());
        double latMin = Math.min(line1.point1.lat(), line1.point2.lat());
        double latMax = Math.max(line1.point1.lat(), line1.point2.lat());
        // Check if the point lies within the bounding box of the line segment
        return (point.lng() <= lngMax && point.lng() >= lngMin && point.lat() <= latMax && point.lat() >= latMin) ? 1 : 0;
    }

    // Method to determine the orientation of the triplet (a, b, c)
    static int findDirection(LngLat a, LngLat b, LngLat c) {
        // Calculate the direction value
        double val = (b.lat() - a.lat()) * (c.lng() - b.lng()) - (b.lng() - a.lng()) * (c.lat() - b.lat());
        // Determine orientation based on the direction value
        return (val == 0) ? 0 : ((val < 0) ? 2 : 1);
    }

    // Method to check if two line segments (line1 and line2) intersect
    static int isIntersect(LineUtilities line1, LineUtilities line2) {
        // Determine the four orientations needed for the general and special cases
        int direction1 = findDirection(line1.point1, line1.point2, line2.point1);
        int direction2 = findDirection(line1.point1, line1.point2, line2.point2);
        int direction3 = findDirection(line2.point1, line2.point2, line1.point1);
        int direction4 = findDirection(line2.point1, line2.point2, line1.point2);

        // Check general case and special cases of intersection
        if ((direction1 != direction2 && direction3 != direction4)
                || (direction1 == 0 && checkIsPointOnLine(line1, line2.point1) == 1)
                || (direction2 == 0 && checkIsPointOnLine(line1, line2.point2) == 1)
                || (direction3 == 0 && checkIsPointOnLine(line2, line1.point1) == 1)
                || (direction4 == 0 && checkIsPointOnLine(line2, line1.point2) == 1)) {
            return 1; // Lines intersect
        } else {
            return 0; // Lines do not intersect
        }
    }

    // Method to check if a point is inside a polygon
    static int checkInside(LngLat[] polygon, int edges, LngLat position) {
        // There must be at least 3 edges in a polygon
        if (edges < 3) return 0;

        // Create a line from the point to a far away point (eastward)
        LineUtilities referenceLatitudeLine = new LineUtilities(position, new LngLat(999.99, position.lat()));
        int count = 0;
        int i = 0;
        do {
            // Create a line for each edge of the polygon
            LineUtilities side = new LineUtilities(polygon[i], polygon[(i + 1) % edges]);
            // If the point is on the edge, it's on the polygon
            if (isIntersect(side, referenceLatitudeLine) == 1 && findDirection(side.point1, position, side.point2) == 0)
                return checkIsPointOnLine(side, position);
            // Otherwise, count the number of times the line intersects with polygon edges
            count += (isIntersect(side, referenceLatitudeLine) == 1) ? 1 : 0;
            i = (i + 1) % edges;  // Move to the next vertex
        } while (i != 0);
        // If count is odd, the point is inside the polygon, otherwise it's outside
        return count & 1; // Return 1 for inside, 0 for outside
    }
}
