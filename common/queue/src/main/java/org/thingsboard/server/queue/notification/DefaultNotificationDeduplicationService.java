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
package org.thingsboard.server.queue.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.SOFT;

/**
 * 中文说明：
 * 1. `DefaultNotificationDeduplicationService` 是 ThingsBoard Common Queue 中负责通知的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `NotificationDeduplicationService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationDeduplicationService implements NotificationDeduplicationService {

    /**
     * `deduplicationDurations`映射关系，用于按键查找对应值。
     */
    private Map<NotificationRuleTriggerType, Long> deduplicationDurations;

    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired(required = false)
    private CacheManager cacheManager;
    private final ConcurrentMap<String, Long> localCache = new ConcurrentReferenceHashMap<>(16, SOFT);

    /**
     * 功能：执行 `alreadyProcessed` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean alreadyProcessed(NotificationRuleTrigger trigger) {
        String deduplicationKey = trigger.getDeduplicationKey();
        return alreadyProcessed(trigger, deduplicationKey, true);
    }

    /**
     * 功能：执行 `alreadyProcessed` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `rule`：`rule` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean alreadyProcessed(NotificationRuleTrigger trigger, NotificationRule rule) {
        String deduplicationKey = getDeduplicationKey(trigger, rule);
        return alreadyProcessed(trigger, deduplicationKey, false);
    }

    /**
     * 功能：执行 `alreadyProcessed` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `deduplicationKey`：键。
     * - `onlyLocalCache`：`onlyLocalCache` 参数。
     * 返回：判断结果。
     */
    private boolean alreadyProcessed(NotificationRuleTrigger trigger, String deduplicationKey, boolean onlyLocalCache) {
        Long lastProcessedTs = localCache.get(deduplicationKey);
        if (lastProcessedTs == null && !onlyLocalCache) {
            Cache externalCache = getExternalCache();
            if (externalCache != null) {
                lastProcessedTs = externalCache.get(deduplicationKey, Long.class);
            } else {
                log.warn("Sent notifications cache is not set up");
            }
        }

        boolean alreadyProcessed = false;
        long deduplicationDuration = getDeduplicationDuration(trigger);
        if (lastProcessedTs != null) {
            long passed = System.currentTimeMillis() - lastProcessedTs;
            log.trace("Deduplicating trigger {} by key '{}'. Deduplication duration: {} ms, passed: {} ms",
                    trigger.getType(), deduplicationKey, deduplicationDuration, passed);
            if (deduplicationDuration == 0 || passed <= deduplicationDuration) {
                alreadyProcessed = true;
            }
        }

        if (!alreadyProcessed) {
            lastProcessedTs = System.currentTimeMillis();
        }
        localCache.put(deduplicationKey, lastProcessedTs);
        if (!onlyLocalCache) {
            if (!alreadyProcessed || deduplicationDuration == 0) {
                // if lastProcessedTs is changed or if deduplicating infinitely (so that cache value not removed by ttl)
                Cache externalCache = getExternalCache();
                if (externalCache != null) {
                    externalCache.put(deduplicationKey, lastProcessedTs);
                }
            }
        }
        return alreadyProcessed;
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `rule`：`rule` 参数。
     * 返回：文本结果。
     */
    public static String getDeduplicationKey(NotificationRuleTrigger trigger, NotificationRule rule) {
        return String.join("_", trigger.getDeduplicationKey(), rule.getDeduplicationKey());
    }

    /**
     * 功能：获取持续时间。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：数值结果。
     */
    private long getDeduplicationDuration(NotificationRuleTrigger trigger) {
        return deduplicationDurations.computeIfAbsent(trigger.getType(), triggerType -> {
            return trigger.getDefaultDeduplicationDuration();
        });
    }

    /**
     * 功能：获取`External Cache`。
     * 参数：无。
     * 返回：处理结果。
     */
    private Cache getExternalCache() {
        return Optional.ofNullable(cacheManager)
                .map(cacheManager -> cacheManager.getCache(CacheConstants.SENT_NOTIFICATIONS_CACHE))
                .orElse(null);
    }

    /**
     * 功能：更新`Deduplication Durations`。
     * 参数：
     * - `deduplicationDurationsStr`：`deduplicationDurationsStr` 参数。
     * 返回：无。
     */
    @Autowired
    public void setDeduplicationDurations(@Value("${notification_system.rules.deduplication_durations:}")
                                          String deduplicationDurationsStr) {
        this.deduplicationDurations = new HashMap<>();
        PropertyUtils.getProps(deduplicationDurationsStr).forEach((triggerType, duration) -> {
            this.deduplicationDurations.put(NotificationRuleTriggerType.valueOf(triggerType), Long.parseLong(duration));
        });
    }

}
