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
package org.thingsboard.server.service.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`AlarmsCleanUpService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmsCleanUpService {

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    @Value("${sql.ttl.alarms.removal_batch_size}")
    private Integer removalBatchSize;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;
    private final AlarmDao alarmDao;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    private final AlarmService alarmService;
    private final RelationService relationService;
    /**
     * 实体，提供当前类调用的业务操作。
     */
    private final EntityActionService entityActionService;
    private final PartitionService partitionService;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    private final TbTenantProfileCache tenantProfileCache;

    /**
     * 功能：删除或清理`Up`。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.alarms.checking_interval})}", fixedDelayString = "${sql.ttl.alarms.checking_interval}")
    public void cleanUp() {
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                cleanUp(tenantId);
            } catch (Exception e) {
                log.warn("Failed to clean up alarms by ttl for tenant {}", tenantId, e);
            }
        }
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    private void cleanUp(TenantId tenantId) {
        if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
            return;
        }

        Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
        if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getAlarmsTtlDays() == 0) {
            return;
        }

        long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getAlarmsTtlDays());
        long expirationTime = System.currentTimeMillis() - ttl;

        PageLink removalBatchRequest = new PageLink(removalBatchSize, 0);
        long totalRemoved = 0;
        Set<String> typesToRemove = new HashSet<>();
        while (true) {
            PageData<AlarmId> toRemove = alarmDao.findAlarmsIdsByEndTsBeforeAndTenantId(expirationTime, tenantId, removalBatchRequest);
            for (AlarmId alarmId : toRemove.getData()) {
                Alarm alarm = alarmService.delAlarm(tenantId, alarmId, false).getAlarm();
                if (alarm != null) {
                    entityActionService.pushEntityActionToRuleEngine(alarm.getOriginator(), alarm, tenantId, null, ActionType.ALARM_DELETE, null);
                    totalRemoved++;
                    typesToRemove.add(alarm.getType());
                }
            }
            if (!toRemove.hasNext()) {
                break;
            }
        }

        alarmService.delAlarmTypes(tenantId, typesToRemove);

        if (totalRemoved > 0) {
            log.info("Removed {} outdated alarm(s) for tenant {} older than {}", totalRemoved, tenantId, new Date(expirationTime));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmsCleanUpService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
