package com.aicodereview.security;

import com.aicodereview.config.AppProperties;
import com.aicodereview.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Issues and validates HS256 JWTs (Nimbus, shipped with Spring Security).
 * Access token claims: sub/uid (user id), email, role, iat, exp.
 */
@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    static final String ISSUER = "ai-code-review-platform";
    private static final String TYP_ACCESS = "access";
    private static final String TYP_GITHUB_STATE = "github_state";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration accessTtl;

    public JwtService(AppProperties props) {
        byte[] keyBytes = resolveSecret(props.getSecurity().getJwtSecret());
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        this.decoder = nimbus;
        this.accessTtl = Duration.ofMinutes(Math.max(1, props.getSecurity().getJwtExpirationMinutes()));
    }

    private static byte[] resolveSecret(String configured) {
        if (configured == null || configured.isBlank()) {
            log.warn("JWT_SECRET is not set: using a random in-memory secret. Tokens will be invalid after a restart. Set JWT_SECRET (>= 32 chars) for real use.");
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = configured.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        return bytes;
    }

    public long accessTtlSeconds() { return accessTtl.toSeconds(); }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(accessTtl))
                .claim("uid", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("typ", TYP_ACCESS)
                .build();
        return encode(claims);
    }

    /** @throws JwtException if the token is invalid, expired or not an access token. */
    public AuthenticatedUser parseAccessToken(String token) {
        Jwt jwt = decoder.decode(token);
        if (!TYP_ACCESS.equals(jwt.getClaimAsString("typ"))) {
            throw new BadJwtException("Not an access token");
        }
        Long uid = Long.valueOf(jwt.getSubject());
        return new AuthenticatedUser(uid, jwt.getClaimAsString("email"), jwt.getClaimAsString("role"));
    }

    /** Short-lived signed value used as the OAuth "state" parameter (CSRF protection + user binding). */
    public String createGithubState(Long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER).subject(String.valueOf(userId))
                .issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(10)))
                .claim("typ", TYP_GITHUB_STATE)
                .build();
        return encode(claims);
    }

    public Long parseGithubState(String state) {
        Jwt jwt = decoder.decode(state);
        if (!TYP_GITHUB_STATE.equals(jwt.getClaimAsString("typ"))) {
            throw new BadJwtException("Not a state token");
        }
        return Long.valueOf(jwt.getSubject());
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
