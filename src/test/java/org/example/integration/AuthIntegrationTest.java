package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.AuthRequest;
import org.example.dto.RegisterRequest;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user, save to DB with hashed password, and allow login")
    void registerAndLogin_endToEnd() throws Exception {
        // RegisterRequest record: (username, email, phone, address, password)
        RegisterRequest registerReq = new RegisterRequest(
                "alice",
                "alice@example.com",
                "0891234567",
                "123 Bangkok Road",
                "Password123!"
        );

        // 1. Register new user
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // Verify state in database
        User savedUser = userRepository.findByEmail("alice@example.com").orElse(null);
        assertNotNull(savedUser, "User should be persisted in database");
        assertEquals("alice", savedUser.getUsername());
        assertEquals("USER", savedUser.getRole());
        assertTrue(passwordEncoder.matches("Password123!", savedUser.getPassword()), "Password should be hashed with BCrypt");

        // 2. Login with registered credentials
        AuthRequest loginReq = new AuthRequest("alice@example.com", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Should reject duplicate registration with 400 Bad Request")
    void register_duplicateEmail_returnsBadRequest() throws Exception {
        RegisterRequest initialUser = new RegisterRequest(
                "bob",
                "bob@example.com",
                "0811111111",
                "Address 1",
                "Secret123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialUser)))
                .andExpect(status().isOk());

        // Try registering the same email again
        RegisterRequest duplicateUser = new RegisterRequest(
                "bob_new",
                "bob@example.com",
                "0822222222",
                "Address 2",
                "AnotherPassword"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    @DisplayName("Should reject login with invalid password with 401 Unauthorized")
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "charlie",
                "charlie@example.com",
                "0833333333",
                "Address 3",
                "CorrectPassword"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk());

        AuthRequest wrongPasswordLogin = new AuthRequest("charlie@example.com", "WrongPassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid password"));
    }

    @Test
    @DisplayName("Should reject login with non-existent email with 401 Unauthorized")
    void login_withNonExistentEmail_returnsUnauthorized() throws Exception {
        AuthRequest nonExistent = new AuthRequest("nobody@example.com", "Password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
