/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.auth;

/**
 * Any authentication failure the user can act on.
 * <p>
 * {@code DebeziumCommand} catches this, prints {@code ERROR: <message>} to stderr and exits 1, so
 * the message is the whole user experience — write it for that channel and name the next command to
 * run wherever one exists.
 */
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
