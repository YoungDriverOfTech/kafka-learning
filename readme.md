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

