package com.kafkaConsumer.notificationService.dao;

import com.kafkaConsumer.notificationService.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByOrderId(long orderId);
    boolean existsByOrderId(Long orderId);
}
