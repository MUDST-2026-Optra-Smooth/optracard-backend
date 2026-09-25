package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.dto.RegisterRequest;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void registerStoresContactDetailsAndBcryptPassword() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt-hash");

        authService.register(new RegisterRequest(
                "new-user",
                "new@example.com",
                "0812345678",
                "Bangkok",
                "secret"
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("new-user", saved.getUsername());
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("0812345678", saved.getPhone());
        assertEquals("Bangkok", saved.getAddress());
        assertEquals("bcrypt-hash", saved.getPassword());
        assertEquals("USER", saved.getRole());
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new User()));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(new RegisterRequest(
                        "another-user", "taken@example.com", null, null, "secret"
                ))
        );

        assertEquals("Email already in use", exception.getMessage());
    }

    @Test
    void loginReturnsJwtAndPublicUserData() {
        User user = new User();
        user.setUsername("existing-user");
        user.setEmail("existing@example.com");
        user.setPassword("bcrypt-hash");
        user.setRole("USER");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "bcrypt-hash")).thenReturn(true);
        when(jwtUtil.generateToken("existing@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new AuthRequest("existing@example.com", "secret"));

        assertEquals("jwt-token", response.token());
        assertEquals("existing-user", response.username());
        assertEquals("USER", response.role());
    }

    @Test
    void loginRejectsAnInvalidPassword() {
        User user = new User();
        user.setEmail("existing@example.com");
        user.setPassword("bcrypt-hash");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(new AuthRequest("existing@example.com", "wrong"))
        );

        assertEquals("Invalid password", exception.getMessage());
    }
}
