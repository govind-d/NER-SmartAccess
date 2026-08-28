package com.ner.smartlogix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the NER-SmartLogix-AI backend.
 *
 * <p>{@code @SpringBootApplication} is three annotations in one:
 * <ul>
 *   <li>{@code @Configuration}       - this class may declare beans</li>
 *   <li>{@code @EnableAutoConfiguration} - Spring Boot wires the web server, JPA,
 *       Flyway, security and so on based on what is on the classpath</li>
 *   <li>{@code @ComponentScan}       - every class annotated with {@code @Component},
 *       {@code @Service}, {@code @Repository} or {@code @RestController} inside
 *       {@code com.ner.smartlogix} is discovered automatically</li>
 * </ul>
 *
 * <p>{@code @EnableScheduling} is switched on now because later phases add
 * {@code @Scheduled} jobs (weather sweep, risk re-scoring, GPS simulator).
 */
@SpringBootApplication
@EnableScheduling
public class NerSmartLogixApplication {

    public static void main(String[] args) {
        SpringApplication.run(NerSmartLogixApplication.class, args);
    }
}
