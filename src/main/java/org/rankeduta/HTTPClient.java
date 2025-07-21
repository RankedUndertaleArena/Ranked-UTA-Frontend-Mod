package org.rankeduta;

import org.json.JSONObject;

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
    private static final String apiUrl = "http://192.168.195.209:8000";

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
            return URI.create(uriBuilder.toString());
        }
    }

    public static HttpResponse<String> sendRequest(String method, String url, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder().version(HttpClient.Version.HTTP_1_1);
            switch (method) {
                case "POST" ->
                    builder.uri(URI.create(apiUrl + url))
                        .header("User-Agent", userAgent)
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(body));
                case "GET" -> {
                    if (body == null) builder.uri(java.net.URI.create(apiUrl+url));
                    else builder.uri(java.net.URI.create(apiUrl+url+body));

                    builder.header("User-Agent", userAgent)
                        .GET();
                }
            }

            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            RankedUTA.LOGGER.error("HTTP {} request to {} failed: {}", method, url, e.getMessage());
            return null;
        }
    }

    public static HttpResponse<String> get(String url, String body) {
        return sendRequest("GET", url, body);
    }

    public static HttpResponse<String> post(String url, String body) {
        return sendRequest("POST", url, body);
    }

    public static JSONObject receivedResponse(HttpResponse<String> response) {
        if (response == null) return null;
        if (response.statusCode() >= 500) {
            RankedUTA.LOGGER.error("Backend Server Error: {} - {}", response.statusCode(), response.body());
            return null;
        }
        return new JSONObject(response.body());
    }
}
