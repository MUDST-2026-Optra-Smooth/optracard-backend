package org.example.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String validSecret = "this-is-a-very-secure-secret-key-that-is-at-least-32-bytes-long!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(validSecret);
    }

    @Test
    void constructor_withShortSecret_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("short-secret"));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(null));
    }

    @Test
    void generateToken_extractsCorrectSubjectAndRole() {
        String token = jwtUtil.generateToken("user@example.com", "ROLE_USER");
        assertNotNull(token);

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

        assertEquals("user@example.com", email);
        assertEquals("ROLE_USER", role);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void validateToken_withTamperedSignature_returnsFalse() {
        String token = jwtUtil.generateToken("admin@example.com", "ROLE_ADMIN");
        String tamperedToken = token + "xyz";
        assertFalse(jwtUtil.validateToken(tamperedToken));
    }
}
