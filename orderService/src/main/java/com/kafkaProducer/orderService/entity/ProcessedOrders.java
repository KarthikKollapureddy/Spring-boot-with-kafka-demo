package com.kafkaProducer.orderService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProcessedOrders {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pk;
    private Long orderId;
    @Column(name = "order_process_date_time")
    private LocalDateTime orderProcessedDateTime;

    public ProcessedOrders(Long orderId, LocalDateTime orderProcessedDateTime) {
        this.orderId = orderId;
        this.orderProcessedDateTime = orderProcessedDateTime;
    }
}
