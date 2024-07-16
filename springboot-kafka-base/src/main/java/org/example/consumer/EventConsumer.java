package org.example.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventConsumer {

    @KafkaListener(topics = "hello-topic", groupId = "hello-topic-consumer")
    public void onEvent(@Payload String message,
                        @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(value = KafkaHeaders.RECEIVED_KEY) String key,
                        @Header(value = KafkaHeaders.RECEIVED_PARTITION) String partition,
                        ConsumerRecord<Object, Object> record) {
        System.out.println("Consumed message = " + message);
        System.out.println("topic = " + topic);
        System.out.println("key = " + key);
        System.out.println("partition = " + partition);
        System.out.println("record = " + record);
    }

    @KafkaListener(topics = "hello-topic", groupId = "hello-topic-consumer")
    public void onEvent04(@Payload String message, Acknowledgment ack) {

        // 开启手动确认消息是否已经被消费了(默认自动确认)
        System.out.println("Confirmed message: " + message);
        ack.acknowledge();
    }

    @KafkaListener(groupId = "${kafka.consumer.group}",
        topicPartitions = {
            @TopicPartition(
                    topic = "${kafka.topic.name}",
                    partitions = {"0", "1", "2"},
                    partitionOffsets = {
                            @PartitionOffset(partition = "3", initialOffset = "3"),
                            @PartitionOffset(partition = "4", initialOffset = "4")
                    }
            )
        }
    )
    public void onEvent05(@Payload String message, Acknowledgment ack) {

        // 开启手动确认消息是否已经被消费了(默认自动确认)
        System.out.println("Confirmed message: " + message);
        ack.acknowledge();
    }

    @KafkaListener(topics = "hello-topic", groupId = "hello-topic-consumer")
    public void onEvent06(List<ConsumerRecords<Object, Object>> list, Acknowledgment ack) {

        // 开启手动确认消息是否已经被消费了(默认自动确认)
        System.out.println("Confirmed message: " + list.toString());
        ack.acknowledge();
    }
}
