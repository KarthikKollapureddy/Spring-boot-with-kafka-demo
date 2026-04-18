package com.kafkaProducer.orderService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders_info")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
//    @Column(name = "products")
    private List<String> products;
    private Float amount;
    private LocalDateTime paymentDateTime;

    public Orders(List<String> products, Float amount, LocalDateTime paymentDateTime) {
        this.products = products;
        this.amount = amount;
        this.paymentDateTime = paymentDateTime;
    }
}
