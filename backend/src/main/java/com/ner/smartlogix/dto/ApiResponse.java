package com.ner.smartlogix.dto;

import java.time.OffsetDateTime;

/**
 * The single success envelope returned by every endpoint in the system.
 *
 * <p>A consistent shape means the React client can write ONE response handler instead
 * of guessing whether an endpoint returned a bare object, a list or a message.
 *
 * <p>This is a Java {@code record}: an immutable class where the compiler writes the
 * constructor, getters, {@code equals}, {@code hashCode} and {@code toString} for us.
 * Jackson serialises it exactly like a normal object.
 *
 * @param <T> the type of the payload
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        OffsetDateTime timestamp) {

    /** Data plus a human-readable message. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now());
    }

    /** Data only, with a default message. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Request successful", data, OffsetDateTime.now());
    }

    /** For operations that change something but return nothing, e.g. a delete. */
    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null, OffsetDateTime.now());
    }
}
