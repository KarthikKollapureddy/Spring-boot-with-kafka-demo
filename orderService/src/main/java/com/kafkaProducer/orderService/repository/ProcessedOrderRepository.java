package com.kafkaProducer.orderService.repository;

import com.kafkaProducer.orderService.entity.ProcessedOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrders, UUID> {

    Optional<ProcessedOrders> findProcessedOrderByOrderId(long orderId);
}
