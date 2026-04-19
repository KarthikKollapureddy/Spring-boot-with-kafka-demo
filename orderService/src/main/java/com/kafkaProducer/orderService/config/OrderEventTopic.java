package com.kafkaProducer.orderService.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic Configuration
 *
 * WHY multiple partitions?
 * By default, Kafka auto-creates topics with 1 partition when a producer first sends to them.
 * 1 partition = only 1 consumer thread can read from the topic = no parallelism.
 *
 * By explicitly defining the topic with multiple partitions:
 * - We enable parallel consumption (up to N consumers for N partitions)
 * - The producer's key (orderId) is hashed to a partition: same orderId → same partition
 * - This guarantees ordering per order while allowing different orders to be processed in parallel
 *
 * Partition count should match consumer concurrency (setConcurrency in KafkaConsumerConfig)
 * for maximum throughput.
 */
@Configuration
public class OrderEventTopic {

    @Value("${spring.kafka.topic.name}")
    private String topicName;
    @Value("${spring.kafka.topic.partitioncount}")
    private int partitionCount;
    @Value("${spring.kafka.topic.replicas}")
    private int topicReplicas;

    @Bean
    public NewTopic orderEventsTopic(){
        return TopicBuilder.name(topicName)
                .partitions(partitionCount)   // 3 partitions → up to 3 parallel consumers
                .replicas(topicReplicas)      // must be <= number of brokers in the cluster
                .build();
    }
}
