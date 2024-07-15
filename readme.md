# Kafka note
## 1. Frequently used commands
如果不带参数直接执行shell，会出现使用提示

### 1.1 查询/创建/删除主题，
```shell
./kafka-topics.sh --bootstrap-server localhost:9092 --list
./kafka-topics.sh --bootstrap-server localhost:9092 --create --topic hello
./kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic hello
```

### 1.2 显示主题详细信息
```shell
./kafka-topics.sh --describe --topic quickstart-events --bootstrap-server localhost:9092
```

### 1.3 修改主题(比如修改分区数)
```shell
—-alter
```

### 1.4 写入消息
```shell
./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic quickstart-events
```

### 1.5 消费消息
```shell
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic quickstart-events --from-beginning
```

## 2. Springboot kafka的相关配置
都可以在这个类里面找到 **KafkaProperties**

## 3. 消费者的默认消费位置
### 3.1 offset相关
默认情况，启动一个新的消费者组的时候，会从每个分区的**最新偏移量**（即该分区中最后一条消息的下一个位置开始消费）  
如果希望从第一条消息开始消费，需要将消费者的auto.offset.reset设置为earliest

<span style="color: red">PS:</span> 如果之前使用相同的消费者组ID消费过该主题，并且kafka已经保存了该消费者组的偏移量，  
那么即使设置了auto.offset.reset=earliest，也不会生效。因为kafka只会在找不到偏移量的时候才会使用这个配置，在这种情况下，需要手动充值偏移量或者  
使用一个新的消费者组ID

### 3.2 充值偏移量
```shell
# 重制到最早
./kafka-consumer-groups.sh --bootstrap-server 127.0.0.1 --group hello-topic-group --topic hello-topic --reset-offsets --to-earliest --execute

# 还可以根据时间什么的重制
```

### 3.3 消费偏移量策略
```yaml
spring:
  kafka:
    consumer:
      # 消费偏移量策略
      # earliest： 自动将偏移量重置为最早的偏移量
      # latest： 自动将偏移量重置为最新的偏移量
      # none： 如果没有为消费者组找到以前的偏移量，则向消费者抛出异常
      # exception： 向消费者抛出异常（Spring-kafka不支持）
      auto-offset-reset: earliest
```

## 4. Replica
### 4.1 副本
Replica：副本，为了实现备份功能，保证集群中的某个节点发生故障时，该节点上的partition数据不丢失，且kafka仍然能够继续工作，kafka提供了副本机制，一个topic的每个分区都有一个或者多个副本  

Replica副本分为Leader Replica和Follower Replica:
- Leader：每个分区多个副本中的主副本，，生产者发送数据/消费者消费数据都是针对leader副本
- Follow：只从leader副本中同步数据

> 设置副本个数不能为0，也不能大于节点个数，否则将不能创建Topic

<span style="color: red; font-weight: bold">创建副本的时候，副本的数量不能大于broker的数据</span> 

### 4.2 命令行指定副本
```shell
./kafka-topics.sh --create --topic-name mytopic --partition 3 --replica-factor 3 --bootstrap-servers localhost:9092
```

### 4.3 代码指定副本
```java
package org.example.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    
    @Bean
    public NewTopic newTopic() {
        // topic name, partition number, replica number
        return new NewTopic("my-topic", 5, (short)1);
    }
}
```

## 5. 生产者
### 5.1 生产者发送消息的分区策略
- 生产者写入消息到topic，kafka根据不同的策略将数据分配到不同的分区中
- 1,默认分配策略 BuiltInPartitioner
  - 有key：Utils.toPositive(Utils.murmur2(serializedKey)) % numPartitions
  - 没有key：使用随机数 % numPartitions
- 2,轮询分配策略：RoundRobinPartitioner， 接口（Partitioner）
- 3,自定义分配策略：自己定义

### 5.2 消息发送流程
KafkaProducer -> ProducerInterceptors -> Serializer -> Partitioner -> Topic

### 5.3 自定义生产者拦截器
实现ProducerInterceptor接口，实现onSend和onAcknowledgement方法。 然后在配置类中配置拦截器即可

