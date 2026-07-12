package com.aque.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-only-minimum-256-bits-long";

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_deveSerValidoEExtrairUsername() {
        String token = jwtService.generateToken("bruno");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("bruno");
    }

    @Test
    void isTokenValid_tokenMalformado_retornaFalse() {
        assertThat(jwtService.isTokenValid("token-invalido")).isFalse();
    }

    @Test
    void isTokenValid_tokenExpirado_retornaFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("bruno")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_assinaturaDeOutraChave_retornaFalse() {
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "outra-chave-completamente-diferente-e-tambem-bem-grande-256-bits"
                        .getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("bruno")
                .expiration(new Date(System.currentTimeMillis() + 10_000))
                .signWith(outraChave)
                .compact();

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
