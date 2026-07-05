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

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.util.StopWatch;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by ashvayka on 24.09.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`TbKafkaConsumerTemplate` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbKafkaConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<ConsumerRecord<String, byte[]>, T> {

    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final KafkaConsumer<String, byte[]> consumer;
    /**
     * 解码器，表示当前对象的对应属性。
     */
    private final TbKafkaDecoder<T> decoder;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbKafkaConsumerStatsService statsService;
    private final String groupId;

    /**
     * 功能：创建 `TbKafkaConsumerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `settings`：配置对象。
     * - `decoder`：`decoder` 参数。
     * - `clientId`：客户端ID。
     * - `groupId`：`groupId`ID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    private TbKafkaConsumerTemplate(TbKafkaSettings settings, TbKafkaDecoder<T> decoder,
                                    String clientId, String groupId, String topic,
                                    TbQueueAdmin admin, TbKafkaConsumerStatsService statsService) {
        super(topic);
        Properties props = settings.toConsumerProps(topic);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        this.statsService = statsService;
        this.groupId = groupId;

        if (statsService != null) {
            statsService.registerClientGroup(groupId);
        }

        this.admin = admin;
        this.consumer = new KafkaConsumer<>(props);
        this.decoder = decoder;
    }

    /**
     * 功能：执行 `doSubscribe` 对应的处理。
     * 参数：
     * - `topicNames`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    protected void doSubscribe(List<String> topicNames) {
        if (!topicNames.isEmpty()) {
            topicNames.forEach(admin::createTopicIfNotExists);
            consumer.subscribe(topicNames);
        } else {
            log.info("unsubscribe due to empty topic list");
            consumer.unsubscribe();
        }
    }

    /**
     * 功能：执行 `doPoll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：处理结果。
     */
    @Override
    protected List<ConsumerRecord<String, byte[]>> doPoll(long durationInMillis) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.trace("poll topic {} maxDuration {}", getTopic(), durationInMillis);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(durationInMillis));

        stopWatch.stop();
        log.trace("poll topic {} took {}ms", getTopic(), stopWatch.getTotalTimeMillis());

        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ConsumerRecord<String, byte[]>> recordList = new ArrayList<>(256);
            records.forEach(recordList::add);
            return recordList;
        }
    }

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `record`：`record` 参数。
     * 返回：处理结果。
     */
    @Override
    public T decode(ConsumerRecord<String, byte[]> record) throws IOException {
        return decoder.decode(new KafkaTbQueueMsg(record));
    }

    /**
     * 功能：执行 `doCommit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doCommit() {
        consumer.commitSync();
    }

    /**
     * 功能：执行 `doUnsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doUnsubscribe() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
        if (statsService != null) {
            statsService.unregisterClientGroup(groupId);
        }
    }

    /**
     * 功能：判断`Long Polling Supported`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isLongPollingSupported() {
        return true;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbKafkaConsumerTemplate` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
