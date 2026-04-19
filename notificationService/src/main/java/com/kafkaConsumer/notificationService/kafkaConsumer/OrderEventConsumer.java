package com.kafkaConsumer.notificationService.kafkaConsumer;

import com.kafkaConsumer.notificationService.dao.NotificationRepository;
import com.kafkaConsumer.notificationService.dto.StatusMessage;
import com.kafkaConsumer.notificationService.entity.Notification;
import com.kafkaConsumer.notificationService.event.OrderEvent;
import com.kafkaConsumer.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    private final NotificationRepository notificationRepository;
    /*
        order-events              ← original
        order-events-retry-0      ← retry 1
        order-events-retry-1      ← retry 2
        order-events-retry-2      ← retry 3
        order-events-dlt          ← dead letter
     */
    @RetryableTopic(
            attempts = "4",                          // 1 original + 3 retries
            backOff = @BackOff(delay = 1000, multiplier = 2.0),  // 1s, 2s, 4s
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "order-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    // public, not private — Spring proxies can't intercept private methods, @RetryableTopic needs to wrap this
    // Removed Acknowledgment param — @RetryableTopic manages acks internally (RECORD mode)
    public void kafkaOrderListener(
            @Payload OrderEvent orderEvent,
            @Header (KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header (KafkaHeaders.OFFSET) long offset
            ){
        log.info("Received order event: {} (Partition: {}, Offset: {})",
                orderEvent.id(), partition, offset);
        // No try-catch needed — @RetryableTopic handles failures automatically
        notificationService.sendNotification(orderEvent);
        log.info("Successfully processed order {}", orderEvent.id());

    }

    // Called when all retries are exhausted — message lands in DLT
    @Transactional
    @DltHandler
    public void handleDlt(
            @Payload OrderEvent orderEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        log.error("Message exhausted all retries, moved to DLT. Topic: {}, OrderID: {}",
                topic, orderEvent.id());
        Notification notification = Notification.builder()
                .orderId(orderEvent.id())
                .statusMessage(StatusMessage.FAILED)
                .errorMessage("All retries exhausted")
                .build();
        try {

        notificationRepository.save(notification);
        log.info("saved Failed notification to DB with order ID {}", orderEvent.id());
        } catch (Exception e){
            log.error("Failed to save notification info with order ID {}", orderEvent.id());
            throw new RuntimeException("Failed to save notification info "+e.getMessage());
        }
    }

}
