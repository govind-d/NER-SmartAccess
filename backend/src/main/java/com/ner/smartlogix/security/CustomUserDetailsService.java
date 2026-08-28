package com.ner.smartlogix.security;

import com.ner.smartlogix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells Spring Security how to find a user. It is called in exactly two places:
 * during login (to check the password) and on every request carrying a JWT
 * (to rebuild the user's authorities).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));
    }
}
