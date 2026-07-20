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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `TbKafkaConsumerStatsService` 是 ThingsBoard Common Queue 中负责统计数据的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
public class TbKafkaConsumerStatsService {
    private final Set<String> monitoredGroups = ConcurrentHashMap.newKeySet();

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaConsumerStatisticConfig statsConfig;

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private PartitionService partitionService;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private AdminClient adminClient;
    private Consumer<String, byte[]> consumer;
    /**
     * 调度器，用于安排延迟任务或周期任务。
     */
    private ScheduledExecutorService statsPrintScheduler;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        if (!statsConfig.getEnabled()) {
            return;
        }
        this.adminClient = AdminClient.create(kafkaSettings.toAdminProps());
        this.statsPrintScheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("kafka-consumer-stats"));

        Properties consumerProps = kafkaSettings.toConsumerProps(null);
        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-stats-loader-client");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-stats-loader-client-group");
        this.consumer = new KafkaConsumer<>(consumerProps);

        startLogScheduling();
    }

    /**
     * 功能：初始化或启动`Log Scheduling`。
     * 参数：无。
     * 返回：无。
     */
    private void startLogScheduling() {
        Duration timeoutDuration = Duration.ofMillis(statsConfig.getKafkaResponseTimeoutMs());
        statsPrintScheduler.scheduleWithFixedDelay(() -> {
            if (!isStatsPrintRequired()) {
                return;
            }
            for (String groupId : monitoredGroups) {
                try {
                    Map<TopicPartition, OffsetAndMetadata> groupOffsets = adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
                            .get(statsConfig.getKafkaResponseTimeoutMs(), TimeUnit.MILLISECONDS);
                    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(groupOffsets.keySet(), timeoutDuration);

                    List<GroupTopicStats> lagTopicsStats = getTopicsStatsWithLag(groupOffsets, endOffsets);
                    if (!lagTopicsStats.isEmpty()) {
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < lagTopicsStats.size(); i++) {
                            builder.append(lagTopicsStats.get(i).toString());
                            if (i != lagTopicsStats.size() - 1) {
                                builder.append(", ");
                            }
                        }
                        log.info("[{}] Topic partitions with lag: [{}].", groupId, builder.toString());
                    }
                } catch (Exception e) {
                    log.warn("[{}] Failed to get consumer group stats. Reason - {}.", groupId, e.getMessage());
                    log.trace("Detailed error: ", e);
                }
            }

        }, statsConfig.getPrintIntervalMs(), statsConfig.getPrintIntervalMs(), TimeUnit.MILLISECONDS);
    }

    /**
     * 功能：判断`Stats Print Required`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isStatsPrintRequired() {
        boolean isMyRuleEnginePartition = partitionService.resolve(ServiceType.TB_RULE_ENGINE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
        boolean isMyCorePartition = partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
        return log.isInfoEnabled() && (isMyRuleEnginePartition || isMyCorePartition);
    }

    /**
     * 功能：获取`Topics Stats With Lag`。
     * 参数：
     * - `groupOffsets`：偏移量。
     * - `endOffsets`：偏移量。
     * 返回：匹配的数据集合。
     */
    private List<GroupTopicStats> getTopicsStatsWithLag(Map<TopicPartition, OffsetAndMetadata> groupOffsets, Map<TopicPartition, Long> endOffsets) {
        List<GroupTopicStats> consumerGroupStats = new ArrayList<>();
        for (TopicPartition topicPartition : groupOffsets.keySet()) {
            long endOffset = endOffsets.get(topicPartition);
            long committedOffset = groupOffsets.get(topicPartition).offset();
            long lag = endOffset - committedOffset;
            if (lag != 0) {
                GroupTopicStats groupTopicStats = GroupTopicStats.builder()
                        .topic(topicPartition.topic())
                        .partition(topicPartition.partition())
                        .committedOffset(committedOffset)
                        .endOffset(endOffset)
                        .lag(lag)
                        .build();
                consumerGroupStats.add(groupTopicStats);
            }
        }
        return consumerGroupStats;
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `groupId`：`groupId`ID。
     * 返回：无。
     */
    public void registerClientGroup(String groupId) {
        if (statsConfig.getEnabled() && !StringUtils.isEmpty(groupId)) {
            monitoredGroups.add(groupId);
        }
    }

    /**
     * 功能：执行 `unregisterClientGroup` 对应的处理。
     * 参数：
     * - `groupId`：`groupId`ID。
     * 返回：无。
     */
    public void unregisterClientGroup(String groupId) {
        if (statsConfig.getEnabled() && !StringUtils.isEmpty(groupId)) {
            monitoredGroups.remove(groupId);
        }
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (statsPrintScheduler != null) {
            statsPrintScheduler.shutdownNow();
        }
        if (adminClient != null) {
            adminClient.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }


    /**
     * 中文说明：
     * 1. `GroupTopicStats` 是 ThingsBoard Common Queue 中围绕统计数据提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Builder
    @Data
    private static class GroupTopicStats {
        /**
         * 主题，用于匹配或发送对应主题的数据。
         */
        private String topic;
        private int partition;
        /**
         * 偏移量，用于控制数量、位置或分页范围。
         */
        private long committedOffset;
        private long endOffset;
        /**
         * `lag` 字段，保存当前对象的对应属性。
         */
        private long lag;

        /**
         * 功能：生成当前对象的文本表示。
         * 参数：无。
         * 返回：文本结果。
         */
        @Override
        public String toString() {
            return "[" +
                    "topic=[" + topic + ']' +
                    ", partition=[" + partition + "]" +
                    ", committedOffset=[" + committedOffset + "]" +
                    ", endOffset=[" + endOffset + "]" +
                    ", lag=[" + lag + "]" +
                    "]";
        }
    }
}
