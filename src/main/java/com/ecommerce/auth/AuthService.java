package com.ecommerce.auth;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.security.JwtService;
import com.ecommerce.user.model.Role;
import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account already exists for this email.");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        User saved = userRepository.save(user);
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername(saved.getEmail())
                .password(saved.getPassword()).roles(saved.getRole().name()).build();
        return toResponse(saved, jwtService.generateToken(details));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        UserDetails details = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmailIgnoreCase(details.getUsername()).orElseThrow();
        return toResponse(user, jwtService.generateToken(details));
    }

    private AuthResponse toResponse(User user, String token) {
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(),
                new AuthResponse.UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole()));
    }
}
