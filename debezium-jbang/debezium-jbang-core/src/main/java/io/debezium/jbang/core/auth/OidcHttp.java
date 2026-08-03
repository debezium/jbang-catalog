/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Minimal JDK-native HTTP for the OAuth endpoints.
 * <p>
 * Deliberately not the Quarkus REST client: these calls go to the authorization server, not to the
 * platform API, so they must not pick up the bearer filter or the platform base URI.
 */
final class OidcHttp {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private OidcHttp() {
    }

    static JsonNode get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        return send(request, url);
    }

    /**
     * POSTs {@code application/x-www-form-urlencoded} and parses the body regardless of status.
     * <p>
     * The OAuth error taxonomy lives in the JSON {@code error} field, not in the status code —
     * {@code authorization_pending} and {@code access_denied} are both HTTP 400 — so the caller has
     * to see the body of a failed response, not an exception.
     */
    static JsonNode postForm(String url, Map<String, String> form) {
        StringJoiner body = new StringJoiner("&");
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (entry.getValue() != null) {
                body.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return send(request, url);
    }

    private static JsonNode send(HttpRequest request, String url) {
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            String payload = response.body();
            if (payload == null || payload.isBlank()) {
                throw new AuthException("Empty response (HTTP " + response.statusCode() + ") from " + url);
            }
            try {
                return MAPPER.readTree(payload);
            }
            catch (Exception e) {
                throw new AuthException("Non-JSON response (HTTP " + response.statusCode() + ") from " + url
                        + " — is this an OIDC issuer URL? First bytes: " + head(payload));
            }
        }
        catch (AuthException e) {
            throw e;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while calling " + url);
        }
        catch (Exception e) {
            throw new AuthException("Cannot reach " + url + " — " + e.getMessage());
        }
    }

    private static String head(String payload) {
        String trimmed = payload.strip();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120) + "…";
    }

    static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
