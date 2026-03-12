package com.cloudprocessing.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Called by Spring Security's DaoAuthenticationProvider during login. */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    /** Called by JwtAuthFilter on every authenticated request. */
    public UserDetails loadUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    }
}
