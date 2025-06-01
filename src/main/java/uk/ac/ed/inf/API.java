package uk.ac.ed.inf;

import java.io.IOException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Restaurant;

public class API {
    // Create a static HttpClient instance to send requests
    private static final HttpClient MyHttpClient = HttpClient.newHttpClient();
    // Create a static ObjectMapper instance to handle JSON
    private static final ObjectMapper MyObjectMapper = new ObjectMapper();

    // A private method to perform HTTP requests and return response as a string
    private static String triggerHttpRequest(String URL) throws IOException, InterruptedException {
        // Create and send an HTTP request to the given URL, getting the response
        HttpResponse<String> response = MyHttpClient.send(HttpRequest.newBuilder().uri(URI.create(URL)).build(), HttpResponse.BodyHandlers.ofString());
        // Check if response status code is 200 (OK), else throw an exception
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("Incorrect response code ( " + response.statusCode() + " ), for URL : " + URL);
        }
        // Return the body of the response
        return response.body();
    }
    // Public method to check if a server is alive by appending "/isAlive" to the URL and sending a request
    public static String checkIsAlive(String URL) throws IOException, InterruptedException {
        return triggerHttpRequest(URL + "/isAlive");
    }

    // Public method to get restaurants by appending "/restaurants" to the URL and sending a request
    // It returns an array of Restaurant objects
    public static Restaurant[] getRestaurants(String URL) throws IOException, InterruptedException {
        return MyObjectMapper.readValue(triggerHttpRequest(URL + "/restaurants"), Restaurant[].class);
    }

    // Public method to get orders by appending "/orders/" and a date to the URL and sending a request
    public static Order[] getOrders(String URL, String date) throws IOException, InterruptedException {
        MyObjectMapper.registerModule(new JavaTimeModule());
        return MyObjectMapper.readValue(triggerHttpRequest(URL + "/orders/" + date), Order[].class);
    }

    // Public method to get the central area by appending "/centralArea" to the URL and sending a request
    public static NamedRegion getCentralArea(String URL) throws IOException, InterruptedException {
        // Configure ObjectMapper to accept single values as arrays
        MyObjectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // Since it's expected to return an array of NamedRegion but only the first is needed, it's fetched and index 0 is returned
        return MyObjectMapper.readValue(triggerHttpRequest(URL + "/centralArea"), NamedRegion[].class)[0];
    }

    // Public method to get no fly zones by appending "/noFlyZones" to the URL and sending a request
    public static NamedRegion[] getNoFlyZones(String URL) throws IOException, InterruptedException {
        return MyObjectMapper.readValue(triggerHttpRequest(URL + "/noFlyZones"), NamedRegion[].class);
    }
}