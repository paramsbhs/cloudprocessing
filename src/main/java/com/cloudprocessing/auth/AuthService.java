package com.cloudprocessing.auth;

import com.cloudprocessing.common.AppException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw AppException.conflict("Email already registered");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase().strip());
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Throws AuthenticationException on bad credentials
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email().toLowerCase().strip(),
                request.password()));

        User user = userRepository.findByEmail(request.email().toLowerCase().strip())
            .orElseThrow(() -> AppException.unauthorized("Invalid credentials"));

        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getEmail());
    }
}
