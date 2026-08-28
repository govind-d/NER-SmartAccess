package com.ner.smartlogix.exception;

/**
 * Thrown when the request is valid but the domain forbids it - for example moving a
 * delivery from DELIVERED back to IN_TRANSIT, or dispatching a vehicle onto a blocked
 * road. Becomes a 422 Unprocessable Entity.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
