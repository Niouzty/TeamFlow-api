package com.teamflow.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    @Test
    void generatedTokenCanBeDecodedAndExpiresAfterFifteenMinutes() {
        SecretKey secretKey = new SecretKeySpec(new byte[32], "HmacSHA256");
        JwtTokenService tokenService =
                new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(secretKey)));
        JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.USER);

        Jwt jwt = jwtDecoder.decode(tokenService.generateToken(user));

        assertEquals("alice@example.com", jwt.getSubject());
        assertEquals("USER", jwt.getClaimAsString("role"));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
    }
}
