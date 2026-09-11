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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(
            UserRepository userRepository,
            OrderItemRepository orderItemRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileResponse getProfile(String email) {
        User user = findUser(email);
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileUpdateResponse updateProfile(String currentEmail, ProfileUpdateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile data is required");
        }

        User user = findUser(currentEmail);

        String username = normalizeRequired(request.username(), "Username is required");
        String email = normalizeRequired(request.email(), "Email is required");

        userRepository.findByEmail(email)
                .filter(existingUser -> !Objects.equals(existingUser.getUaId(), user.getUaId()))
                .ifPresent(existingUser -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                });

        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(normalizeOptional(request.phone()));
        user.setAddress(normalizeOptional(request.address()));

        String password = normalizeOptional(request.password());
        if (password != null) {
            user.setPassword(passwordEncoder.encode(password));
        }

        // Flush immediately so a successful response means the new values have
        // actually been written to the database before the client is notified.
        userRepository.saveAndFlush(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new ProfileUpdateResponse(
                toProfileResponse(user),
                token,
                "Profile updated successfully"
        );
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileResponse toProfileResponse(User user) {

        List<CollectionItemResponse> collection = orderItemRepository
                .findPurchasedCardsByUserId(user.getUaId())
                .stream()
                .map(this::toCollectionItem)
                .toList();

        return new ProfileResponse(
                user.getUaId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                collection
        );
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private CollectionItemResponse toCollectionItem(CollectionItemProjection item) {
        return new CollectionItemResponse(
                item.getProductId(),
                item.getName(),
                item.getGame(),
                item.getImageUrl(),
                item.getQuantity()
        );
    }
}
