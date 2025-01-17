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

## 6. 消费者
### 6.1 获取生产者发送的消息 @Payload注解
下面代码的Payload注解是用来获得消息体的注解
```java
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

```

### 6.2 获取消息头 @Header
下面代码展示了从哪个topic，的那个partition，获得的消息，有什么key
```java
package org.example.consumer;

import org.springframework.kafka.annotation.KafkaListener;
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
                        @Header(value = KafkaHeaders.RECEIVED_PARTITION) String partition) {
        System.out.println("Consumed message = " + message);
        System.out.println("topic = " + topic);
        System.out.println("key = " + key);
        System.out.println("partition = " + partition);
    }
}

```

### 6.3 使用record对象获取信息
```java
package org.example.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
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
}
```

### 6.4 手动确认消息模式
```diff
spring:
  kafka:
    # Kafka broker addresses
    bootstrap-servers: localhost:9092
    # Producer, 27 config in total
    producer:
      # 默认StringSerializer.class, 默认的序列化类，不能序列化对象，意味着不能发送对象到kafka
      key-serializer: com.fasterxml.jackson.databind.JsonSerializer # 用来序列化对象
      value-serializer: com.fasterxml.jackson.databind.JsonSerializer # 用来序列化对象
    # Consumer, 24 config intotal
    consumer:
      # 从最开的位置读取消息
      auto-offset-reset: earliest
    # 配置监听器
+   listener:
+     # 开启手动确认消息模式
+     ack-mode: manual
    template:
      # 配置模版默认的主题，如果使用sendDefault方法的话，会发到这个topic里面
      default-topic: default-topic
```
```java
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
    public void onEvent04(@Payload String message, Acknowledgment ack) {
        
        // 开启手动确认消息是否已经被消费了(默认自动确认)
        System.out.println("Confirmed message: " + message);
        ack.acknowledge(); // 如果不执行这句代码，代表消息没有没确认 -> 消息没有被消费。 有可能消息会被重复消费
    }
}

```

### 6.5 指定分区，指定偏移量消费
```java
package org.example.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

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
}
```

### 6.6 批量消费
kafka默认是单条消费，通过下面的配置可以实现批量消费  
```diff
spring:
  kafka:
    # Kafka broker addresses
    bootstrap-servers: localhost:9092
    # Producer, 27 config in total
    producer:
      # 默认StringSerializer.class, 默认的序列化类，不能序列化对象，意味着不能发送对象到kafka
      key-serializer: com.fasterxml.jackson.databind.JsonSerializer # 用来序列化对象
      value-serializer: com.fasterxml.jackson.databind.JsonSerializer # 用来序列化对象
    # Consumer, 24 config intotal
    consumer:
      # 从最开的位置读取消息
+     auto-offset-reset: earliest
      # 批量消费时候，每次拉去多少条记录
+     max-poll-records: 20
    # 配置监听器
    listener:
      # 开启手动确认消息模式
      ack-mode: manual
      # 开启批量消费
+     type: batch
      
    template:
      # 配置模版默认的主题，如果使用sendDefault方法的话，会发到这个topic里面
      default-topic: default-topic

kafka:
  consumer:
    group: hello-topic-group
  topic:
    name: hello-topic
```

```java
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
    public void onEvent06(List<ConsumerRecords<Object, Object>> list, Acknowledgment ack) {

        // 开启手动确认消息是否已经被消费了(默认自动确认)
        System.out.println("Confirmed message: " + list.toString());
        ack.acknowledge();
    }
}
```

### 6.7 消费拦截器
- 实现ConsumerInterceptor拦截器接口
- 在ConsumerFactory配置中注册这个拦截器
  - props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, CustomizeInterceptor.class.getName())

### 6.8 消费消息的分区策略


## 7. 消息转发
### 7.1 转发
从topic A收到消息后，经过处理然后再发送到topic B上
```java
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
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventConsumer {
    @KafkaListener(topics = "hello-topic", groupId = "hello-topic-consumer")
    @SendTo(value = "topic-B")
    public String onEvent07(List<ConsumerRecords<Object, Object>> list) {

        // 开启手动确认消息是否已经被消费了(默认自动确认)
        System.out.println("Confirmed message: " + list.toString());
        return list.get(0) + "wahaha";
    }
}
```

## 8. 消费消息分区策略
### 8.1 分类
- RangeAssignor：默认的分区策略，平均分，比如有10个分区，3个消费者，那么会分成4-3-3. （10 / 3 = 3，余数给第一个消费者）
- RoundRobinAssignor：轮询
- StickyAssignor：亲和度策略
- CooperativeStickyAssignor：合作

### 8.2 RangeAssignor例子
因为是默认的分区策略，所以只需要完成消费者个数演示就可以  
concurrency = "3"属性就代表一共有几个消费者
```java
@Component
public class EventConsumer {

  @KafkaListener(topics = "hello-topic", groupId = "hello-topic-consumer", concurrency = "3")
  public void onEvent06(List<ConsumerRecords<Object, Object>> list, Acknowledgment ack) {

    // 开启手动确认消息是否已经被消费了(默认自动确认)
    System.out.println("Confirmed message: " + list.toString());
    ack.acknowledge();
  }

}

```
### 8.3 RoundRobinAssignor轮询

