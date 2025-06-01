package uk.ac.ed.inf;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.io.FileWriter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Capture the start time for performance metrics
        long StartTime = System.currentTimeMillis();
        // Verify that exactly two arguments are passed to the program, else throw an exception

        if (args.length != 2) {
            throw new IllegalArgumentException("Argument count error: The argument count is not as expected.");
        }
        // Capture the date and URL from the arguments passed
        String date = args[0];
        String URL = args[1];

        // Print the entered date and URL to the console for verification
        System.out.println("Entered Date : " + date);
        System.out.println("Entered URL : " + URL + "\n");

        // Verify the date format and URL format, throwing exceptions if invalid
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Improper date format: Ensure the date is in the YYYY-MM-DD format.");
        } else if (!URL.matches("https://ilp-rest.azurewebsites.net")) {
            throw new IllegalArgumentException("Invalid URL: The URL should match https://ilp-rest.azurewebsites.net.");
        }
        // Check if the service at the URL is alive, throwing an exception if not
        String isActive = API.checkIsAlive(URL);
        if (!isActive.equals("true")) {
            throw new IllegalArgumentException("Unresponsive service: The service is not alive and responsive.");
        }

        System.out.println("Collecting data from the REST service... ");
        // Fetch the necessary data from the REST service using the API class
        Restaurant[] restaurants = API.getRestaurants(URL);
        Order[] orders = API.getOrders(URL, date);
        NamedRegion[] noFlyZones = API.getNoFlyZones(URL);
        NamedRegion centralArea = API.getCentralArea(URL);

        System.out.println("Data has been successfully obtained from the service.\n");
        // Validate the orders and prepare a list of valid orders
        ValidateOrder orderValidator = new ValidateOrder();
        List<Order> validOrderList = new ArrayList<>();
        for (Order order : orders) {
            if (orderValidator.validateOrder(order, restaurants).getOrderStatus() != OrderStatus.INVALID) {
                validOrderList.add(order);
            }
        }

        System.out.println("Evaluating navigation paths for delivery drones...");

        // Determine routes for delivery drones using path management
        PathManager pathManager = new PathManager(noFlyZones, centralArea, restaurants, validOrderList);
        List<Movement> paths = pathManager.determineRoutes();

        // Extract year, month, and day from the date for file naming
        String year = date.substring(0, 4);
        String month = date.substring(5, 7);
        String day = date.substring(8, 10);

        // Create a directory for result files if it does not exist
        new File("resultfiles").mkdirs();

        // Generate filenames for delivery, flightpath, and drone data and write the data to these files
        String deliveryFileName = "deliveries-" + year + "-" + month + "-" + day + ".json";
        try (FileWriter fileWriter = new FileWriter("resultfiles/" + deliveryFileName)) {
            fileWriter.write(deliveryJson(orders));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Created " + deliveryFileName);

        String flightpathFileName = "flightpath-" + year + "-" + month + "-" + day + ".json";
        try (FileWriter fileWriter = new FileWriter("resultfiles/" + flightpathFileName)) {
            fileWriter.write(flightpathJson(paths));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Created " + flightpathFileName);

        String droneFileName = "drone-" + year + "-" + month + "-" + day + ".geojson";
        try (FileWriter fileWriter = new FileWriter("resultfiles/" + droneFileName)) {
            fileWriter.write(geoJson(paths));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Created " + droneFileName + "\n");

        // Calculate and print the elapsed time of the program execution
        long EndTime = System.currentTimeMillis();
        System.out.println("Time Elapsed : " + ((double) (EndTime - StartTime) / 1000) + " seconds");
    }

    // Helper method to convert orders to a JSON string for delivery data
    public static String deliveryJson(Order[] orders) {
        JSONArray all_deliveries = new JSONArray();
        for (Order order : orders) {
            JSONObject delivery = new JSONObject();
            delivery.put("orderNo", order.getOrderNo());
            delivery.put("orderStatus", order.getOrderStatus());
            delivery.put("orderValidationCode", order.getOrderValidationCode());
            delivery.put("costInPence", order.getPriceTotalInPence());
            all_deliveries.put(delivery);
        }
        return all_deliveries.toString();
    }

    // Helper method to convert movements to a JSON string for flight path data
    public static String flightpathJson(List<Movement> moves) {
        JSONArray flightPath = new JSONArray();

        for (Movement move : moves) {
            JSONObject path = new JSONObject();
            path.put("orderNo", move.getOrderNumber());
            path.put("fromLongitude", move.getStart().lng());
            path.put("fromLatitude", move.getStart().lat());
            path.put("angle", move.getAngle());
            path.put("toLongitude", move.getEnd().lng());
            path.put("toLatitude", move.getEnd().lat());
            flightPath.put(path);
        }
        return flightPath.toString();
    }

    // Helper method to convert movements to a GeoJSON string for drone path visualization
    public static String geoJson(List<Movement> moves) {
        JsonObject featureCollection = new JsonObject();
        featureCollection.addProperty("type", "FeatureCollection");
        JsonArray features = new JsonArray();
        JsonObject feature = new JsonObject();
        feature.addProperty("type", "Feature");
        feature.addProperty("properties", "NULL");
        JsonObject geometry = new JsonObject();
        geometry.addProperty("type", "LineString");
        JsonArray coordinates = new JsonArray();

        for (Movement move : moves) {
            JsonArray longitudeLatitude = new JsonArray();
            longitudeLatitude.add(move.getStart().lng());
            longitudeLatitude.add(move.getStart().lat());
            coordinates.add(longitudeLatitude);
        }

        geometry.add("coordinates", coordinates);
        feature.add("geometry", geometry);
        features.add(feature);
        featureCollection.add("features", features);

        return featureCollection.toString();
    }
}
