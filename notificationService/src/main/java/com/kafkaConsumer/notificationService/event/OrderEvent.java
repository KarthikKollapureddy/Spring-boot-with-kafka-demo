package com.kafkaConsumer.notificationService.event;

import java.time.LocalDateTime;
import java.util.List;

public record OrderEvent(
        Long id,
        List<String> products,
        Float amount,
        LocalDateTime paymentDateTime,
        String customerEmail
) {
}
