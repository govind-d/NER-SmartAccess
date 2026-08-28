package com.ner.smartlogix.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The HTTP client used to talk to weather and routing services.
 *
 * <p>The timeouts are the point of this class. An external service that hangs must not
 * hang our request threads with it: five seconds to connect and ten to read means a dead
 * provider degrades the platform for ten seconds, not indefinitely.
 *
 * <p>{@link SimpleClientHttpRequestFactory} is used deliberately - it is part of Spring
 * itself and needs no extra HTTP library for the handful of outbound calls this project
 * makes.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    @Bean
    public RestClient.Builder restClientBuilder(ClientHttpRequestFactory requestFactory) {
        return RestClient.builder().requestFactory(requestFactory);
    }
}
