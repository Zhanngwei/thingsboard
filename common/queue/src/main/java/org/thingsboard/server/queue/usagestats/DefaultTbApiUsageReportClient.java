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
 * 1. 类目的：`DefaultTbApiUsageReportClient` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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
     * 1. 类目的：`ReportLevel` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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
     * 1. 类目的：`ParentEntity` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbApiUsageReportClient` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
