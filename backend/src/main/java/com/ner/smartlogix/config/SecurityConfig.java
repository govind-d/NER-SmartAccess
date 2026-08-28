package com.ner.smartlogix.config;

import com.ner.smartlogix.security.JwtAccessDeniedHandler;
import com.ner.smartlogix.security.JwtAuthEntryPoint;
import com.ner.smartlogix.security.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The real security configuration (this file replaces the temporary Phase 2 version
 * that permitted everything).
 *
 * <p>Four decisions are encoded here:
 * <ol>
 *   <li><b>Stateless.</b> {@code SessionCreationPolicy.STATELESS} means the server keeps
 *       no session in memory; identity travels in the JWT on every request. This is what
 *       lets the backend be restarted or scaled out without logging anybody out.</li>
 *   <li><b>CSRF disabled.</b> CSRF attacks rely on the browser automatically attaching a
 *       cookie. We authenticate with an Authorization header that a foreign site cannot
 *       set, so the protection is not needed - and it would break every POST.</li>
 *   <li><b>Explicit public list.</b> Everything not listed as public requires a valid
 *       token. The default is "deny", never "allow".</li>
 *   <li><b>Coarse rules here, fine rules in the services.</b> This file only says
 *       "you must be logged in". Role checks live next to the code they protect, as
 *       {@code @PreAuthorize} annotations, because that is where they can be read
 *       together with the business rule they enforce.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // switches on @PreAuthorize / @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    /** Endpoints reachable without a token. Everything else needs one. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",   // the service still requires ADMIN unless the
            "/api/v1/auth/refresh",    // database has no users at all (bootstrap)
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/ws/**"                   // STOMP handshake; authenticated in Phase 6
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtAuthEntryPoint)   // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler))  // 403
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Browsers send a pre-flight OPTIONS before a cross-origin call;
                        // it carries no credentials and must not be challenged.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                // Our filter must run BEFORE the username/password filter so that a
                // request carrying a JWT is already authenticated by the time Spring
                // decides whether to challenge it.
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Exposes the AuthenticationManager so AuthServiceImpl can hand it a username and
     * password and let Spring do the BCrypt comparison.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt automatically generates and embeds a per-password salt, which is why there
     * is no salt column in the users table. Strength 10 balances safety against login
     * latency.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * The browser refuses cross-origin calls unless the server allows them. Only the one
     * configured frontend origin is allowed - never "*", which would let any website on
     * the internet call this API with the user's token.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
