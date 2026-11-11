package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.OrderNotificationResponse;
import org.example.service.OrderNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Customer Notifications", description = "Endpoints for retrieving and acknowledging customer order tracking and fulfillment notifications")
@SecurityRequirement(name = "bearerAuth")
public class OrderNotificationController {
    private final OrderNotificationService notificationService;

    public OrderNotificationController(OrderNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "Get customer notifications",
            description = "Retrieve shipment tracking updates and order milestone notifications for the authenticated customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping
    public List<OrderNotificationResponse> notifications(Authentication authentication) {
        return notificationService.getNotifications(authentication.getName());
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Acknowledge and mark all unread notifications as read for the authenticated customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All notifications marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
