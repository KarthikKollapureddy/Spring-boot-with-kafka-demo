package com.kafkaConsumer.notificationService.service;

import com.kafkaConsumer.notificationService.event.OrderEvent;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
    void sendNotification(OrderEvent orderEvent);
}
