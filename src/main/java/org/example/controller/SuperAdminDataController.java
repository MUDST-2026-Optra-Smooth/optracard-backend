package org.example.controller;

import org.example.dto.SuperAdminData;
import org.example.service.SuperAdminDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = "http://localhost:5173")
public class SuperAdminDataController {
    private final SuperAdminDataService superAdminDataService;

    public SuperAdminDataController(SuperAdminDataService superAdminDataService) {
        this.superAdminDataService = superAdminDataService;
    }

    @GetMapping("/overview")
    public SuperAdminData.OverviewResponse overview() { return superAdminDataService.getOverview(); }

    @GetMapping("/stores")
    public List<SuperAdminData.StoreResponse> stores() { return superAdminDataService.getStores(); }

    @GetMapping("/stores/{storeId}")
    public SuperAdminData.StoreResponse store(@PathVariable Integer storeId) { return superAdminDataService.getStore(storeId); }

    @GetMapping("/catalog")
    public List<SuperAdminData.CatalogProductResponse> catalog() { return superAdminDataService.getCatalog(); }

    @GetMapping("/transactions")
    public List<SuperAdminData.TransactionResponse> transactions() { return superAdminDataService.getTransactions(); }

    @GetMapping("/users")
    public List<SuperAdminData.UserResponse> users() { return superAdminDataService.getUsers(); }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer userId,
                                           @RequestBody SuperAdminData.DeleteAccountRequest request,
                                           Authentication authentication) {
        superAdminDataService.deleteUser(userId, authentication == null ? null : authentication.getName(),
                request == null ? null : request.confirmation());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/staff")
    public List<SuperAdminData.StaffResponse> staff() { return superAdminDataService.getStaff(); }

    @DeleteMapping("/staff/{userId}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Integer userId,
                                            @RequestBody SuperAdminData.DeleteAccountRequest request,
                                            Authentication authentication) {
        superAdminDataService.deleteStaff(userId, authentication == null ? null : authentication.getName(),
                request == null ? null : request.confirmation());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/staff")
    public ResponseEntity<SuperAdminData.StaffResponse> createStaff(@RequestBody SuperAdminData.CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminDataService.createStaff(request));
    }
}
