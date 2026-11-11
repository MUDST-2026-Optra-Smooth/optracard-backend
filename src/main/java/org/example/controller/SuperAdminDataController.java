package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.SuperAdminData;
import org.example.service.SuperAdminDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Super Admin Executive", description = "Executive-level endpoints for platform oversight, staff provisioning, user administration, and financial transactions")
@SecurityRequirement(name = "bearerAuth")
public class SuperAdminDataController {
    private final SuperAdminDataService superAdminDataService;

    public SuperAdminDataController(SuperAdminDataService superAdminDataService) {
        this.superAdminDataService = superAdminDataService;
    }

    @Operation(
            summary = "Get platform executive overview",
            description = "Retrieve high-level KPIs including total platform sales, merchant stores, registered users, and system health. Requires SUPER_ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview KPIs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority")
    })
    @GetMapping("/overview")
    public SuperAdminData.OverviewResponse overview() { return superAdminDataService.getOverview(); }

    @Operation(
            summary = "List all merchant stores",
            description = "Retrieve comprehensive list of all registered seller stores on the platform."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stores list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority")
    })
    @GetMapping("/stores")
    public List<SuperAdminData.StoreResponse> stores() { return superAdminDataService.getStores(); }

    @Operation(
            summary = "Get merchant store details",
            description = "Retrieve full profile, owner identity, bank details, and metrics for a specific store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @GetMapping("/stores/{storeId}")
    public SuperAdminData.StoreResponse store(
            @Parameter(description = "Store ID", example = "1") @PathVariable Integer storeId) {
        return superAdminDataService.getStore(storeId);
    }

    @Operation(
            summary = "Get global product catalog",
            description = "Retrieve entire inventory catalog across both Official store and all Marketplace vendors."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Global catalog retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority")
    })
    @GetMapping("/catalog")
    public List<SuperAdminData.CatalogProductResponse> catalog() { return superAdminDataService.getCatalog(); }

    @Operation(
            summary = "List financial transactions",
            description = "Retrieve audit log of all completed purchases and financial transactions across the platform."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority")
    })
    @GetMapping("/transactions")
    public List<SuperAdminData.TransactionResponse> transactions() { return superAdminDataService.getTransactions(); }

    @Operation(
            summary = "List registered platform users",
            description = "Retrieve list of all registered customers and sellers with account statuses and roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority")
    })
    @GetMapping("/users")
    public List<SuperAdminData.UserResponse> users() { return superAdminDataService.getUsers(); }

    @Operation(
            summary = "Delete user account",
            description = "Permanently delete a customer or seller account with confirmation requirement."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User account deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid confirmation token"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", example = "10") @PathVariable Integer userId,
            @RequestBody SuperAdminData.DeleteAccountRequest request,
            Authentication authentication) {
        superAdminDataService.deleteUser(userId, authentication == null ? null : authentication.getName(),
                request == null ? null : request.confirmation());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List administrative staff",
            description = "Retrieve all staff members holding ADMIN or SUPER_ADMIN privileges."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority")
    })
    @GetMapping("/staff")
    public List<SuperAdminData.StaffResponse> staff() { return superAdminDataService.getStaff(); }

    @Operation(
            summary = "Delete staff account",
            description = "Revoke and permanently delete an administrator staff account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Staff account deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Missing confirmation or cannot delete self"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "Staff not found")
    })
    @DeleteMapping("/staff/{userId}")
    public ResponseEntity<Void> deleteStaff(
            @Parameter(description = "Staff user ID", example = "2") @PathVariable Integer userId,
            @RequestBody SuperAdminData.DeleteAccountRequest request,
            Authentication authentication) {
        superAdminDataService.deleteStaff(userId, authentication == null ? null : authentication.getName(),
                request == null ? null : request.confirmation());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create new staff administrator",
            description = "Provision a new staff member with ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Staff account created successfully"),
            @ApiResponse(responseCode = "400", description = "Duplicate username or email, or invalid payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN authority")
    })
    @PostMapping("/staff")
    public ResponseEntity<SuperAdminData.StaffResponse> createStaff(@RequestBody SuperAdminData.CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdminDataService.createStaff(request));
    }
}
