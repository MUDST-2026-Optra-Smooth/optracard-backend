package org.example.service;

import org.example.dto.CollectionItemResponse;
import org.example.dto.ProfileResponse;
import org.example.dto.ProfileUpdateRequest;
import org.example.dto.ProfileUpdateResponse;
import org.example.model.User;
import org.example.repository.CollectionItemProjection;
import org.example.repository.OrderItemRepository;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CollectionItemProjection collectionItem;

    private ProfileService profileService;
    private User user;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository, orderItemRepository, jwtUtil, passwordEncoder);

        user = new User();
        user.setUaId(7);
        user.setUsername("old-name");
        user.setEmail("old@example.com");
        user.setPhone("0000000000");
        user.setAddress("Old address");
        user.setPassword("old-hash");
        user.setRole("USER");

    }

    @Test
    void getProfileReturnsUserFieldsAndPurchasedCollection() {
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));
        stubPurchasedCollection();

        ProfileResponse response = profileService.getProfile("old@example.com");

        assertEquals(7, response.userId());
        assertEquals("old-name", response.username());
        assertEquals("old@example.com", response.email());
        assertEquals("0000000000", response.phone());
        assertEquals("Old address", response.address());
        assertEquals(List.of(new CollectionItemResponse(
                101,
                "Charizard ex",
                "Pokemon",
                "https://example.test/card.png",
                2L
        )), response.collection());
    }

    @Test
    void updateProfilePersistsNormalizedFieldsAndHashesNewPassword() {
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        when(jwtUtil.generateToken("new@example.com", "USER")).thenReturn("fresh-token");
        stubPurchasedCollection();

        ProfileUpdateResponse response = profileService.updateProfile(
                "old@example.com",
                new ProfileUpdateRequest(
                        "  new-name  ",
                        "  new@example.com  ",
                        " 0812345678 ",
                        "  New address  ",
                        "new-password"
                )
        );

        assertEquals("new-name", user.getUsername());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("0812345678", user.getPhone());
        assertEquals("New address", user.getAddress());
        assertEquals("new-hash", user.getPassword());
        assertEquals("fresh-token", response.token());
        assertEquals("Profile updated successfully", response.message());
        assertEquals("new-name", response.profile().username());
        assertEquals("new@example.com", response.profile().email());
        verify(userRepository).saveAndFlush(user);
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void updateProfileKeepsExistingPasswordWhenPasswordIsBlank() {
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("old@example.com", "USER")).thenReturn("same-token");
        stubPurchasedCollection();

        ProfileUpdateResponse response = profileService.updateProfile(
                "old@example.com",
                new ProfileUpdateRequest("new-name", "old@example.com", "", "New address", " ")
        );

        assertEquals("old-hash", user.getPassword());
        assertEquals("same-token", response.token());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void updateProfileRejectsDuplicateEmail() {
        User otherUser = new User();
        otherUser.setUaId(8);
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updateProfile(
                        "old@example.com",
                        new ProfileUpdateRequest("new-name", "taken@example.com", null, null, null)
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateProfileRejectsBlankRequiredFields() {
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updateProfile(
                        "old@example.com",
                        new ProfileUpdateRequest(" ", "old@example.com", null, null, null)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void getProfileReturnsNotFoundWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> profileService.getProfile("missing@example.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(orderItemRepository, never()).findPurchasedCardsByUserId(any());
    }

    private void stubPurchasedCollection() {
        when(orderItemRepository.findPurchasedCardsByUserId(7)).thenReturn(List.of(collectionItem));
        when(collectionItem.getProductId()).thenReturn(101);
        when(collectionItem.getName()).thenReturn("Charizard ex");
        when(collectionItem.getGame()).thenReturn("Pokemon");
        when(collectionItem.getImageUrl()).thenReturn("https://example.test/card.png");
        when(collectionItem.getQuantity()).thenReturn(2L);
    }
}
