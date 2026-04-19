package com.kafkaProducer.orderService.service;

import com.kafkaProducer.orderService.dto.OrderRequestDTO;
import com.kafkaProducer.orderService.entity.Orders;
import com.kafkaProducer.orderService.entity.ProcessedOrders;
import com.kafkaProducer.orderService.event.OrderEvent;
import com.kafkaProducer.orderService.repository.OrdersRepository;
import com.kafkaProducer.orderService.repository.ProcessedOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessorImpl implements OrderProcessor {

    private final OrdersRepository ordersRepository;
    private final ProcessedOrderRepository processedOrderRepository;
    private final OrderProducer orderProducer;

    /**
     * DateTimeFormatter for ISO-8601 format: "2026-04-18T19:45:31.383Z"
     *
     * Pattern explanation:
     *   yyyy-MM-dd  → Year-Month-Day (2026-04-18)
     *   'T'         → Literal T separator (quotes mean literal char, not pattern)
     *   HH:mm:ss    → Hours:Minutes:Seconds (19:45:31)
     *   .SSS        → Dot + Milliseconds (383)
     *   'Z'         → Literal Z (UTC indicator)
     *
     * Usage: LocalDateTime.parse("2026-04-18T19:45:31.383Z", ISO_FORMATTER)
     * Result: LocalDateTime object ready for database storage
     */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    );

    @Override
    public long processOrder(OrderRequestDTO order) {
        // Parse ISO-8601 datetime: "2026-04-18T19:45:31.383Z" → LocalDateTime
        // Using ISO_FORMATTER for explicit, controlled datetime parsing
        LocalDateTime orderDateTime = LocalDateTime.parse(order.orderProcessingDateTime(), DateTimeFormatter.ISO_DATE_TIME);
        Orders receivedOrder = Orders.builder()
                .products(order.products())
                .paymentDateTime(orderDateTime)
                .customerEmail(order.customerEmail())
                .amount(order.amount()).build();
        long orderID = saveOrderToDB(receivedOrder);
        if (orderID > 0) {
            ProcessedOrders processedOrders = ProcessedOrders.builder()
                    .orderProcessedDateTime(orderDateTime)
                    .orderId(orderID)
                    .build();
            saveProcessOrderToDB(processedOrders);
            log.info("starting processing order with id {}", orderID);
            try {
                OrderEvent orderEvent = new OrderEvent(
                        orderID,
                        receivedOrder.getProducts(),
                        receivedOrder.getAmount(),
                        receivedOrder.getPaymentDateTime(),
                        receivedOrder.getCustomerEmail()
                );

                log.info("--------Sending to kafka-----------");
                orderProducer.sendOrderEvent(orderEvent);
                return orderID;
            } catch (Exception e) {
                log.info("failed to publish event with ID: {}", orderID);
                return -1;
            }
        } else {
            return -1;
        }

    }

    @Transactional
    public long saveOrderToDB(Orders order) {
        try {
            Orders savedOrder = ordersRepository.save(order);
            log.info("Successfully saved order with ID: {}", savedOrder.getId());
            return savedOrder.getId();
        } catch (Exception e) {
            log.error("failed to save the order with exception : {}", e.getMessage());
        }
        return -1;
    }

    @Transactional
    public long saveProcessOrderToDB(ProcessedOrders order) {
        try {
            ProcessedOrders processedOrder = processedOrderRepository.save(order);
            log.info("Successfully saved processedOrder with ID: {}", processedOrder.getOrderId());
            return processedOrder.getOrderId();
        } catch (Exception e) {
            log.error("failed to save the processedOrder with exception : {}", e.getMessage());
            return -1;
        }
    }
}
