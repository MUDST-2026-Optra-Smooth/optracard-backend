package org.example.controller;

import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.dto.RegisterRequest;
import org.example.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("john", "john@example.com", "Password123", "0812345678", "Bangkok");
        authRequest = new AuthRequest("john@example.com", "Password123");
    }

    @Test
    void register_whenSuccessful_returns200() {
        doNothing().when(authService).register(registerRequest);

        ResponseEntity<?> response = authController.register(registerRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("User registered successfully"));
        verify(authService).register(registerRequest);
    }

    @Test
    void register_whenThrowsException_returnsBadRequest() {
        doThrow(new RuntimeException("Email already exists")).when(authService).register(registerRequest);

        ResponseEntity<?> response = authController.register(registerRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Email already exists"));
    }

    @Test
    void login_whenSuccessful_returnsAuthResponse() {
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", 1, "john", "USER", "john@example.com");
        when(authService.login(authRequest)).thenReturn(authResponse);

        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void login_whenFails_returns401() {
        when(authService.login(authRequest)).thenThrow(new RuntimeException("Bad credentials"));

        ResponseEntity<?> response = authController.login(authRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Bad credentials"));
    }
}
