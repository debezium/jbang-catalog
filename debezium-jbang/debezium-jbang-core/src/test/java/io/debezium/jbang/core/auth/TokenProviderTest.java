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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Exercises the refresh path against an authorization server that rotates refresh tokens.
 * <p>
 * The server here behaves like a realm with rotation turned on: every refresh answers with a new
 * refresh token, and the previous one stops working at once. A client that forgets to save the
 * replacement works exactly once and is then logged out, which is the failure these tests pin.
 */
public class TokenProviderTest {

    private static final String PLATFORM = "https://api.example.com";

    @TempDir
    Path home;

    private String originalHome;
    private HttpServer server;
    private String issuer;

    /** The only refresh token the server accepts right now; replaced on every successful refresh. */
    private volatile String currentRefreshToken = "refresh-1";

    private final AtomicInteger generation = new AtomicInteger(1);

    /** Every refresh token the token endpoint was sent, in order. */
    private final List<String> presented = new ArrayList<>();

    @BeforeEach
    public void start() throws IOException {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        issuer = "http://127.0.0.1:" + server.getAddress().getPort();

        server.createContext("/.well-known/openid-configuration", exchange -> respond(exchange, 200,
                "{\"issuer\":\"" + issuer + "\","
                        + "\"authorization_endpoint\":\"" + issuer + "/auth\","
                        + "\"token_endpoint\":\"" + issuer + "/token\","
                        + "\"device_authorization_endpoint\":\"" + issuer + "/device\","
                        + "\"jwks_uri\":\"" + issuer + "/certs\","
                        + "\"response_types_supported\":[\"code\"],"
                        + "\"subject_types_supported\":[\"public\"],"
                        + "\"id_token_signing_alg_values_supported\":[\"RS256\"]}"));

        server.createContext("/token", exchange -> {
            String sent = formValue(exchange, "refresh_token");
            synchronized (presented) {
                presented.add(sent);
            }
            if (!currentRefreshToken.equals(sent)) {
                respond(exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"Token is not active\"}");
                return;
            }
            currentRefreshToken = "refresh-" + generation.incrementAndGet();
            respond(exchange, 200, "{\"access_token\":\"" + JwtFixtures.ACCESS_TOKEN + "\",\"token_type\":\"Bearer\","
                    + "\"refresh_token\":\"" + currentRefreshToken + "\",\"expires_in\":300}");
        });

        server.start();

        CredentialStore.save(PLATFORM, new StoredCredential(issuer, "dbz-cli", "refresh-1",
                "someone@example.com", "Offline", "2026-08-03T10:00:00Z"));
    }

    @AfterEach
    public void stop() {
        server.stop(0);
        TokenProvider.invalidate(PLATFORM);
        if (originalHome != null) {
            System.setProperty("user.home", originalHome);
        }
    }

    /**
     * Two refreshes in a row, each starting from what is on disk, the way two separate CLI commands
     * would. The second one only works if the first one saved the token it was handed.
     */
    @Test
    public void shouldSaveTheRotatedRefreshTokenSoTheNextRefreshWorks() {
        TokenProvider.refresh(PLATFORM, CredentialStore.find(PLATFORM));
        assertThat(CredentialStore.find(PLATFORM).refreshToken()).isEqualTo("refresh-2");

        TokenProvider.refresh(PLATFORM, CredentialStore.find(PLATFORM));
        assertThat(CredentialStore.find(PLATFORM).refreshToken()).isEqualTo("refresh-3");

        assertThat(presented).containsExactly("refresh-1", "refresh-2");
    }

    /**
     * {@code auth status --verify} followed by an ordinary command. The check refreshes the session,
     * so it must leave a usable credential behind rather than spend the stored one.
     */
    @Test
    public void shouldLeaveAUsableSessionAfterAVerifyingRefresh() {
        TokenProvider.refresh(PLATFORM, CredentialStore.find(PLATFORM));

        // A new command is a new process: nothing is cached, so the token comes from the stored session.
        TokenProvider.invalidate(PLATFORM);

        assertThat(TokenProvider.accessTokenOrNull(PLATFORM)).isEqualTo(JwtFixtures.ACCESS_TOKEN);
        assertThat(presented).containsExactly("refresh-1", "refresh-2");
    }

    @Test
    public void shouldKeepTheRestOfTheCredentialWhenTheTokenRotates() {
        TokenProvider.refresh(PLATFORM, CredentialStore.find(PLATFORM));

        StoredCredential stored = CredentialStore.find(PLATFORM);
        assertThat(stored.issuer()).isEqualTo(issuer);
        assertThat(stored.clientId()).isEqualTo("dbz-cli");
        assertThat(stored.subject()).isEqualTo("someone@example.com");
        assertThat(stored.refreshTokenType()).isEqualTo("Offline");
    }

    /** What the server does with a refresh token that was already used, so the tests above mean something. */
    @Test
    public void shouldReportAnAlreadyUsedRefreshTokenAsAnExpiredSession() {
        StoredCredential stale = CredentialStore.find(PLATFORM);
        TokenProvider.refresh(PLATFORM, stale);

        assertThatThrownBy(() -> TokenProvider.refresh(PLATFORM, stale))
                .isInstanceOf(CredentialExpiredException.class)
                .hasMessageContaining("Token is not active");
    }

    private static String formValue(HttpExchange exchange, String name) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        for (String pair : body.split("&")) {
            int split = pair.indexOf('=');
            if (split > 0 && name.equals(URLDecoder.decode(pair.substring(0, split), StandardCharsets.UTF_8))) {
                return URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
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
