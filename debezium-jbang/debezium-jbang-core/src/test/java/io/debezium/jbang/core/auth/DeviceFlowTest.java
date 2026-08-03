/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Exercises the device grant against a scripted authorization server.
 * <p>
 * The polling loop is the part of this flow that is easiest to get subtly wrong, and every mistake
 * looks the same from the outside — a client that hangs. These tests pin the behaviour that a live
 * Keycloak would only reveal intermittently: that a denial stops the loop instead of polling until
 * timeout, and that success stops it immediately, because a device code is single-use.
 */
public class DeviceFlowTest {

    private HttpServer server;
    private OidcEndpoints endpoints;

    /** Token endpoint responses, served in order. */
    private final ConcurrentLinkedQueue<String> tokenResponses = new ConcurrentLinkedQueue<>();

    /** Every form body the token endpoint received, for assertions about what was sent. */
    private final List<Map<String, String>> tokenRequests = new ArrayList<>();

    private volatile String deviceResponse = deviceAuthorization(0);

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/device", exchange -> respond(exchange, 200, deviceResponse));
        server.createContext("/token", exchange -> {
            synchronized (tokenRequests) {
                tokenRequests.add(readForm(exchange));
            }
            String next = tokenResponses.poll();
            if (next == null) {
                // The loop asked for one poll more than the test scripted. Answering with a
                // distinctive error makes that show up as a failed assertion rather than a hang.
                respond(exchange, 400, "{\"error\":\"unscripted_extra_poll\"}");
                return;
            }
            respond(exchange, next.contains("\"error\"") ? 400 : 200, next);
        });

        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        endpoints = new OidcEndpoints(base, base + "/device", base + "/token", base + "/logout");
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
    }

    // A device_code long enough to look real; expires_in high enough that these tests never race it.
    private static String deviceAuthorization(int interval) {
        return "{\"device_code\":\"dev-code-abc\",\"user_code\":\"WDJB-MJHT\","
                + "\"verification_uri\":\"https://auth.example.com/realms/dmp/device\","
                + "\"verification_uri_complete\":\"https://auth.example.com/realms/dmp/device?user_code=WDJB-MJHT\","
                + "\"interval\":" + interval + ",\"expires_in\":600}";
    }

    private static final String SUCCESS = "{\"access_token\":\"" + JwtFixtures.ACCESS_TOKEN
            + "\",\"refresh_token\":\"refresh-abc\",\"expires_in\":300,\"scope\":\"openid profile email offline_access\"}";

    private DeviceAuthorization authorize(Pkce pkce) {
        return DeviceFlow.authorize(endpoints, "dbz-cli", "openid profile email offline_access", pkce);
    }

    private TokenSet poll(DeviceAuthorization authorization, Pkce pkce) {
        return DeviceFlow.poll(endpoints, "dbz-cli", authorization, pkce, message -> {
        });
    }

    @Test
    public void shouldSendPkceChallengeOnTheDeviceRequest() throws IOException {
        List<Map<String, String>> deviceRequests = new ArrayList<>();
        server.removeContext("/device");
        server.createContext("/device", exchange -> {
            deviceRequests.add(readForm(exchange));
            respond(exchange, 200, deviceResponse);
        });

        Pkce pkce = Pkce.generate();
        authorize(pkce);

        assertThat(deviceRequests).hasSize(1);
        assertThat(deviceRequests.get(0))
                .containsEntry("client_id", "dbz-cli")
                .containsEntry("code_challenge", pkce.challenge())
                .containsEntry("code_challenge_method", "S256")
                .containsEntry("scope", "openid profile email offline_access");
    }

    @Test
    public void shouldReadTheDeviceAuthorizationResponse() {
        DeviceAuthorization authorization = authorize(Pkce.generate());

        assertThat(authorization.deviceCode()).isEqualTo("dev-code-abc");
        assertThat(authorization.userCode()).isEqualTo("WDJB-MJHT");
        assertThat(authorization.verificationUri()).isEqualTo("https://auth.example.com/realms/dmp/device");
        assertThat(authorization.verificationUriComplete()).contains("user_code=WDJB-MJHT");
        assertThat(authorization.expiresInSeconds()).isEqualTo(600);
    }

    @Test
    public void shouldKeepPollingWhileAuthorizationIsPending() {
        tokenResponses.add("{\"error\":\"authorization_pending\"}");
        tokenResponses.add("{\"error\":\"authorization_pending\"}");
        tokenResponses.add(SUCCESS);

        Pkce pkce = Pkce.generate();
        TokenSet tokens = poll(authorize(pkce), pkce);

        assertThat(tokens.accessToken()).isEqualTo(JwtFixtures.ACCESS_TOKEN);
        assertThat(tokens.refreshToken()).isEqualTo("refresh-abc");
        assertThat(tokenRequests).hasSize(3);
    }

    /** The device code is single-use: one more poll after success comes back as a failure. */
    @Test
    public void shouldStopImmediatelyOnSuccess() {
        tokenResponses.add(SUCCESS);
        tokenResponses.add("{\"error\":\"invalid_grant\"}");

        Pkce pkce = Pkce.generate();
        poll(authorize(pkce), pkce);

        assertThat(tokenRequests).hasSize(1);
    }

    /**
     * {@code access_denied} and {@code authorization_pending} are both HTTP 400. Branching on the
     * status instead of the JSON error field turns a denial into a loop that runs until timeout.
     */
    @Test
    public void shouldStopOnAccessDeniedRatherThanPolling() {
        tokenResponses.add("{\"error\":\"authorization_pending\"}");
        tokenResponses.add("{\"error\":\"access_denied\"}");

        Pkce pkce = Pkce.generate();
        DeviceAuthorization authorization = authorize(pkce);

        assertThatThrownBy(() -> poll(authorization, pkce))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("denied");

        assertThat(tokenRequests).hasSize(2);
    }

    @Test
    public void shouldStopOnExpiredTokenAndSayHowToRecover() {
        tokenResponses.add("{\"error\":\"expired_token\"}");

        Pkce pkce = Pkce.generate();
        DeviceAuthorization authorization = authorize(pkce);

        assertThatThrownBy(() -> poll(authorization, pkce))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("auth login");
    }

    @Test
    public void shouldFailLoudlyOnAnUnrecognisedError() {
        tokenResponses.add("{\"error\":\"invalid_client\",\"error_description\":\"Client not found\"}");

        Pkce pkce = Pkce.generate();
        DeviceAuthorization authorization = authorize(pkce);

        assertThatThrownBy(() -> poll(authorization, pkce))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("invalid_client")
                .hasMessageContaining("Client not found");
    }

    /** The verifier must accompany every poll — it is bound to the device code for this attempt. */
    @Test
    public void shouldSendTheVerifierAndDeviceCodeOnEveryPoll() {
        tokenResponses.add("{\"error\":\"authorization_pending\"}");
        tokenResponses.add(SUCCESS);

        Pkce pkce = Pkce.generate();
        poll(authorize(pkce), pkce);

        assertThat(tokenRequests).allSatisfy(form -> assertThat(form)
                .containsEntry("client_id", "dbz-cli")
                .containsEntry("grant_type", DeviceFlow.GRANT_TYPE)
                .containsEntry("device_code", "dev-code-abc")
                .containsEntry("code_verifier", pkce.verifier()));
    }

    /**
     * A {@code slow_down} must widen the interval. Timing it is the only way to observe that from
     * outside, so this asserts the loop actually waited the extra five seconds rather than ignoring
     * the signal — which is the standard way a device-flow client gets rate-limited.
     */
    @Test
    public void shouldBackOffAfterSlowDown() {
        tokenResponses.add("{\"error\":\"slow_down\"}");
        tokenResponses.add(SUCCESS);

        Pkce pkce = Pkce.generate();
        DeviceAuthorization authorization = authorize(pkce);

        long startedAt = System.nanoTime();
        poll(authorization, pkce);
        long elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000L;

        assertThat(tokenRequests).hasSize(2);
        assertThat(elapsedSeconds).isGreaterThanOrEqualTo(5L);
    }

    @Test
    public void shouldSurfaceADeviceAuthorizationError() {
        deviceResponse = "{\"error\":\"invalid_client\",\"error_description\":\"Client not enabled for device flow\"}";

        assertThatThrownBy(() -> authorize(Pkce.generate()))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Client not enabled for device flow");
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            int split = pair.indexOf('=');
            if (split > 0) {
                form.put(URLDecoder.decode(pair.substring(0, split), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }
}
