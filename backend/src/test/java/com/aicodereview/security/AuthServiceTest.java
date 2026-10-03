package com.aicodereview.security;

import com.aicodereview.config.AppProperties;
import com.aicodereview.dto.LoginRequest;
import com.aicodereview.dto.RegisterRequest;
import com.aicodereview.entity.RefreshToken;
import com.aicodereview.entity.Role;
import com.aicodereview.entity.User;
import com.aicodereview.exception.DuplicateResourceException;
import com.aicodereview.repository.RefreshTokenRepository;
import com.aicodereview.repository.UserRepository;
import com.aicodereview.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock AuthenticationManager authenticationManager;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getSecurity().setJwtSecret("0123456789abcdef0123456789abcdef-test-secret");
        service = new AuthService(users, refreshTokens, encoder, authenticationManager, new JwtService(props), props);
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(7L);
            return u;
        });
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RegisterRequest register() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("alice");
        r.setEmail("Alice@Example.com");
        r.setPassword("s3cret-password");
        return r;
    }

    @Test
    void registerHashesPasswordNormalisesEmailAndAssignsUserRole() {
        AuthService.AuthResult result = service.register(register());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertNotEquals("s3cret-password", saved.getPassword());
        assertTrue(encoder.matches("s3cret-password", saved.getPassword()));
        assertEquals("alice@example.com", saved.getEmail());
        assertEquals(Role.USER, saved.getRole());
        assertNotNull(result.body().accessToken());
        assertNotNull(result.refreshToken());
    }

    @Test
    void registerStoresOnlyTheHashOfTheRefreshToken() {
        AuthService.AuthResult result = service.register(register());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(captor.capture());
        assertNotEquals(result.refreshToken(), captor.getValue().getTokenHash());
        assertEquals(64, captor.getValue().getTokenHash().length());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.register(register()));
        verify(users, never()).save(any());
    }

    @Test
    void loginWithBadCredentialsPropagatesAuthenticationFailure() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("wrong");
        assertThrows(BadCredentialsException.class, () -> service.login(request));
    }

    @Test
    void refreshRotatesTheToken() {
        User user = User.builder().id(7L).username("alice").email("alice@example.com").role(Role.USER).enabled(true).password("x").build();
        AuthService.AuthResult first = service.register(register());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(captor.capture());
        RefreshToken stored = captor.getValue();
        stored.setUser(user);
        when(refreshTokens.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

        AuthService.AuthResult second = service.refresh(first.refreshToken());

        assertTrue(stored.isRevoked(), "the used refresh token must be revoked");
        assertNotEquals(first.refreshToken(), second.refreshToken());
    }

    @Test
    void reuseOfARevokedRefreshTokenRevokesEverySession() {
        User user = User.builder().id(7L).role(Role.USER).enabled(true).build();
        RefreshToken revoked = RefreshToken.builder().user(user).tokenHash("h").revoked(true).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));
        assertThrows(BadCredentialsException.class, () -> service.refresh("stolen-token"));
        verify(refreshTokens).revokeAllForUser(7L);
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        User user = User.builder().id(7L).role(Role.USER).enabled(true).build();
        RefreshToken expired = RefreshToken.builder().user(user).tokenHash("h").revoked(false).expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(expired));
        assertThrows(BadCredentialsException.class, () -> service.refresh("old"));
    }

    @Test
    void missingRefreshTokenIsRejected() {
        assertThrows(BadCredentialsException.class, () -> service.refresh(null));
    }
}
