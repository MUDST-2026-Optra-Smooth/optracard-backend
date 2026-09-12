package org.example.controller;

import org.example.dto.CollectionItemResponse;
import org.example.dto.ProfileResponse;
import org.example.dto.ProfileUpdateRequest;
import org.example.dto.ProfileUpdateResponse;
import org.example.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController(profileService)).build();
    }

    @Test
    void getProfileRejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfileReturnsTheAuthenticatedUsersProfile() throws Exception {
        ProfileResponse profile = new ProfileResponse(
                7,
                "new-user",
                "new@example.com",
                "0812345678",
                "Bangkok",
                List.of(new CollectionItemResponse(101, "Charizard ex", "Pokemon", null, 2L))
        );
        when(profileService.getProfile("new@example.com")).thenReturn(profile);

        mockMvc.perform(get("/api/profile")
                        .principal(new UsernamePasswordAuthenticationToken("new@example.com", null, List.of())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.username").value("new-user"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.phone").value("0812345678"))
                .andExpect(jsonPath("$.collection[0].name").value("Charizard ex"));

        verify(profileService).getProfile("new@example.com");
    }

    @Test
    void updateProfilePassesAuthenticatedEmailAndBodyToService() throws Exception {
        ProfileResponse profile = new ProfileResponse(7, "updated-user", "updated@example.com", null, null, List.of());
        when(profileService.updateProfile(eq("old@example.com"), any(ProfileUpdateRequest.class)))
                .thenReturn(new ProfileUpdateResponse(profile, "new-token", "Profile updated successfully"));

        mockMvc.perform(put("/api/profile")
                        .principal(new UsernamePasswordAuthenticationToken("old@example.com", null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "updated-user",
                                  "email": "updated@example.com",
                                  "phone": "0812345678",
                                  "address": "Bangkok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("updated-user"))
                .andExpect(jsonPath("$.token").value("new-token"))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        verify(profileService).updateProfile(eq("old@example.com"), any(ProfileUpdateRequest.class));
    }
}
