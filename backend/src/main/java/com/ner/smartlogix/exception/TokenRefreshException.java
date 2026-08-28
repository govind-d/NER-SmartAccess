package com.ner.smartlogix.exception;

/** Thrown when a refresh token is unknown, expired or already revoked. Becomes a 401. */
public class TokenRefreshException extends RuntimeException {

    public TokenRefreshException(String message) {
        super(message);
    }
}
