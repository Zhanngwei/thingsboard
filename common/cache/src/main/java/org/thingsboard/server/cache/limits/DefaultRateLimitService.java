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
package org.thingsboard.server.cache.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.TenantProfileNotFoundException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.tools.TbRateLimits;

import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultRateLimitService` 是 ThingsBoard Common Cache 中负责 `Rate Limit` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `RateLimitService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Lazy
@Service
@Slf4j
public class DefaultRateLimitService implements RateLimitService {

    /**
     * 租户，用于按场景创建或提供目标对象。
     */
    private final TenantProfileProvider tenantProfileProvider;
    private final NotificationRuleProcessor notificationRuleProcessor;

    /**
     * 功能：创建 `DefaultRateLimitService` 实例，并初始化必要字段。
     * 参数：
     * - `tenantProfileProvider`：租户信息或租户标识。
     * - `notificationRuleProcessor`：处理器对象。
     * - `rateLimitsTtl`：数量限制。
     * - `rateLimitsCacheMaxSize`：数量限制。
     * 返回：新创建的对象实例。
     */
    public DefaultRateLimitService(TenantProfileProvider tenantProfileProvider,
                                   @Lazy NotificationRuleProcessor notificationRuleProcessor,
                                   @Value("${cache.rateLimits.timeToLiveInMinutes:120}") int rateLimitsTtl,
                                   @Value("${cache.rateLimits.maxSize:200000}") int rateLimitsCacheMaxSize) {
        this.tenantProfileProvider = tenantProfileProvider;
        this.notificationRuleProcessor = notificationRuleProcessor;
        this.rateLimits = Caffeine.newBuilder()
                .expireAfterAccess(rateLimitsTtl, TimeUnit.MINUTES)
                .maximumSize(rateLimitsCacheMaxSize)
                .build();
    }

    /**
     * 频率，表示当前对象的对应属性。
     */
    private final Cache<RateLimitKey, TbRateLimits> rateLimits;

    /**
     * 功能：校验数量限制。
     * 参数：
     * - `api`：`api` 参数。
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId) {
        return checkRateLimit(api, tenantId, tenantId);
    }

    /**
     * 功能：校验数量限制。
     * 参数：
     * - `api`：`api` 参数。
     * - `tenantId`：租户IDID。
     * - `level`：`level` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean checkRateLimit(LimitedApi api, TenantId tenantId, Object level) {
        if (tenantId.isSysTenantId()) {
            return true;
        }
        TenantProfile tenantProfile = tenantProfileProvider.get(tenantId);
        if (tenantProfile == null) {
            throw new TenantProfileNotFoundException(tenantId);
        }

        String rateLimitConfig = tenantProfile.getProfileConfiguration()
                .map(api::getLimitConfig).orElse(null);
        boolean success = checkRateLimit(api, level, rateLimitConfig);
        if (!success) {
            notificationRuleProcessor.process(RateLimitsTrigger.builder()
                    .tenantId(tenantId)
                    .api(api)
                    .limitLevel(level instanceof EntityId ? (EntityId) level : tenantId)
                    .limitLevelEntityName(null)
                    .build());
        }
        return success;
    }

    /**
     * 功能：校验数量限制。
     * 参数：
     * - `api`：`api` 参数。
     * - `level`：`level` 参数。
     * - `rateLimitConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean checkRateLimit(LimitedApi api, Object level, String rateLimitConfig) {
        RateLimitKey key = new RateLimitKey(api, level);
        if (StringUtils.isEmpty(rateLimitConfig)) {
            rateLimits.invalidate(key);
            return true;
        }
        log.trace("[{}] Checking rate limit for {} ({})", level, api, rateLimitConfig);

        TbRateLimits rateLimit = rateLimits.asMap().compute(key, (k, limit) -> {
            if (limit == null || !limit.getConfiguration().equals(rateLimitConfig)) {
                limit = new TbRateLimits(rateLimitConfig, api.isRefillRateLimitIntervally());
                log.trace("[{}] Created new rate limit bucket for {} ({})", level, api, rateLimitConfig);
            }
            return limit;
        });
        boolean success = rateLimit.tryConsume();
        if (!success) {
            log.debug("[{}] Rate limit exceeded for {} ({})", level, api, rateLimitConfig);
        }
        return success;
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：
     * - `api`：`api` 参数。
     * - `level`：`level` 参数。
     * 返回：无。
     */
    @Override
    public void cleanUp(LimitedApi api, Object level) {
        RateLimitKey key = new RateLimitKey(api, level);
        rateLimits.invalidate(key);
    }

    /**
     * 中文说明：
     * 1. `RateLimitKey` 是 ThingsBoard Common Cache 中管理 `Rate Limit Key` 缓存内容或失效事件的类型。
     * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
     * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
     * 4. 它直接协作于缓存实现、领域标识符和调用该缓存的服务。
     * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
     * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
     */
    @Data(staticConstructor = "of")
    private static class RateLimitKey {
        /**
         * `api` 字段，保存当前对象的对应属性。
         */
        private final LimitedApi api;
        private final Object level;
    }

}
