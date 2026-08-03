/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;

/**
 * A PKCE verifier/challenge pair, bound to a single login attempt.
 * <p>
 * The verifier, its challenge and the {@code device_code} form one triple with the lifetime of one
 * login: the challenge goes up with the device authorization request and the verifier comes back on
 * every token poll. Regenerating any part of it mid-flow fails as {@code Code not valid}.
 * <p>
 * Generation over the RFC 7636 unreserved alphabet, and the unpadded BASE64URL(SHA256(verifier))
 * challenge encoding, both come from the OAuth SDK.
 */
public final class Pkce {

    private final CodeVerifier verifier;
    private final CodeChallenge challenge;

    private Pkce(CodeVerifier verifier) {
        this.verifier = verifier;
        this.challenge = CodeChallenge.compute(CodeChallengeMethod.S256, verifier);
    }

    public static Pkce generate() {
        return new Pkce(new CodeVerifier());
    }

    static String challengeFor(String verifier) {
        return CodeChallenge.compute(CodeChallengeMethod.S256, new CodeVerifier(verifier)).getValue();
    }

    public String verifier() {
        return verifier.getValue();
    }

    public String challenge() {
        return challenge.getValue();
    }
}
