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

### 


