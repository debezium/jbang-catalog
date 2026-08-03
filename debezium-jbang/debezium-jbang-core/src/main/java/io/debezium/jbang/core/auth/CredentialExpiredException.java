/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

/**
 * The stored refresh token is dead and cannot be recovered without a fresh login.
 * <p>
 * Separate from {@link AuthException} so callers can tell "you need to log in again" apart from
 * "something went wrong talking to the server" — only the former should print a login command.
 */
public class CredentialExpiredException extends AuthException {

    private static final long serialVersionUID = 1L;

    public CredentialExpiredException(String message) {
        super(message);
    }
}
