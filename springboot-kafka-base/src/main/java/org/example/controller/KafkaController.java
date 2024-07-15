package org.example.controller;

import org.example.producer.EventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaController {

    private final EventProducer kafkaProducer;

    @Autowired
    public KafkaController(EventProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping("/send/{message}")
    public String sendMessage(@PathVariable("message") String message) throws Exception {
        kafkaProducer.sendEvent(message);
        return message;
    }
}
