package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.data.LngLat;

import java.util.*;

import static uk.ac.ed.inf.ilp.constant.OrderStatus.DELIVERED;

// Define a class to manage paths for delivery drones, including avoiding no-fly zones and navigating to destinations
public class PathManager {
    // Declare variables for no-fly zones, central area, restaurants, and orders
    private final NamedRegion[] noFlyZones;
    private final NamedRegion centralArea;
    private final Restaurant[] restaurants;
    private final List<Order> orders;

    // A HashMap to store calculated paths for reuse
    private final HashMap<String, List<Movement>> paths = new HashMap<>();

    // Constructor to initialize the PathManager with necessary data
    public PathManager(NamedRegion[] noFlyZones, NamedRegion centralArea, Restaurant[] restaurants, List<Order> orders) {
        this.noFlyZones = noFlyZones;
        this.centralArea = centralArea;
        this.restaurants = restaurants;
        this.orders = orders;
    }

    // Method to find the location of the restaurant associated with a given order
    private LngLat restaurantLocation(Order order) {
        // Loop through each restaurant and check if it serves all pizzas in the order
        for (Restaurant restaurant : restaurants) {
            if (ValidateOrder.findNumberOfOrderListPizzasInMenu(
                    Arrays.stream(order.getPizzasInOrder()).toList(),
                    Arrays.stream(restaurant.menu()).toList()
            ) == order.getPizzasInOrder().length) {
                return restaurant.location(); // Return the location of the matching restaurant
            }
        }
        throw new IllegalArgumentException("The order cannot be associated with any available restaurant.");
    }

    // Method to determine the routes for all orders
    public List<Movement> determineRoutes() {

        List<Movement> routes = new ArrayList<>();
        // Calculate the path for each order and mark it as delivered
        for (Order order : orders) {
            routes.addAll(calculateTotalPath(restaurantLocation(order),
                    new LngLat(-3.186874, 55.944494), order.getOrderNo()));// The LngLat seems to be a fixed delivery point
            order.setOrderStatus(DELIVERED);
        }
        return routes; // Return the complete list of movements for all routes
    }

    // Method to calculate the total path for a single delivery from a source to a target
    public List<Movement> calculateTotalPath(LngLat sourceLocation, LngLat targetLocation, String orderNumber) {
        // Create a unique key for the path to check if it's already calculated
        String key = "Key : " + sourceLocation.lng() + sourceLocation.lat() + targetLocation.lng() + targetLocation.lat();
        if (paths.containsKey(key)) {
            // If path is already calculated, return a copy with the current order number
            List<Movement> result = new ArrayList<>();
            for (Movement move : paths.get(key)) {
                result.add(new Movement(move.getStart(), move.getAngle(), move.getEnd(), orderNumber));
            }
            return result;
        } else {
            // If path is not calculated, determine a new path
            List<Movement> path = calculatePath(targetLocation, sourceLocation, orderNumber);
            ArrayList<Movement> moveList = new ArrayList<>();
            for (Movement move : path) {
                if (move.getAngle() == 999) {
                    continue; // Skip the moves with angle 999 (indicating arrival or invalid movement)
                }
                moveList.add(new Movement(move.getEnd(), (move.getAngle() + 180) % 360, move.getStart(), orderNumber));
            }
            Collections.reverse(moveList); // Reverse the order of movements for return trip
            moveList.add(new Movement(moveList.get(moveList.size() - 1).getEnd(), 999, moveList.get(moveList.size() - 1).getEnd(), orderNumber));
            path.addAll(moveList); // Add the return movements to the path
            paths.put(key, path); // Cache the path for future use
        }
        return paths.get(key); // Return the calculated or retrieved path
    }

    // Helper method to calculate a path from one location to another
    private List<Movement> calculatePath(LngLat location1, LngLat location2, String orderNumber) {
        // Initialize variables for path calculation
        List<LngLat> previousMovement = new ArrayList<>();
        LongitudeAndLatitudeHandler lngLatHandler = new LongitudeAndLatitudeHandler();
        LngLat currentPosition = location1;
        ArrayList<Movement> path = new ArrayList<>();
        double[] angles = new double[16]; // Array to hold possible movement angles
        for (int i = 0; i < 16; i++) {
            angles[i] = i * 22.5; // Populate angles by 22.5 degrees increments
        }
        double tempAngle = 0;
        double distance;

        // Continue calculating path until the drone is close to the target
        while (!lngLatHandler.isCloseTo(currentPosition, location2)) {
            double closestDistance = Double.MAX_VALUE;
            // Iterate through all possible angles to find the closest next position
            for (double angle : angles) {
                LngLat nextPosition = lngLatHandler.nextPosition(currentPosition, angle);
                // Ensure the drone doesn't retrace its steps and avoids no-fly zones
                if (!previousMovement.contains(new LngLat(nextPosition.lng(), nextPosition.lat()))) {
                    boolean isCurrentPositionInCentralArea = lngLatHandler.isInCentralArea(currentPosition, centralArea);
                    boolean isNextPositionInCentralArea = lngLatHandler.isInCentralArea(nextPosition, centralArea);
                    boolean isDroneInNoFlyZone = false;

                    for (NamedRegion noFlyZone : this.noFlyZones) {
                        if (lngLatHandler.isInRegion(nextPosition, noFlyZone)) {
                            isDroneInNoFlyZone = true; // Check if the next position is in any no-fly zone
                            break;
                        }
                    }
                    // Update path if next position is valid
                    if (!isDroneInNoFlyZone) {
                        if (isCurrentPositionInCentralArea || !isNextPositionInCentralArea) {
                            distance = lngLatHandler.distanceTo(nextPosition, location2);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                tempAngle = angle; // Update the angle leading to the closest position
                            }
                        }
                    }
                    previousMovement.add(new LngLat(nextPosition.lng(), nextPosition.lat())); // Remember this movement
                }
            }
            // Add the movement to the path and update the current position
            path.add(new Movement(currentPosition, tempAngle, lngLatHandler.nextPosition(currentPosition, tempAngle), orderNumber));
            currentPosition = lngLatHandler.nextPosition(currentPosition, tempAngle);
        }
        path.add(new Movement(currentPosition, 999, currentPosition, orderNumber));
        return path;// Return the calculated path
    }
}
