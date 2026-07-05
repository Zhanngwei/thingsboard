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
package org.thingsboard.server.service.notification.rule.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.notification.NotificationRuleService;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultNotificationRulesCache` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationRulesCache implements NotificationRulesCache {

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationRuleService notificationRuleService;

    /**
     * `cacheMaxSize` 字段，保存当前对象的对应属性。
     */
    @Value("${cache.notificationRules.maxSize:1000}")
    private int cacheMaxSize;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Value("${cache.notificationRules.timeToLiveInMinutes:30}")
    private int cacheValueTtl;
    private Cache<String, List<NotificationRule>> cache;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheValueTtl, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        switch (event.getEntityId().getEntityType()) {
            case NOTIFICATION_RULE:
                evict(event.getTenantId()); // TODO: evict by trigger type of the rule
                break;
            case TENANT:
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    lock.writeLock().lock(); // locking in case rules for tenant are fetched while evicting
                    try {
                        evict(event.getTenantId());
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
                break;
        }
    }

    /**
     * 功能：获取`Enabled`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `triggerType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationRule> getEnabled(TenantId tenantId, NotificationRuleTriggerType triggerType) {
        lock.readLock().lock();
        try {
            log.trace("Retrieving notification rules of type {} for tenant {} from cache", triggerType, tenantId);
            return cache.get(key(tenantId, triggerType), k -> {
                List<NotificationRule> rules = notificationRuleService.findEnabledNotificationRulesByTenantIdAndTriggerType(tenantId, triggerType);
                log.trace("Fetched notification rules of type {} for tenant {} (count: {})", triggerType, tenantId, rules.size());
                return !rules.isEmpty() ? rules : Collections.emptyList();
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    public void evict(TenantId tenantId) {
        cache.invalidateAll(Arrays.stream(NotificationRuleTriggerType.values())
                .map(triggerType -> key(tenantId, triggerType))
                .collect(Collectors.toList()));
        log.trace("Evicted all notification rules for tenant {} from cache", tenantId);
    }

    /**
     * 功能：执行 `key` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `triggerType`：类型。
     * 返回：文本结果。
     */
    private static String key(TenantId tenantId, NotificationRuleTriggerType triggerType) {
        return tenantId + "_" + triggerType;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNotificationRulesCache` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
