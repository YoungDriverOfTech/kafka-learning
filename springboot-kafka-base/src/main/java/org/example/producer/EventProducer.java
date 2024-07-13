package org.example.producer;

import jakarta.annotation.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    // 加入spring-kafka依赖+配置文件，会自动装配KafkaTemplate到IOC容器中
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String message) {
        kafkaTemplate.send("hello-topic", "wahaha");
    }
}
