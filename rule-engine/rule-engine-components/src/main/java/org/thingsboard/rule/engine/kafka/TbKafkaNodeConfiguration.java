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

/**
 * `TbKafkaNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbKafkaNodeConfiguration implements NodeConfiguration<TbKafkaNodeConfiguration> {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private String topicPattern;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String keyPattern;
    /**
     * `bootstrapServers` 字段，保存当前对象的对应属性。
     */
    private String bootstrapServers;
    /**
     * `retries` 字段，保存当前对象的对应属性。
     */
    private int retries;
    /**
     * 批量大小，用于控制处理规模或位置。
     */
    private int batchSize;
    /**
     * `linger` 字段，保存当前对象的对应属性。
     */
    private int linger;
    /**
     * `bufferMemory` 字段，保存当前对象的对应属性。
     */
    private int bufferMemory;
    /**
     * `acks` 字段，保存当前对象的对应属性。
     */
    private String acks;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String keySerializer;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private String valueSerializer;
    /**
     * `otherProperties`映射关系，用于按键查找对应值。
     */
    private Map<String, String> otherProperties;

    /**
     * 是否满足键条件。
     */
    private boolean addMetadataKeyValuesAsKafkaHeaders;
    /**
     * `kafkaHeadersCharset` 字段，保存当前对象的对应属性。
     */
    private String kafkaHeadersCharset;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
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
