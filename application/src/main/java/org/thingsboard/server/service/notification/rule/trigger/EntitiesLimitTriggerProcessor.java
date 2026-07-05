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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.EntitiesLimitNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntitiesLimitTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * 中文说明：
 * 1. 类目的：`EntitiesLimitTriggerProcessor` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@RequiredArgsConstructor
public class EntitiesLimitTriggerProcessor implements NotificationRuleTriggerProcessor<EntitiesLimitTrigger, EntitiesLimitNotificationRuleTriggerConfig> {

    /**
     * 实体，提供当前类调用的业务操作。
     */
    private final EntityCountService entityCountService;
    private final TbTenantProfileCache tenantProfileCache;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;

    /**
     * 功能：执行 `matchesFilter` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean matchesFilter(EntitiesLimitTrigger trigger, EntitiesLimitNotificationRuleTriggerConfig triggerConfig) {
        if (isNotEmpty(triggerConfig.getEntityTypes()) && !triggerConfig.getEntityTypes().contains(trigger.getEntityType())) {
            return false;
        }
        DefaultTenantProfileConfiguration profileConfiguration = tenantProfileCache.get(trigger.getTenantId()).getDefaultProfileConfiguration();
        long limit = profileConfiguration.getEntitiesLimit(trigger.getEntityType());
        if (limit <= 0) {
            return false;
        }
        long currentCount = entityCountService.countByTenantIdAndEntityType(trigger.getTenantId(), trigger.getEntityType());
        if (currentCount == 0) {
            return false;
        }
        trigger.setLimit(limit);
        trigger.setCurrentCount(currentCount);
        return (int) (limit * triggerConfig.getThreshold()) == currentCount; // strict comparing not to send notification on each new entity
    }

    /**
     * 功能：执行 `constructNotificationInfo` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EntitiesLimitTrigger trigger) {
        return EntitiesLimitNotificationInfo.builder()
                .entityType(trigger.getEntityType())
                .currentCount(trigger.getCurrentCount())
                .limit(trigger.getLimit())
                .percents((int) (((float) trigger.getCurrentCount() / trigger.getLimit()) * 100))
                .tenantId(trigger.getTenantId())
                .tenantName(tenantService.findTenantById(trigger.getTenantId()).getName())
                .build();
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITIES_LIMIT;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EntitiesLimitTriggerProcessor` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
