package com.ner.smartlogix.websocket;

import com.ner.smartlogix.security.CustomUserDetailsService;
import com.ner.smartlogix.security.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Authenticates a WebSocket session.
 *
 * <p>A WebSocket carries no HTTP headers after the handshake, so the usual
 * {@code JwtAuthenticationFilter} cannot help here. The client therefore sends its token
 * once, in the STOMP CONNECT frame, and the resulting Principal stays bound to the
 * session for its whole life - which is also what makes {@code /user/queue/...}
 * messages reach the right person.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = firstHeader(accessor, "Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (tokenProvider.isValid(token)) {
                    UserDetails user = userDetailsService.loadUserByUsername(
                            tokenProvider.getUsernameFromToken(token));
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()));
                    log.debug("STOMP session authenticated as '{}'", user.getUsername());
                } else {
                    log.debug("STOMP CONNECT rejected: invalid token");
                }
            }
        }
        return message;
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
