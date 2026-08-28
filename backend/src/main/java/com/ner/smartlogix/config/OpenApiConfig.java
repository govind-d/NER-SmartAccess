package com.ner.smartlogix.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Swagger UI at {@code /swagger-ui.html}.
 *
 * <p>The security scheme below is what adds the green "Authorize" button: paste an
 * access token once and every subsequent "Try it out" call carries the Authorization
 * header. Without it you could only exercise the three public endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI nerSmartLogixOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NER-SmartLogix-AI API")
                        .version("v1")
                        .description("""
                                AI-Based Smart Logistics and Accessibility Intelligence \
                                Platform for the North Eastern Region, India.

                                Log in through POST /api/v1/auth/login, copy the \
                                accessToken from the response, then click Authorize and \
                                paste it (without the word Bearer).""")
                        .contact(new Contact().name("NER-SmartLogix-AI project"))
                        .license(new License().name("Academic project")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
