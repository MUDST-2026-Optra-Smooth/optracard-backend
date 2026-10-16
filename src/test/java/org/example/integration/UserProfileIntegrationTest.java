package org.example.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.ProfileUpdateRequest;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setPhone("0812345678");
        testUser.setAddress("123 Old Town Road");
        testUser.setPassword(passwordEncoder.encode("SecretPass123"));
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        jwtToken = jwtUtil.generateToken(testUser.getEmail(), testUser.getRole());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/profile is rejected with 403")
    void getProfile_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated user can fetch their profile successfully")
    void getProfile_withAuth_returnsProfileData() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getUaId()))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("0812345678"))
                .andExpect(jsonPath("$.address").value("123 Old Town Road"));
    }

    @Test
    @DisplayName("Authenticated user can update profile and changes persist to database")
    void updateProfile_withAuth_updatesDatabaseAndReturnsNewProfile() throws Exception {
        ProfileUpdateRequest updateReq = new ProfileUpdateRequest(
                "john_updated",
                "john.updated@example.com",
                "0899999999",
                "456 New City Avenue",
                null
        );

        mockMvc.perform(put("/api/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("john_updated"))
                .andExpect(jsonPath("$.profile.email").value("john.updated@example.com"))
                .andExpect(jsonPath("$.profile.phone").value("0899999999"))
                .andExpect(jsonPath("$.profile.address").value("456 New City Avenue"))
                .andExpect(jsonPath("$.token").isString());

        // Verify update in the database
        User updated = userRepository.findById(testUser.getUaId()).orElseThrow();
        assertEquals("john_updated", updated.getUsername());
        assertEquals("john.updated@example.com", updated.getEmail());
        assertEquals("0899999999", updated.getPhone());
        assertEquals("456 New City Avenue", updated.getAddress());
    }
}
