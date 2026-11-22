/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class JwtTest {

    @Test
    public void shouldReadClaimsFromThePayload() {
        Jwt jwt = Jwt.parse(JwtFixtures.accessToken("someone@example.com", Instant.now().plusSeconds(300)));

        assertThat(jwt.claim("sub")).contains("3f9a-user");
        assertThat(jwt.claim("email")).contains("someone@example.com");
        assertThat(jwt.subjectLabel()).isEqualTo("someone@example.com");
    }

    @Test
    public void shouldReturnEmptyForAnAbsentClaim() {
        Jwt jwt = Jwt.parse(JwtFixtures.refreshToken("Offline"));

        assertThat(jwt.claim("email")).isEmpty();
        assertThat(jwt.expiresAt()).isEmpty();
    }

    /**
     * The distinction that decides whether a login lasts a month or half an hour: only an Offline
     * refresh token survives the realm's SSO idle timeout.
     */
    @Test
    public void shouldExposeTheRefreshTokenType() {
        assertThat(Jwt.parse(JwtFixtures.refreshToken("Offline")).type()).contains("Offline");
        assertThat(Jwt.parse(JwtFixtures.refreshToken("Refresh")).type()).contains("Refresh");
    }

    @Test
    public void shouldReadTheExpiry() {
        Instant expiry = Instant.now().plusSeconds(300);

        Jwt jwt = Jwt.parse(JwtFixtures.accessToken("someone@example.com", expiry));

        assertThat(jwt.expiresAt()).contains(Instant.ofEpochSecond(expiry.getEpochSecond()));
    }

    @Test
    public void shouldFallBackToUsernameThenSubject() {
        String noEmail = JwtFixtures.refreshToken("Offline");

        assertThat(Jwt.parse(noEmail).subjectLabel()).isEqualTo("3f9a-user");
    }

    @Test
    public void shouldRejectSomethingThatIsNotAJwt() {
        assertThatThrownBy(() -> Jwt.parse("not-a-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("not a JWT");
    }

    @Test
    public void shouldRejectAnEmptyToken() {
        assertThatThrownBy(() -> Jwt.parse(""))
                .isInstanceOf(AuthException.class);
    }
}
