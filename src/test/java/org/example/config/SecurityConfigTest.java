package org.example.config;

import org.example.security.JwtAuthenticationFilter;
import org.example.security.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private RateLimitFilter rateLimitFilter;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(rateLimitFilter, jwtAuthenticationFilter);
    }

    @Test
    void passwordEncoder_hashesAndMatchesPasswords() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);

        String rawPassword = "securePassword123!";
        String encoded = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertTrue(encoder.matches(rawPassword, encoded));
        assertFalse(encoder.matches("wrongPassword", encoded));
    }

    @Test
    void corsConfigurationSource_configuresAllowedOriginsAndMethods() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/products");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        List<String> origins = config.getAllowedOrigins();
        assertNotNull(origins);
        assertTrue(origins.containsAll(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:4173",
                "http://127.0.0.1:4173",
                "http://localhost:30080",
                "http://127.0.0.1:30080"
        )));
        assertEquals("http://127.0.0.1:30080", config.checkOrigin("http://127.0.0.1:30080"));
        assertNull(config.checkOrigin("http://127.0.0.1:30081"));

        List<String> methods = config.getAllowedMethods();
        assertNotNull(methods);
        assertTrue(methods.containsAll(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")));

        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
        assertEquals(3600L, config.getMaxAge());

        List<String> exposedHeaders = config.getExposedHeaders();
        assertNotNull(exposedHeaders);
        assertTrue(exposedHeaders.containsAll(List.of("Authorization", "Content-Type", "Retry-After", "X-XSRF-TOKEN")));
    }
}