```diff
public Map<String, Object> producerConfigs() {
  Map<String, Object> props = new HashMap<>();
  // 指定消费分区策略
+ props.put(ConsumerConfig,PARTITION_ASSIGNMENT_STRATEGY_CONFIG, RoundRobinAssignor.class.getName());
  return props;
}
```

## 9. Kafka消息存储
### 9.1 消息的存储
- Kafka的所有消息都存储在**/tmp/kafka-logs**的目录中，可以通过log.dirs=/tmp/kafka-logs配置
- kafka的所有消息都是以日志文件的方式来保存
- kafka一般都是海量的消息数据，为了避免日志文件过大，日志文件都被存放在多个日志目录下面，日志目录的命名规则为：<topic_name>-<partition_id>
- 比如创建一个名字是firstTopic的topic，其中有3个partition，那么在kafka的数据目录（/tmp/kafka-logs）中，就有3个目录firstTopic-0,firstTopic-1,firstTopic-2
  - 0000000000000.index 消息索引文件
  - 0000000000000.log 消息数据文件
  - 0000000000000.timeindex 消息时间戳索引文件
  - 0000000000000.snapshot 快照文件，生产者发生故障或者重起时能够回复并继续之前的操作
  - leader-epoch-checkpoint 记录每个分区当前领导者的epoch以及领导者开始写入消息时的起始偏移量
  - partition.metadata 存储关于特定分区的元数据信息

### 9.2 offset的存储
- 每次消费一个消息并且提交以后，会保存当前消费到的最近的一个offset
- 在kafka中，有一个__consumer_offsets的topic，消费者消费提交的offset信息会写入到该topic中，__consumer_offsets保存了每个consumer group某一时刻提交的offset信息，默认有50个分区
- consumer_group保存在哪个分区中的计算公式
  - Math.abs("groupId" .hashCode() % groupMetadataTopicPartitionCount)

## 10. offset详解
### 10.1 生产者offset详解
- 生产者发送一条消息到kafka的broker的某个topic下某个partition中
- kafka内部会为每条消息分配一个唯一的offset，该offset就是该消息在partition中的位置


### 10.2 消费者offset（最新被消费掉的消息的下一个位置）
- 消费者offset是消费者需要知道自己已经读取到了哪个位置了，接下来需要从哪个位置开始继续读取消息
- 每个消费者组（consumer group）中的消费者都会独立地维护自己的offset，当消费者从某个partition读取消息时，它会几率当前读取到的offset，这样即使消费者崩溃或者重启，它叶可以从上次读取的位置继续读取，而不会重复读取或者遗漏消息
- 注意：消费者offset需要消费消息并提交后才记录offset
- 每个消费者启动开始监听消息，默认从消息的最新的位置开始监听消息，即把最新的位置作为消费者offset
  - 分区中还没有发送过消息，则最新的位置就是0
  - 分区中已经发送过消息，则最新的位置就是生产者offset的下一个位置
- 消费者消费消息后，如果不提交确认（ack），则offset不更新，提交了才更新
- 结论：消费者从什么位置开始消费，就看消费者的offset是多少，消费者启动后它的offset是多少


## 11. 架构
### 11.1 架构图
![img.png](img.png)

### 11.2 ISR副本
- ISR副本：在同步中的副本(In-Sync Replicas)，包含了Leader副本和所有与Leader副本保持同步的Follower副本
- 写请求首先由Leader副本处理，之后Follower副本会从Leader上拉取写入的消息，这个过程会有一定的延迟，导致Follwer副本中保存的消息略少于Leader副本，但是只要没有超出阈值都可以容忍，但是如果一个Follower副本出现异常，比如宕机，网络断开灯原因长时间没有同步到消息，那这个时候，Leader就回把它踢出去，Kafka通过ISR集合来维护一个<可用且消息量与Leader相差不同的副本集合，它是整个副本集合的一个子集>
- 在kafka中，一个副本要成为ISR副本，需要满足一定的条件
  - Leader副本本身就是一个ISR副本
  - Follower副本最后一条消息的offset与Leader副本的最后一条消息的offset之间的差值不能超过指定阈值，超过阈值则该Follower副本将会从ISR列表中移除
    - replica.lag.time.max.ms:默认是30s，如果该Follower在此时间间隔内一只没有追上过Leader副本的所有消息，则改Follower副本就回呗踢出ISR列表
    - replica.lag.max.messages:落后了多少条消息时，该Follower副本就回呗踢出ISR列表，改配置在最新版本的kafka中已经过时

### 11.3 LEO 日志末端偏移量
- LEO(Log End Offset)，记录该副本消息日志中下一条消息的票一亮，注意是下一条消息，也就是说，如果LEO=10，那么表示该副本之保存了偏移量值是[0, 9]的10条消息

### 11.4 高水位值
- HW(High Watermark)即高水位值，它代表一个便宜量offset信息，表示消息的复制进度，也就是消息已经成功复制到了哪个位置了？即在HW之前的所有消息都已经被成功写入副本中并且可以在所有的副本中找到，因为，消费者可以安全的消费这些已经成功复制的消息
- 对于同一个副本而言，小于等于HW值的所有消息都被人无视已备份的（replicated），消费者只能拉取到这个offset之前的消息，确保了数据的可靠性
