package com.kafkaProducer.orderService.dto;

import java.util.List;

public record OrderRequestDTO(List<String> products, Float amount, String orderProcessingDateTime, String customerEmail) {

}
