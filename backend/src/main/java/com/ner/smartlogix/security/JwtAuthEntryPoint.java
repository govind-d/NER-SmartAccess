package com.ner.smartlogix.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ner.smartlogix.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Runs when an anonymous caller hits a protected endpoint.
 *
 * <p>Without it Spring would answer with an HTML login page - useless to a React client.
 * This writes the same JSON error envelope the rest of the API uses, so the frontend has
 * exactly one error shape to parse.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(
                "Authentication required. Provide a valid Bearer token.",
                request.getRequestURI(),
                HttpStatus.UNAUTHORIZED.value());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
