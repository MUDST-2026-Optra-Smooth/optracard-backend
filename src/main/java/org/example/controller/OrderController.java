package org.example.controller;

import org.example.dto.CreateOrderRequest;
import org.example.dto.CheckoutResponse;
import org.example.dto.OrderResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {
    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> history(Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderHistory(currentUser(authentication).getUaId()));
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> placeOrder(Authentication authentication, @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(currentUser(authentication).getUaId(), request));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
