package com.aicodereview.security;

import com.aicodereview.config.AppProperties;
import com.aicodereview.entity.Role;
import com.aicodereview.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef-test-secret";

    private JwtService service(String secret) {
        AppProperties props = new AppProperties();
        props.getSecurity().setJwtSecret(secret);
        return new JwtService(props);
    }

    private User user() {
        return User.builder().id(42L).username("alice").email("alice@example.com").role(Role.ADMIN).enabled(true).build();
    }

    @Test
    void accessTokenRoundTripKeepsIdentityAndRole() {
        JwtService jwt = service(SECRET);
        AuthenticatedUser parsed = jwt.parseAccessToken(jwt.createAccessToken(user()));
        assertEquals(42L, parsed.id());
        assertEquals("alice@example.com", parsed.email());
        assertEquals("ADMIN", parsed.role());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = service(SECRET);
        String token = jwt.createAccessToken(user());
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertThrows(JwtException.class, () -> jwt.parseAccessToken(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = service(SECRET).createAccessToken(user());
        assertThrows(JwtException.class, () -> service("another-secret-another-secret-another-secret").parseAccessToken(token));
    }

    @Test
    void githubStateIsNotAnAccessTokenAndViceVersa() {
        JwtService jwt = service(SECRET);
        String state = jwt.createGithubState(42L);
        assertEquals(42L, jwt.parseGithubState(state));
        assertThrows(JwtException.class, () -> jwt.parseAccessToken(state));
        assertThrows(JwtException.class, () -> jwt.parseGithubState(jwt.createAccessToken(user())));
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> service("too-short"));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(JwtException.class, () -> service(SECRET).parseAccessToken("not.a.jwt"));
    }
}
