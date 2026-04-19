package com.kafkaConsumer.notificationService.entity;

import com.kafkaConsumer.notificationService.dto.StatusMessage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long orderId;
    @EnumeratedValue
    private StatusMessage statusMessage;
    private String recipientEmail;
    private LocalDateTime sentAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    @Column(length = 500)
    private String errorMessage;

}
