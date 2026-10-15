package org.example.service;

import org.example.model.Order;
import org.example.model.OrderNotification;
import org.example.model.User;
import org.example.repository.OrderNotificationRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceTest {

    @Mock private OrderNotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    @Test
    void createShippingNotification_persistsTheBuyersTrackingUpdate() {
        Order order = new Order();
        order.setOrderId(41);
        order.setUserId(7);
        order.setCreatedAt(LocalDateTime.of(2026, 10, 8, 10, 30));
        order.setTrackingNumber("TH123456789");

        service().createShippingNotification(order);

        ArgumentCaptor<OrderNotification> notification = ArgumentCaptor.forClass(OrderNotification.class);
        verify(notificationRepository).save(notification.capture());
        assertEquals(7, notification.getValue().getUserId());
        assertEquals(41, notification.getValue().getOrderId());
        assertEquals("ORD-20261008-0041", notification.getValue().getOrderNumber());
        assertEquals("TH123456789", notification.getValue().getTrackingNumber());
        assertTrue(notification.getValue().getMessage().contains("TH123456789"));
    }

    @Test
    void getNotifications_returnsTheCustomersReadState() {
        User user = user(7);
        OrderNotification unread = notification(1, null);
        OrderNotification read = notification(2, LocalDateTime.of(2026, 10, 8, 11, 0));
        when(userRepository.findByEmail("buyer@example.test")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7)).thenReturn(List.of(unread, read));

        var notifications = service().getNotifications("buyer@example.test");

        assertEquals(2, notifications.size());
        assertFalse(notifications.get(0).read());
        assertTrue(notifications.get(1).read());
        assertEquals("TH123456789", notifications.get(0).trackingNumber());
    }

    @Test
    void markAllRead_marksOnlyUnreadNotifications() {
        User user = user(7);
        OrderNotification unread = notification(1, null);
        OrderNotification read = notification(2, LocalDateTime.of(2026, 10, 8, 11, 0));
        when(userRepository.findByEmail("buyer@example.test")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(7)).thenReturn(List.of(unread, read));

        service().markAllRead("buyer@example.test");

        assertNotNull(unread.getReadAt());
        assertEquals(LocalDateTime.of(2026, 10, 8, 11, 0), read.getReadAt());
        verify(notificationRepository).saveAll(List.of(unread));
    }

    private OrderNotificationService service() {
        return new OrderNotificationService(notificationRepository, userRepository);
    }

    private User user(int id) {
        User user = new User();
        user.setUaId(id);
        return user;
    }

    private OrderNotification notification(int id, LocalDateTime readAt) {
        OrderNotification notification = new OrderNotification();
        notification.setId(id);
        notification.setOrderId(41);
        notification.setOrderNumber("ORD-20261008-0041");
        notification.setMessage("Order ORD-20261008-0041 has shipped. Tracking number: TH123456789");
        notification.setTrackingNumber("TH123456789");
        notification.setReadAt(readAt);
        return notification;
    }
}
