package org.rankeduta.utils;

import java.net.URI;

public class URIBuilder {
    private final StringBuilder queryParams = new StringBuilder("?");

    public URIBuilder addParam(String key, String value) {
        if (queryParams.length() > 1)  queryParams.append("&");
        queryParams.append(key).append("=").append(value);
        return this;
    }

    public URI build() {
        return URI.create(queryParams.toString());
    }
}
