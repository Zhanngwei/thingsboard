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
package org.thingsboard.server.queue.usagestats;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import javax.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. `DefaultTbApiUsageReportClient` 是 ThingsBoard Common Queue 中访问用量统计的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 直接依赖的类型边界包括 `TbApiUsageReportClient`。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultTbApiUsageReportClient implements TbApiUsageReportClient {

    /**
     * 是否启用`enabled`。
     */
    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;
    /**
     * 是否启用客户。
     */
    @Value("${usage.stats.report.enabled_per_customer:false}")
    private boolean enabledPerCustomer;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${usage.stats.report.interval:10}")
    private int interval;

    private final EnumMap<ApiUsageRecordKey, ConcurrentMap<ReportLevel, AtomicLong>> stats = new EnumMap<>(ApiUsageRecordKey.class);

    /**
     * 分区，提供当前类调用的业务操作。
     */
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 调度器，用于安排延迟任务或周期任务。
     */
    private final SchedulerComponent scheduler;
    private final TbQueueProducerProvider producerProvider;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgProducer;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        if (enabled) {
            msgProducer = this.producerProvider.getTbUsageStatsMsgProducer();
            for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                stats.put(key, new ConcurrentHashMap<>());
            }
            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    reportStats();
                } catch (Exception e) {
                    log.warn("Failed to report statistics: ", e);
                }
            }, new Random().nextInt(interval), interval, TimeUnit.SECONDS);
        }
    }

    /**
     * 功能：上报`Stats`。
     * 参数：无。
     * 返回：无。
     */
    private void reportStats() {
        ConcurrentMap<ParentEntity, ToUsageStatsServiceMsg.Builder> report = new ConcurrentHashMap<>();

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            ConcurrentMap<ReportLevel, AtomicLong> statsForKey = stats.get(key);
            statsForKey.forEach((reportLevel, statsValue) -> {
                long value = statsValue.get();
                if (value == 0 && key.isCounter()) return;

                ToUsageStatsServiceMsg.Builder statsMsg = report.computeIfAbsent(reportLevel.getParentEntity(), parent -> {
                    ToUsageStatsServiceMsg.Builder newStatsMsg = ToUsageStatsServiceMsg.newBuilder();

                    TenantId tenantId = parent.getTenantId();
                    newStatsMsg.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
                    newStatsMsg.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());

                    CustomerId customerId = parent.getCustomerId();
                    if (customerId != null) {
                        newStatsMsg.setCustomerIdMSB(customerId.getId().getMostSignificantBits());
                        newStatsMsg.setCustomerIdLSB(customerId.getId().getLeastSignificantBits());
                    }

                    newStatsMsg.setServiceId(serviceInfoProvider.getServiceId());
                    return newStatsMsg;
                });

                UsageStatsKVProto.Builder statsItem = UsageStatsKVProto.newBuilder()
                        .setKey(key.name())
                        .setValue(value);
                statsMsg.addValues(statsItem.build());
            });
            statsForKey.clear();
        }

        report.forEach(((parent, statsMsg) -> {
            //TODO: figure out how to minimize messages into the queue. Maybe group by 100s of messages?
            try {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, parent.getTenantId(), parent.getId())
                        .newByTopic(msgProducer.getDefaultTopic());
                msgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), statsMsg.build()), null);
            } catch (TenantNotFoundException e) {
                log.debug("Couldn't report usage stats for non-existing tenant: {}", e.getTenantId());
            } catch (Exception e) {
                log.warn("Failed to report usage stats for tenant {}", parent.getTenantId(), e);
            }
        }));

        if (!report.isEmpty()) {
            log.debug("Reporting API usage statistics for {} tenants and customers", report.size());
        }
    }

    /**
     * 功能：执行 `report` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key, long value) {
        if (!enabled) return;

        ReportLevel[] reportLevels = new ReportLevel[3];
        reportLevels[0] = ReportLevel.of(tenantId);
        if (key.isCounter()) {
            reportLevels[1] = ReportLevel.of(TenantId.SYS_TENANT_ID);
        }
        if (enabledPerCustomer && customerId != null && !customerId.isNullUid()) {
            reportLevels[2] = ReportLevel.of(tenantId, customerId);
        }
        report(key, value, reportLevels);
    }

    /**
     * 功能：执行 `report` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `key`：键。
     * 返回：无。
     */
    @Override
    public void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key) {
        report(tenantId, customerId, key, 1);
    }

    /**
     * 功能：执行 `report` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * - `levels`：`levels` 参数。
     * 返回：无。
     */
    private void report(ApiUsageRecordKey key, long value, ReportLevel... levels) {
        ConcurrentMap<ReportLevel, AtomicLong> statsForKey = stats.get(key);
        for (ReportLevel level : levels) {
            if (level == null) continue;

            AtomicLong n = statsForKey.computeIfAbsent(level, k -> new AtomicLong());
            if (key.isCounter()) {
                n.addAndGet(value);
            } else {
                n.set(value);
            }
        }
    }

    /**
     * 中文说明：
     * 1. `ReportLevel` 是 ThingsBoard Common Queue 中围绕 `Report Level` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Data
    private static class ReportLevel {
        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final CustomerId customerId;

        /**
         * 功能：执行 `of` 对应的处理。
         * 参数：
         * - `tenantId`：租户IDID。
         * 返回：处理结果。
         */
        public static ReportLevel of(TenantId tenantId) {
            return new ReportLevel(tenantId, null);
        }

        /**
         * 功能：执行 `of` 对应的处理。
         * 参数：
         * - `tenantId`：租户IDID。
         * - `customerId`：客户IDID。
         * 返回：处理结果。
         */
        public static ReportLevel of(TenantId tenantId, CustomerId customerId) {
            return new ReportLevel(tenantId, customerId);
        }

        /**
         * 功能：获取实体。
         * 参数：无。
         * 返回：处理结果。
         */
        public ParentEntity getParentEntity() {
            return new ParentEntity(tenantId, customerId);
        }

    }

    /**
     * 中文说明：
     * 1. `ParentEntity` 是 ThingsBoard Common Queue 中表示实体持久化结构的实体类型。
     * 2. 它保存与存储表或查询结果对应的字段。
     * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
     * 4. 它直接协作于领域模型、实体映射器和存取实现。
     * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
     * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
     */
    @Data
    private static class ParentEntity {
        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final CustomerId customerId;

        /**
         * 功能：获取`Id`。
         * 参数：无。
         * 返回：处理结果。
         */
        public EntityId getId() {
            return customerId != null ? customerId : tenantId;
        }
    }

}
