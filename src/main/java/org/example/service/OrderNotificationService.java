package org.example.service;

import org.example.dto.OrderNotificationResponse;
import org.example.model.Order;
import org.example.model.OrderNotification;
import org.example.model.User;
import org.example.repository.OrderNotificationRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderNotificationService {
    private final OrderNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public OrderNotificationService(OrderNotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createShippingNotification(Order order) {
        String orderNumber = orderNumber(order);
        OrderNotification notification = new OrderNotification();
        notification.setUserId(order.getUserId());
        notification.setOrderId(order.getOrderId());
        notification.setOrderNumber(orderNumber);
        notification.setTrackingNumber(order.getTrackingNumber());
        notification.setMessage("Order " + orderNumber + " has shipped. Tracking number: " + order.getTrackingNumber());
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<OrderNotificationResponse> getNotifications(String email) {
        User user = currentUser(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getUaId()).stream()
                .limit(20)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markAllRead(String email) {
        User user = currentUser(email);
        List<OrderNotification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getUaId()).stream()
                .filter(notification -> notification.getReadAt() == null)
                .toList();
        if (unread.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(notification -> notification.setReadAt(now));
        notificationRepository.saveAll(unread);
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private OrderNotificationResponse toResponse(OrderNotification notification) {
        return new OrderNotificationResponse(notification.getId(), notification.getOrderId(),
                notification.getOrderNumber(), notification.getMessage(), notification.getTrackingNumber(),
                notification.getCreatedAt(), notification.getReadAt() != null);
    }

    private String orderNumber(Order order) {
        String date = order.getCreatedAt() == null ? LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                : order.getCreatedAt().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "ORD-" + date + "-" + String.format("%04d", order.getOrderId());
    }
}
