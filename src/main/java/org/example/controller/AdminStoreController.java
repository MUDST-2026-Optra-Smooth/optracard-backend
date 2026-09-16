package org.example.controller;

import org.example.dto.StoreApplicationResponse;
import org.example.service.MarketplaceStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/stores")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminStoreController {
    private final MarketplaceStoreService storeService;

    public AdminStoreController(MarketplaceStoreService storeService) {
        this.storeService = storeService;
    }

    @PutMapping("/{id}/review")
    public StoreApplicationResponse review(Authentication authentication,
                                           @PathVariable Integer id,
                                           @RequestBody ReviewRequest request) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN"));
        if (!admin) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        return storeService.reviewApplication(id, request == null ? null : request.status(), request == null ? null : request.note());
    }

    public record ReviewRequest(String status, String note) {}
}
