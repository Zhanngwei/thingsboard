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
 * 1. 类目的：`DefaultRateLimitService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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
     * 1. 类目的：`RateLimitKey` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultRateLimitService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
