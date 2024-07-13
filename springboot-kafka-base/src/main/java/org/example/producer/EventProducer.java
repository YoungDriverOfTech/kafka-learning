package org.example.producer;

import jakarta.annotation.Resource;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class EventProducer {

    // 加入spring-kafka依赖+配置文件，会自动装配KafkaTemplate到IOC容器中
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息，通过传递参数指定topic
     * @param message
     */
    public void sendEvent(String message) throws Exception {
        CompletableFuture<SendResult<String, String>> future
                = kafkaTemplate.send("hello-topic", message);

        // 拿到发送的结果

        // 1. 阻塞等待拿结果
        SendResult<String, String> sendResult = future.get();
        if(sendResult.getRecordMetadata() != null) {
            // kafka确认接收到了额消息
            System.out.println("发送成功" + sendResult.getRecordMetadata().toString());
            System.out.println("消息本身" + sendResult.getProducerRecord().toString());
        }

        // 2. 非阻塞式拿结果 (thenAccept/thenRun/thenApply)
        future.thenAccept((t) -> {
            if(t.getRecordMetadata() != null) {
                // kafka确认接收到了额消息
                System.out.println("异步发送成功" + t.getRecordMetadata().toString());
                System.out.println("异步消息本身" + t.getProducerRecord().toString());
            }
        }).exceptionally((t) -> {
            // 出异常了
            t.printStackTrace();
            return null;
        });
    }

    /**
     * 通过Message对象发送消息，指定topic的时候，设置header
     * @param message
     */
    public void sendEventThroughMessage(String message) {
        Message<String> messageObj = MessageBuilder.withPayload(message)
                        .setHeader(KafkaHeaders.TOPIC, "hello-topic") // 设置topic
                        .build();
        kafkaTemplate.send(messageObj);
    }

    /**
     * 发送消息，通过ProducerRecord
     * @param message
     */
    public void sendEventThroughProducerRecord(String message) {
        // 存放一些信息，供消费消费者使用
        Headers headers = new RecordHeaders();
        headers.add("wahaha", "huiyuanguozhi".getBytes());

        // new 出ProducerRecord对象
        ProducerRecord<String, String> record = new ProducerRecord<>(
                "hello-topid", // topic
                0, // partition
                System.currentTimeMillis(), // 该record的时间
                "key1", // key
                message, // value
                headers // 生产者提供的额外信息
        );
        kafkaTemplate.send(record);
    }

    /**
     * 发送消息到默认topic，通过ProducerRecord
     * @param message
     */
    public void sendToDefaultTopic(String message) {
        /**
         *     template:
         *       # 配置模版默认的主题，如果使用sendDefault方法的话，会发到这个topic里面
         *       default-topic: default-topic
         */
        kafkaTemplate.sendDefault(
                0, // 0 partition
                System.currentTimeMillis(),
                "key",
                "value"
        );
    }
}
