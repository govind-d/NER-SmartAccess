package com.ner.smartlogix.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The single failure envelope. Produced ONLY by
 * {@link com.ner.smartlogix.exception.GlobalExceptionHandler}, so no controller ever
 * builds an error response by hand.
 *
 * <p>{@code errors} carries per-field validation messages; it is null for
 * everything else, and Jackson omits null fields (see application.yml).
 */
public record ErrorResponse(
        boolean success,
        String message,
        String path,
        int status,
        List<FieldError> errors,
        OffsetDateTime timestamp) {

    /** One rejected field: which field, and why it was rejected. */
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String message, String path, int status) {
        return new ErrorResponse(false, message, path, status, null, OffsetDateTime.now());
    }

    public static ErrorResponse of(String message, String path, int status,
                                   List<FieldError> errors) {
        return new ErrorResponse(false, message, path, status, errors, OffsetDateTime.now());
    }
}
