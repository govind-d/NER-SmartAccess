package com.ner.smartlogix.exception;

/**
 * Thrown when a value that must be unique already exists (username, email, road code).
 * Becomes a 409 Conflict - not a 400, because the request itself was well formed.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resource, String field, Object value) {
        super("%s already exists with %s: %s".formatted(resource, field, value));
    }
}
