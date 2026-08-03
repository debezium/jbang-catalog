/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * A PKCE verifier/challenge pair, bound to a single login attempt.
 * <p>
 * The verifier, its challenge and the {@code device_code} form one triple with the lifetime of one
 * login: the challenge goes up with the device authorization request and the verifier comes back on
 * every token poll. Regenerating any part of it mid-flow fails as {@code Code not valid}.
 */
public final class Pkce {

    /** RFC 7636 unreserved set: ALPHA / DIGIT / "-" / "." / "_" / "~". */
    private static final String UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

    /** Within the 43-128 character range RFC 7636 allows. */
    private static final int VERIFIER_LENGTH = 64;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String verifier;
    private final String challenge;

    private Pkce(String verifier, String challenge) {
        this.verifier = verifier;
        this.challenge = challenge;
    }

    public static Pkce generate() {
        StringBuilder sb = new StringBuilder(VERIFIER_LENGTH);
        for (int i = 0; i < VERIFIER_LENGTH; i++) {
            sb.append(UNRESERVED.charAt(RANDOM.nextInt(UNRESERVED.length())));
        }
        String generated = sb.toString();
        return new Pkce(generated, challengeFor(generated));
    }

    /**
     * BASE64URL(SHA256(verifier)), unpadded. The padding is the classic silent failure: a trailing
     * '=' is accepted by the encoder and rejected by the authorization server.
     */
    static String challengeFor(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }

    public String verifier() {
        return verifier;
    }

    public String challenge() {
        return challenge;
    }
}
