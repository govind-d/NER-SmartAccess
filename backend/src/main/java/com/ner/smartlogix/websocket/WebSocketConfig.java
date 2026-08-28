package com.ner.smartlogix.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket.
 *
 * <p>Plain WebSocket gives one raw two-way pipe. STOMP adds topics and subscriptions on
 * top of it, which is what lets the dashboard subscribe to every vehicle while a driver
 * subscribes only to their own - over a single connection.
 *
 * <p>The broker is Spring's in-memory {@code SimpleBroker}: enough for one node, which is
 * what this project runs. Switching to RabbitMQ for multiple nodes would mean replacing
 * {@code enableSimpleBroker} with {@code enableStompBrokerRelay} and nothing else.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigin)
                .withSockJS();   // fallback for networks that block raw WebSockets
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages the server sends out.
        registry.enableSimpleBroker("/topic", "/queue");
        // Messages the client sends in, routed to @MessageMapping methods.
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix for messages addressed to one user: /user/queue/notifications
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Authentication happens once, on the CONNECT frame - see the interceptor.
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
