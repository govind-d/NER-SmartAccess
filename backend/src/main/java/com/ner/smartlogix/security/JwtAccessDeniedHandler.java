package com.ner.smartlogix.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ner.smartlogix.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Runs when a caller IS logged in but lacks the required role - a DRIVER calling an
 * admin endpoint, for example. The difference from {@link JwtAuthEntryPoint} matters:
 * 401 means "I do not know who you are", 403 means "I know who you are, and no".
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(
                "You do not have permission to perform this action",
                request.getRequestURI(),
                HttpStatus.FORBIDDEN.value());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
