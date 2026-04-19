package com.kafkaProducer.orderService.service;

import com.kafkaProducer.orderService.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<Long, OrderEvent> kafkaTemplate;
    @Value("${spring.kafka.topic.name}")
    private String topicName;

    /**
     * Publishes an OrderEvent to Kafka topic with retry mechanism and detailed logging
     *
     * Flow:
     * 1. Log the sending attempt
     * 2. Send message asynchronously using kafkaTemplate
     * 3. Use whenComplete() callback to handle both success and failure scenarios
     * 4. Log success with partition/offset details or error details on failure
     *
     * Retry Mechanism:
     * - Configured in application.yaml: retries: 3
     * - Producer automatically retries up to 3 times if send fails
     * - Acks=all ensures durability before returning
     * - enable-idempotence prevents duplicate messages even if retries occur
     *
     * @param orderEvent The order event to publish (contains orderId, products, amount, paymentDateTime)
     * @throws RuntimeException if exception occurs during sending
     */
    public void sendOrderEvent(OrderEvent orderEvent) {
        try {
//            kafkaTemplate.send(
//                    topicName,
//                    orderEvent.id(),
//                    orderEvent
//            );
//            Better approach below:
            log.info("🔔 Sending OrderEvent to Kafka topic: {} - OrderID: {}", topicName, orderEvent.id());

            // ============= SEND TO KAFKA (ASYNCHRONOUS) =============
            // kafkaTemplate.send() returns CompletableFuture<SendResult<Long, OrderEvent>>
            // Sends immediately returns (non-blocking), actual sending happens in background
            // Key (orderEvent.id()): Long value used for partitioning - same orderId always goes to same partition
            // Value (orderEvent): The actual OrderEvent object serialized as JSON
            // Topic (topicName): "order-events" - configured in application.yaml

            // ============= KAFKA TRANSACTION =============
            // Problem solved: Without transactions, DB save + Kafka send are independent operations.
            // If DB save succeeds but Kafka send fails → order is saved but no event is published
            // → consumer never gets notified → customer never receives email → inconsistent state.
            //
            // executeInTransaction wraps the send in a Kafka transaction:
            //   1. Calls beginTransaction() on the broker
            //   2. Sends the message (buffered, invisible to consumers until commit)
            //   3. On success → commitTransaction() → message becomes visible atomically
            //   4. On failure → abortTransaction() → message is discarded
            //
            // Requires: transaction-id-prefix in application.yaml (enables transactional producer)
            // Consumer must use isolation.level=read_committed to only see committed messages
            // Return value is required by the API but unused — true is a placeholder
            kafkaTemplate.executeInTransaction(ops->{
                ops.send(topicName, orderEvent.id(), orderEvent)
                        .whenComplete((result, ex)->{
                            if (result != null && ex == null) {
                                log.info(
                                        "✅ Successfully published OrderEvent to topic: {} - " +
                                                "OrderID: {}, Partition: {}, Offset: {}, Timestamp: {}",
                                        topicName,
                                        orderEvent.id(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset(),
                                        result.getRecordMetadata().timestamp()
                                );
                            } else if (ex != null) {
                                log.info(
                                        "❌ Failed to publish OrderEvent to topic: {} - OrderID: {}, Error: {}",
                                        topicName,
                                        orderEvent.id(),
                                        ex.getMessage(),
                                        ex
                                );
                            }
                        });
                return true;
            });

//            kafkaTemplate.send(topicName, orderEvent.id(), orderEvent)
//                    // ============= HANDLE RESPONSE WITH whenComplete() =============
//                    // whenComplete() is a callback that executes when send completes (success or failure)
//                    // Parameters:
//                    //   - result: SendResult<Long, OrderEvent> containing metadata (partition, offset, timestamp) on success
//                    //   - ex: Throwable exception if send failed (null on success)
//                    //
//                    // Why whenComplete() instead of blocking?
//                    // - Non-blocking: Application continues, Kafka send happens in background
//                    // - Async: Callback fires when send completes (or fails)
//                    // - Efficient: Multiple messages can be sent without waiting for each one
//                    .whenComplete((result, ex) -> {
//                        // ============= SUCCESS CASE: ex == null && result != null =============
//                        // Kafka accepted the message and assigned it a partition/offset
//                        if (ex == null && result != null) {
//                            // Log success with detailed metadata:
//                            // - partition: Which partition message was assigned to (0, 1, 2, etc.)
//                            //   Useful for debugging: same orderId should always go to same partition
//                            // - offset: Message's position in the partition
//                            //   Can be used for tracking message consumption
//                            // - timestamp: Server-side timestamp when Kafka received the message
//                            log.info("✅ Successfully published OrderEvent to topic: {} - " +
//                                    "OrderID: {}, Partition: {}, Offset: {}, Timestamp: {}",
//                                    topicName,
//                                    orderEvent.id(),
//                                    result.getRecordMetadata().partition(),
//                                    result.getRecordMetadata().offset(),
//                                    result.getRecordMetadata().timestamp()
//                            );
//                        }
//                        // ============= FAILURE CASE: ex != null =============
//                        // Kafka rejected the message or network error occurred
//                        // Note: Retry logic (configured in application.yaml) happens BEFORE this callback
//                        // If send fails after 3 retries, then this callback fires with exception
//                        else if (ex != null) {
//                            // Log error with:
//                            // - Topic: Where we tried to send
//                            // - OrderID: Which order failed
//                            // - Error message: Human-readable error
//                            // - Full exception: Stack trace for debugging
//                            // After 3 retries fail, this is logged to indicate delivery failure
//                            log.error("❌ Failed to publish OrderEvent to topic: {} - OrderID: {}, Error: {}",
//                                    topicName,
//                                    orderEvent.id(),
//                                    ex.getMessage(),
//                                    ex
//                            );
//                        }
//                    });
        } catch (Exception e) {
            // ============= EXCEPTION HANDLING (OUTER TRY-CATCH) =============
            // Catches exceptions that occur BEFORE sending (e.g., invalid input)
            // This is different from whenComplete() failure which is DURING/AFTER sending
            //
            // Examples of exceptions caught here:
            // - NullPointerException: If orderEvent is null
            // - IllegalArgumentException: If orderEvent fields are invalid
            // - Configuration errors: If kafkaTemplate is not properly initialized
            log.error("❌ Exception encountered when sending event to topic {} with error {}", topicName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
