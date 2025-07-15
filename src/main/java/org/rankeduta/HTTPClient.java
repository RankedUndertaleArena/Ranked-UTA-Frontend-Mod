package org.rankeduta;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class HTTPClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String userAgent = "application/rankeduta";
    private static final String apiUrl = "https://test-api.nebcraft.vip";

    public static class URIBuilder {
        private final Map<String,String> queryParams = new HashMap<>();

        public URIBuilder addParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public URI build() {
            StringBuilder uriBuilder = new StringBuilder(apiUrl);
            if (!queryParams.isEmpty()) {
                uriBuilder.append("?");
                queryParams.forEach((key, value) -> uriBuilder.append(key).append("=").append(value).append("&"));
                uriBuilder.setLength(uriBuilder.length() - 1); // Remove the last '&'
            }
            return java.net.URI.create(uriBuilder.toString());
        }
    }

    public static HttpResponse<String> get(String url, String uri) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiUrl+url+uri))
                .header("User-Agent", userAgent)
                .GET()
                .build();
            return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            RankedUTA.LOGGER.error("HTTP GET request failed: {}", e.getMessage());
            return null;
        }
    }

    public static HttpResponse<String> get(String url, URI uri) {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl+url).resolve(uri))
                .header("User-Agent", userAgent)
                .GET()
                .build();
            return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            RankedUTA.LOGGER.error("HTTP GET request failed: {}", e.getMessage());
            return null;
        }
    }

    public static HttpResponse<String> get(String url, URIBuilder uriBuilder) {
            try {
                var request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl+url).resolve(uriBuilder.build()))
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
