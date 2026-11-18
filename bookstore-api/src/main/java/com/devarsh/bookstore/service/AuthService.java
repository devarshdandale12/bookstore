package com.devarsh.bookstore.service;

import com.devarsh.bookstore.dto.*;
import com.devarsh.bookstore.entity.Role;
import com.devarsh.bookstore.entity.User;
import com.devarsh.bookstore.exception.ConflictException;
import com.devarsh.bookstore.repository.UserRepository;
import com.devarsh.bookstore.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.admin.registration-code}")
    private String adminRegistrationCode;

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Role role = req.role() == null ? Role.CUSTOMER : req.role();
        // Anyone can sign up as a customer, but becoming an admin needs the secret code.
        if (role == Role.ADMIN && !adminRegistrationCode.equals(req.adminCode())) {
            throw new AccessDeniedException("A valid adminCode is required to register an admin account");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(role);
        return UserResponse.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        // Same message for "unknown email" and "wrong password" so attackers learn nothing.
        User user = users.findByEmail(email)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        return users.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new BadCredentialsException("This account no longer exists"));
    }
}
