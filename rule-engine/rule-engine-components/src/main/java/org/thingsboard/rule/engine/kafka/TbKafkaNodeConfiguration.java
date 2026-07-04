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
package org.thingsboard.rule.engine.kafka;

import lombok.Data;
import org.apache.kafka.common.serialization.StringSerializer;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Map;

@Data
/**
 * Kafka 节点配置模型，保存 Kafka Producer 参数、Topic/key 模板和 Header 转换选项。
 * 配置类本身不直接创建 KafkaProducer、不执行异步回调，也不涉及数据库或缓存。
 */
public class TbKafkaNodeConfiguration implements NodeConfiguration<TbKafkaNodeConfiguration> {

    /**
     * Kafka Topic 模板，运行时基于 TbMsg 解析。
     */
    private String topicPattern;
    /**
     * Kafka Key 模板，空值表示发送无 key 记录。
     */
    private String keyPattern;
    /**
     * Kafka bootstrap.servers 配置。
     */
    private String bootstrapServers;
    /**
     * Producer retries 配置。
     */
    private int retries;
    /**
     * Producer batch.size 配置。
     */
    private int batchSize;
    /**
     * Producer linger.ms 配置。
     */
    private int linger;
    /**
     * Producer buffer.memory 配置。
     */
    private int bufferMemory;
    /**
     * Producer acks 配置，决定 Kafka 端确认语义。
     */
    private String acks;
    /**
     * Kafka key 序列化器类名。
     */
    private String keySerializer;
    /**
     * Kafka value 序列化器类名。
     */
    private String valueSerializer;
    /**
     * 透传给 Kafka Producer 的其它属性。
     */
    private Map<String, String> otherProperties;

    /**
     * 是否把 TbMsg 元数据作为 Kafka Headers 发送。
     */
    private boolean addMetadataKeyValuesAsKafkaHeaders;
    /**
     * 元数据写入 Kafka Headers 时使用的字符集名称。
     */
    private String kafkaHeadersCharset;

    /**
     * 构造 Kafka 节点默认配置。
     * 本方法仅设置默认值，不直接连接 Kafka，也不处理 Rule Engine 消息确认或失败路由。
     */
    @Override
    public TbKafkaNodeConfiguration defaultConfiguration() {
        TbKafkaNodeConfiguration configuration = new TbKafkaNodeConfiguration();
        configuration.setTopicPattern("my-topic");
        configuration.setBootstrapServers("localhost:9092");
        configuration.setRetries(0);
        configuration.setBatchSize(16384);
        configuration.setLinger(0);
        configuration.setBufferMemory(33554432);
        configuration.setAcks("-1");
        configuration.setKeySerializer(StringSerializer.class.getName());
        configuration.setValueSerializer(StringSerializer.class.getName());
        configuration.setOtherProperties(Collections.emptyMap());
        configuration.setAddMetadataKeyValuesAsKafkaHeaders(false);
        configuration.setKafkaHeadersCharset("UTF-8");
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类定义 Kafka 外部节点的 Producer 配置和消息映射选项，供 TbKafkaNode 在初始化和发送时读取。
 * 它不直接进行外部调用、异步回调、数据库、缓存或 Rule Engine Actor 操作。
 */
