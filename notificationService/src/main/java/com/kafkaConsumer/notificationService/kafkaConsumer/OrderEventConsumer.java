package com.kafkaConsumer.notificationService.kafkaConsumer;

import com.kafkaConsumer.notificationService.event.OrderEvent;
import com.kafkaConsumer.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.internals.Acknowledgements;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "order-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    private void kafkaOrderListener(
            @Payload OrderEvent orderEvent,
            Acknowledgment acks,
            @Header (KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header (KafkaHeaders.OFFSET) long offset
            ){
        log.info("Received order event: {} (Partition: {}, Offset: {})",
                orderEvent.id(), partition, offset);
        try{
            notificationService.sendNotification(orderEvent);
            acks.acknowledge();
            log.info("Successfully processed and acknowledged order {}", orderEvent.id());
        } catch (Exception e){
            log.error("Error processing order {}: {}", orderEvent.id(), e.getMessage(), e);
            // Don't acknowledge - message will be redelivered to consumer group
        }
    }

}
