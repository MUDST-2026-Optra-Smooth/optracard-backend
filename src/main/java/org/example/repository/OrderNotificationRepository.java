package org.example.repository;

import org.example.model.OrderNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderNotificationRepository extends JpaRepository<OrderNotification, Integer> {
    List<OrderNotification> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
