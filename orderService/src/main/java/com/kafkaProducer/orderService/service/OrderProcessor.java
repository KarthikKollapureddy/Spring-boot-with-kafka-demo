package com.kafkaProducer.orderService.service;

import com.kafkaProducer.orderService.dto.OrderRequestDTO;
import org.springframework.stereotype.Service;

@Service
public interface OrderProcessor {
    long processOrder(OrderRequestDTO order);
}
