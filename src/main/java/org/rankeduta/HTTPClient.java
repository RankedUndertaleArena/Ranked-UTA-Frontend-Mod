package org.rankeduta;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse;

public class HTTPClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String userAgent = "application/rankeduta";
    private static final String apiUrl = "https://test-api.nebcraft.vip";

    public static HttpResponse<String> get(String url){
        try {
            var request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(apiUrl+url))
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();
            return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            RankedUTA.LOGGER.error("HTTP GET request failed: {}", e.getMessage());
            return null;
        }
    }

    public static HttpResponse<String> post(String url, String body) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(apiUrl+url))
                    .header("User-Agent", userAgent)
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(body))
                    .build();
            return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            RankedUTA.LOGGER.error("HTTP POST request failed: {}", e.getMessage());
            return null;
        }
    }
}
