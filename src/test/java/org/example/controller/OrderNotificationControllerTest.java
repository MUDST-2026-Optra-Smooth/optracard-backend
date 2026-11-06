package org.example.controller;

import org.example.dto.OrderNotificationResponse;
import org.example.service.OrderNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderNotificationControllerTest {

    @Mock
    private OrderNotificationService notificationService;

    @InjectMocks
    private OrderNotificationController controller;

    @Test
    @DisplayName("notifications returns list of user notifications")
    void notifications_success() {
        Authentication auth = new UsernamePasswordAuthenticationToken("customer@example.com", "pass");
        List<OrderNotificationResponse> mockList = List.of(
                new OrderNotificationResponse(1, 101, "ORD-123", "Order placed", "TRK-999", LocalDateTime.now(), false)
        );
        when(notificationService.getNotifications("customer@example.com")).thenReturn(mockList);

        List<OrderNotificationResponse> result = controller.notifications(auth);

        assertEquals(1, result.size());
        assertEquals("ORD-123", result.get(0).orderNumber());
        verify(notificationService).getNotifications("customer@example.com");
    }

    @Test
    @DisplayName("markAllRead invokes service and returns 204 No Content")
    void markAllRead_success() {
        Authentication auth = new UsernamePasswordAuthenticationToken("customer@example.com", "pass");

        ResponseEntity<Void> response = controller.markAllRead(auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(notificationService).markAllRead("customer@example.com");
    }
}

