package org.example.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    @KafkaListener(topics = "hello-topic", groupId = "hello-topic-consumer")
    public void onEvent(@Payload String message) {
        System.out.println("Consumed message = " + message);
    }
}
