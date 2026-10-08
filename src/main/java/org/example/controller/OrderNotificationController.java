package org.example.controller;

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
public class OrderNotificationController {
    private final OrderNotificationService notificationService;

    public OrderNotificationController(OrderNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<OrderNotificationResponse> notifications(Authentication authentication) {
        return notificationService.getNotifications(authentication.getName());
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
