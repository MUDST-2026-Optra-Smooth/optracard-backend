package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.ProfileResponse;
import org.example.dto.ProfileUpdateRequest;
import org.example.dto.ProfileUpdateResponse;
import org.example.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "User Profile", description = "Endpoints for viewing and updating personal customer profile and delivery address")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(
            summary = "Get current user profile",
            description = "Retrieve the authenticated user's account details, contact info, shipping address, and collection items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(profileService.getProfile(authentication.getName()));
    }

    @Operation(
            summary = "Update current user profile",
            description = "Update username, email, phone, or address. Returns an updated JWT token if the email was changed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email format or duplicate email"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PutMapping
    public ResponseEntity<ProfileUpdateResponse> updateProfile(
            Authentication authentication,
            @RequestBody ProfileUpdateRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(profileService.updateProfile(authentication.getName(), request));
    }
}
