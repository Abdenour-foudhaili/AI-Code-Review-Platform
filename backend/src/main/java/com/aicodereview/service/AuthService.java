package com.aicodereview.service;

import com.aicodereview.config.AppProperties;
import com.aicodereview.dto.AuthResponse;
import com.aicodereview.dto.LoginRequest;
import com.aicodereview.dto.RegisterRequest;
import com.aicodereview.dto.UserResponse;
import com.aicodereview.entity.RefreshToken;
import com.aicodereview.entity.Role;
import com.aicodereview.entity.User;
import com.aicodereview.exception.DuplicateResourceException;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.repository.RefreshTokenRepository;
import com.aicodereview.repository.UserRepository;
import com.aicodereview.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    /** Access token body + raw refresh token (sent as HttpOnly cookie by the controller). */
    public record AuthResult(AuthResponse body, String refreshToken) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppProperties props;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService, AppProperties props) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.props = props;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (users.existsByUsernameIgnoreCase(request.getUsername().trim())) {
            throw new DuplicateResourceException("This username is already taken");
        }
        User user = users.save(User.builder()
                .username(request.getUsername().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))   // BCrypt, never clear text
                .role(Role.USER)                                            // self-registration never yields ADMIN
                .enabled(true)
                .build());
        return issue(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        // throws BadCredentialsException / DisabledException -> mapped to 401 by GlobalExceptionHandler
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return issue(user);
    }

    /** Rotates the refresh token. Re-use of a revoked token revokes every session of that user. */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new BadCredentialsException("Missing refresh token");
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (stored.isRevoked()) {
            refreshTokens.revokeAllForUser(stored.getUser().getId());
            throw new BadCredentialsException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        User user = stored.getUser();
        if (!user.isEnabled()) throw new BadCredentialsException("Account disabled");
        stored.setRevoked(true);
        refreshTokens.save(stored);
        return issue(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokens.save(t);
        });
        refreshTokens.deleteExpired(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return toResponse(users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    public int refreshTtlSeconds() { return props.getSecurity().getRefreshExpirationDays() * 24 * 3600; }

    private AuthResult issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(raw))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(props.getSecurity().getRefreshExpirationDays()))
                .revoked(false)
                .build());
        AuthResponse body = new AuthResponse(jwtService.createAccessToken(user), "Bearer", jwtService.accessTtlSeconds(), toResponse(user));
        return new AuthResult(body, raw);
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }

    static String hash(String raw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
