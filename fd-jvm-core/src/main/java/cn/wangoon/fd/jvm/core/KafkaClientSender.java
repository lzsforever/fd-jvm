package cn.wangoon.fd.jvm.core;


import cn.wangoon.fd.jvm.core.constants.BaseConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import shade.cn.wangoon.fd.common.tracking.Metric;
import shade.cn.wangoon.fd.common.tracking.TraceLogging;
import shade.cn.wangoon.fd.common.util.FastJsonConvertUtil;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * <B>主类名称：</B>KafkaClientSender<BR>
 * <B>概要说明：</B>Kafka消息发送器<BR>
 *
 * @author Scott.Bai
 * @since 2022年4月25日 下午4:09:39
 */
@Slf4j
public class KafkaClientSender {
    private static final KafkaClientSender INSTANCE = new KafkaClientSender();
    public static KafkaClientSender getInstance() {
        return INSTANCE;
    }
    private final KafkaCollector kafkaCollector;

    public KafkaClientSender(String address) {
        this.kafkaCollector = new KafkaCollector(address);
        this.kafkaCollector.start();
    }

    public KafkaClientSender() {
        String kafkaBootstrapServer = AgentArgs.getValue("kafkaBootstrapServer");
        if ("".equals(kafkaBootstrapServer)) {
            kafkaBootstrapServer = "172.20.2.131:9092";
        }
        this.kafkaCollector = new KafkaCollector(kafkaBootstrapServer);
        this.kafkaCollector.start();
    }

    public void send(Metric metric) {
        try {
            kafkaCollector.sendAsync(metric.getDestination(), metric,
                    (metadata, exception) -> {
                        if (exception != null) {
                            log.error("#KafkaClientSender# callback exception, metric: {}, {}", metric, exception.getMessage());
                        }
                    }
            );
        } catch (Exception e) {
            log.error("#KafkaClientSender# send exception, metric: {}", metric.toString(), e);
        }
    }

    public void sendBatch(List<Metric> metricList) {
        for (Metric metric : metricList) {
            send(metric);
        }
    }

    public void shutdown() {
        this.kafkaCollector.shutdown();
    }

    static class KafkaCollector {

        /**
         * 每个发送批次大小为 16K
         **/
        private final int batchSize = 1024 * 16;
        /**
         * batch 没满的情况下默认等待 100 ms
         **/
        private final int lingerMs = 100;
        /**
         * producer 的缓存为 64M
         **/
        private final int bufferMemory = 1024 * 1024 * 64;
        /**
         * 需要确保写入副本 leader
         **/
        private final String acks = "1";
        /**
         * 为了减少带宽，使用 lz4 压缩
         **/
        private final String compressionType = "lz4";
        /**
         * 当 memory buffer 满了之后，send() 在抛出异常之前阻塞的最长时间
         **/
        private final int blockMs = 10000;

        private final String serializerClass = "org.apache.kafka.common.serialization.StringSerializer";

        private final Properties properties;

        private KafkaProducer<String, String> producer;

        public KafkaCollector(String address) {
            properties = new Properties();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, address);
            properties.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
            properties.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
            properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
            properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
            properties.put(ProducerConfig.ACKS_CONFIG, acks);
            properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, blockMs);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializerClass);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializerClass);
        }

        public void start() {
            this.producer = new KafkaProducer<>(properties);
        }

        /**
         * <B>方法名称：</B>sendSync<BR>
         * <B>概要说明：</B>同步发送<BR>
         *
         * @param topic      主题
         * @param routingKey 发送kafka时指定key，确保同一个metric都发送到同一个分区
         * @param message    消息
         * @return RecordMetadata
         * @throws InterruptedException
         * @throws ExecutionException
         * @author hezhuo.bai
         * @since 2020年5月12日 下午4:10:26
         */
        public RecordMetadata sendSync(String topic, String routingKey, Object message) throws InterruptedException, ExecutionException {
            Future<RecordMetadata> future = producer.send(
                    new ProducerRecord<>(topic, routingKey, FastJsonConvertUtil.convertObjectToJSON(message)));
            return future.get();
        }

        /**
         * <B>方法名称：</B>sendAsync<BR>
         * <B>概要说明：</B>异步发送<BR>
         *
         * @param topic    主题
         * @param message  消息
         * @param callback 异步回调
         * @throws InterruptedException
         * @author hezhuo.bai
         * @since 2020年5月12日 下午4:12:10
         */
        public void sendAsync(String topic, Metric message, Callback callback) {

            Objects.requireNonNull(topic);
            Objects.requireNonNull(message);

            String routingKey = TraceLogging.Agent.SERVICE_APPLICATION_NAME + BaseConstants.LINE_UNDERLINE + message.getName();
            producer.send(new ProducerRecord<>(topic,
                            routingKey,
                            FastJsonConvertUtil.convertObjectToJSON(message)),
                    callback);
        }

        public void shutdown() {
            this.producer.close();
        }
    }

}
