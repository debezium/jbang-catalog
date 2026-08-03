/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PkceTest {

    /**
     * The worked example from RFC 7636 Appendix B. Getting this wrong — usually by leaving base64
     * padding on — fails at the token exchange with a message that says nothing about padding.
     */
    @Test
    public void shouldMatchRfc7636TestVector() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        assertThat(Pkce.challengeFor(verifier)).isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    public void shouldProduceUnpaddedBase64Url() {
        String challenge = Pkce.generate().challenge();

        assertThat(challenge).doesNotContain("=").doesNotContain("+").doesNotContain("/");
    }

    @Test
    public void shouldGenerateVerifierWithinLengthLimits() {
        String verifier = Pkce.generate().verifier();

        assertThat(verifier.length()).isBetween(43, 128);
        assertThat(verifier).matches("[A-Za-z0-9\\-._~]+");
    }

    @Test
    public void shouldGenerateADifferentVerifierEachTime() {
        assertThat(Pkce.generate().verifier()).isNotEqualTo(Pkce.generate().verifier());
    }

    @Test
    public void shouldKeepVerifierAndChallengeConsistent() {
        Pkce pkce = Pkce.generate();

        assertThat(pkce.challenge()).isEqualTo(Pkce.challengeFor(pkce.verifier()));
    }
}
