/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.queue.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbProperty;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by ashvayka on 25.09.18.
 */
/**
 * 中文说明：
 * 1. `TbKafkaSettings` 是 ThingsBoard Common Queue 中描述 `Tb Kafka` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Slf4j
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
@ConfigurationProperties(prefix = "queue.kafka")
@Component
public class TbKafkaSettings {

    /**
     * `servers` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.bootstrap.servers}")
    private String servers;

    /**
     * 是否启用 SSL。
     */
    @Value("${queue.kafka.ssl.enabled:false}")
    private boolean sslEnabled;

    /**
     * SSL，表示当前对象的对应属性。
     */
    @Value("${queue.kafka.ssl.truststore.location:}")
    private String sslTruststoreLocation;

    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${queue.kafka.ssl.truststore.password:}")
    private String sslTruststorePassword;

    /**
     * SSL，表示当前对象的对应属性。
     */
    @Value("${queue.kafka.ssl.keystore.location:}")
    private String sslKeystoreLocation;

    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${queue.kafka.ssl.keystore.password:}")
    private String sslKeystorePassword;

    /**
     * 密码，用于定位映射、配置或数据项。
     */
    @Value("${queue.kafka.ssl.key.password:}")
    private String sslKeyPassword;

    /**
     * `acks` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.acks:all}")
    private String acks;

    /**
     * `retries` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.retries:1}")
    private int retries;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Value("${queue.kafka.compression.type:none}")
    private String compressionType;

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    @Value("${queue.kafka.batch.size:16384}")
    private int batchSize;

    /**
     * `lingerMs` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.linger.ms:1}")
    private long lingerMs;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${queue.kafka.max.request.size:1048576}")
    private int maxRequestSize;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${queue.kafka.max.in.flight.requests.per.connection:5}")
    private int maxInFlightRequestsPerConnection;

    /**
     * `bufferMemory` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.buffer.memory:33554432}")
    private long bufferMemory;

    /**
     * `replicationFactor` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.replication_factor:1}")
    @Getter
    private short replicationFactor;

    /**
     * `maxPollRecords` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.max_poll_records:8192}")
    private int maxPollRecords;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${queue.kafka.max_poll_interval_ms:300000}")
    private int maxPollIntervalMs;

    /**
     * 分区，用于定位消息或数据所属分区。
     */
    @Value("${queue.kafka.max_partition_fetch_bytes:16777216}")
    private int maxPartitionFetchBytes;

    /**
     * `fetchMaxBytes` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.fetch_max_bytes:134217728}")
    private int fetchMaxBytes;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${queue.kafka.request.timeout.ms:30000}")
    private int requestTimeoutMs;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${queue.kafka.session.timeout.ms:10000}")
    private int sessionTimeoutMs;

    /**
     * 偏移量，用于控制数量、位置或分页范围。
     */
    @Value("${queue.kafka.auto_offset_reset:earliest}")
    private String autoOffsetReset;

    /**
     * 是否使用`confluent`。
     */
    @Value("${queue.kafka.use_confluent_cloud:false}")
    private boolean useConfluent;

    /**
     * SSL，表示当前对象的对应属性。
     */
    @Value("${queue.kafka.confluent.ssl.algorithm:}")
    private String sslAlgorithm;

    /**
     * `saslMechanism` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.confluent.sasl.mechanism:}")
    private String saslMechanism;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Value("${queue.kafka.confluent.sasl.config:}")
    private String saslConfig;

    /**
     * `securityProtocol` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.confluent.security.protocol:}")
    private String securityProtocol;

    /**
     * `otherInline` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.kafka.other-inline:}")
    private String otherInline;

    /**
     * `other`列表，用于保存一组待处理对象。
     */
    @Deprecated
    @Setter
    private List<TbProperty> other;

    @Setter
    private Map<String, List<TbProperty>> consumerPropertiesPerTopic = Collections.emptyMap();

    /**
     * 功能：执行 `toAdminProps` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public Properties toAdminProps() {
        Properties props = toProps();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(AdminClientConfig.RETRIES_CONFIG, retries);

        return props;
    }

    /**
     * 功能：执行 `toConsumerProps` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    public Properties toConsumerProps(String topic) {
        Properties props = toProps();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        consumerPropertiesPerTopic
                .getOrDefault(topic, Collections.emptyList())
                .forEach(kv -> props.put(kv.getKey(), kv.getValue()));
        return props;
    }

    /**
     * 功能：执行 `toProducerProps` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public Properties toProducerProps() {
        Properties props = toProps();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection);
        return props;
    }

    /**
     * 功能：执行 `toProps` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    Properties toProps() {
        Properties props = new Properties();

        if (useConfluent) {
            props.put("ssl.endpoint.identification.algorithm", sslAlgorithm);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config", saslConfig);
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        }

        props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        props.put(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);

        props.putAll(PropertyUtils.getProps(otherInline));

        if (other != null) {
            other.forEach(kv -> props.put(kv.getKey(), kv.getValue()));
        }

        configureSSL(props);

        return props;
    }

    /**
     * 功能：执行 `configureSSL` 对应的处理。
     * 参数：
     * - `props`：`props` 参数。
     * 返回：无。
     */
    void configureSSL(Properties props) {
        if (sslEnabled) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
        }
    }

}
