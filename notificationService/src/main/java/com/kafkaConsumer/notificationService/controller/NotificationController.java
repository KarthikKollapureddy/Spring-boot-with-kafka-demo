package com.kafkaConsumer.notificationService.controller;

import com.kafkaConsumer.notificationService.dao.NotificationRepository;
import com.kafkaConsumer.notificationService.dto.StatusMessage;
import com.kafkaConsumer.notificationService.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }

    /**
     * Get notification status by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getNotificationStatus(@PathVariable Long orderId) {
        Optional<Notification> notification = notificationRepository.findByOrderId(orderId);

        if (notification.isPresent()) {
            return ResponseEntity.ok(notification.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all notifications (for debugging)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    /**
     * Get notifications by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Notification>> getNotificationsByStatus(
            @PathVariable String status) {

        List<Notification> notifications = notificationRepository.findAll()
                .stream()
                .filter(n -> n.getStatusMessage().equals(StatusMessage.valueOf(status)))
                .toList();

        return ResponseEntity.ok(notifications);
    }

}
