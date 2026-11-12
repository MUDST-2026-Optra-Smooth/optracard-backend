package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.StockAlertResponse;
import org.example.dto.StockAlertSettingsRequest;
import org.example.dto.StockAlertSettingsResponse;
import org.example.service.StockAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock-alerts")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Stock Alerts", description = "Endpoints for inventory threshold monitoring, low/out-of-stock notifications, and notification settings")
@SecurityRequirement(name = "bearerAuth")
public class StockAlertController {
    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @Operation(
            summary = "Get active stock alerts",
            description = "Retrieve low stock and out-of-stock alert notifications scoped to seller's store or admin official inventory."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock alerts list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER or ADMIN role")
    })
    @GetMapping
    public List<StockAlertResponse> alerts(Authentication authentication) {
        return stockAlertService.getAlerts(authentication.getName());
    }

    @Operation(
            summary = "Mark all stock alerts read",
            description = "Mark all active stock alert notifications as read for authenticated merchant or admin."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All stock alerts marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER or ADMIN role")
    })
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        stockAlertService.markAllRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Mark stock alert as read",
            description = "Mark a single stock alert notification as read by alert ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock alert marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER or ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Stock alert not found")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            Authentication authentication,
            @Parameter(description = "Stock alert ID", example = "1") @PathVariable Integer id) {
        stockAlertService.markRead(authentication.getName(), id, true);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Mark stock alert as unread",
            description = "Revert a single stock alert notification to unread status by alert ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock alert marked as unread successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER or ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Stock alert not found")
    })
    @PutMapping("/{id}/unread")
    public ResponseEntity<Void> markUnread(
            Authentication authentication,
            @Parameter(description = "Stock alert ID", example = "1") @PathVariable Integer id) {
        stockAlertService.markRead(authentication.getName(), id, false);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get stock alert settings",
            description = "Retrieve current inventory low-stock alert thresholds for seller store or official inventory."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock alert settings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER or ADMIN role")
    })
    @GetMapping("/settings")
    public StockAlertSettingsResponse settings(Authentication authentication) {
        return stockAlertService.getSettings(authentication.getName());
    }

    @Operation(
            summary = "Update stock alert threshold settings",
            description = "Update low-stock quantity threshold trigger value for the store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock alert settings updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid threshold value (must be >= 0)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER or ADMIN role")
    })
    @PutMapping("/settings")
    public StockAlertSettingsResponse updateSettings(
            Authentication authentication,
            @RequestBody StockAlertSettingsRequest request) {
        return stockAlertService.updateSettings(authentication.getName(), request);
    }
}
