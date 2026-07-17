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
 * 1. `DefaultNotificationRulesCache` 是 ThingsBoard Application 中管理通知缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `NotificationRulesCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
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
