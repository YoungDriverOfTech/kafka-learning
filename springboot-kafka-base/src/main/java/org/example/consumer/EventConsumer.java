package org.example.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
}
