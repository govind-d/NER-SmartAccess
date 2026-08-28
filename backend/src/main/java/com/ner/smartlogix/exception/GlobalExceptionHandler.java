package com.ner.smartlogix.exception;

import com.ner.smartlogix.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * One place where every exception in the application becomes an HTTP response.
 *
 * <p>{@code @RestControllerAdvice} makes these methods apply to EVERY controller, so no
 * controller needs a try/catch. Services simply throw a meaningful exception and this
 * class decides the status code and the wording.
 *
 * <p>Note the last handler: unexpected exceptions are logged with their full stack trace
 * but the client is told only "An unexpected error occurred". Internal details such as
 * SQL fragments or class names must never reach a browser.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- 400 : the request itself is wrong ----------

    /** Fired when a @Valid request body fails validation. Returns every rejected field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(
                        v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
                       MethodArgumentTypeMismatchException.class,
                       MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex,
                                                                HttpServletRequest request) {
        log.debug("Malformed request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed or missing request data", request, null);
    }

    // ---------- 401 / 403 : who you are, and what you may do ----------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                              HttpServletRequest request) {
        // Deliberately vague: never reveal whether it was the username or the password
        // that was wrong, or an attacker could enumerate valid usernames.
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password", request, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "This account has been disabled", request, null);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefresh(TokenRefreshException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action", request, null);
    }

    // ---------- 404 / 409 / 422 : the domain says no ----------

    @ExceptionHandler({ResourceNotFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    // ---------- 500 : anything we did not foresee ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex,
                                                          HttpServletRequest request) {
        log.error("Unexpected error at {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request,
                                                List<ErrorResponse.FieldError> errors) {
        ErrorResponse body = ErrorResponse.of(message, request.getRequestURI(),
                status.value(), errors);
        return ResponseEntity.status(status).body(body);
    }
}
