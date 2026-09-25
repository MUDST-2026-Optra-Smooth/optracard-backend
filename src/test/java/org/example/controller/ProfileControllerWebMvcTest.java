package org.example.controller;

import org.example.dto.ProfileResponse;
import org.example.security.JwtUtil;
import org.example.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A Spring MVC integration test: the real controller, argument resolvers and
 * JSON message converters are assembled while the service is mocked.
 */
@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void getProfileReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileSerializesTheServiceResponse() throws Exception {
        when(profileService.getProfile("user@example.com"))
                .thenReturn(new ProfileResponse(3, "user", "user@example.com", "0812345678", "Bangkok", List.of()));

        mockMvc.perform(get("/api/profile")
                        .principal(new UsernamePasswordAuthenticationToken("user@example.com", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }
}
