package org.rankeduta.service;

import org.json.JSONObject;
import org.rankeduta.RankedUTA;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BackendService {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String userAgent = "application/rankeduta";
    private static final String API_URL = RankedUTA.apiUrl;

    public static HttpResponse<String> sendRequest(String method, String url, String arg) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .header("User-Agent", userAgent);
            switch (method.toUpperCase()) {
                case "POST" -> builder.uri(URI.create(API_URL + url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(arg));
                case "GET" -> {
                    if (arg == null || arg.isEmpty())  builder.uri(URI.create(API_URL + url));
                    else  builder.uri(URI.create(API_URL + url + "?" + arg));

                    builder.GET();
                }
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            RankedUTA.LOGGER.error("HTTP {} request failed: {}", method, e.getMessage());
            return null;
        }
    }

    public static JSONObject receivedResponse(HttpResponse<String> response) {
        if (response == null) {
            RankedUTA.LOGGER.error("Received null response");
            return null;
        }

        if (response.statusCode() >= 500) {
            RankedUTA.LOGGER.error("Backend Server Error: {} - {}", response.statusCode(), response.body());
            return null;
        }

        try {
            return new JSONObject(response.body());
        } catch (Exception e) {
            RankedUTA.LOGGER.error("Failed to parse JSON response: {}", e.getMessage());
            return null;
        }
    }
}
