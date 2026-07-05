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
package org.thingsboard.server.service.limits;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.cache.limits.DefaultRateLimitService;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.tenant.DefaultTbTenantProfileCache;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`RateLimitServiceTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@RunWith(MockitoJUnitRunner.class)
public class RateLimitServiceTest {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private RateLimitService rateLimitService;
    private DefaultTbTenantProfileCache tenantProfileCache;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeEach() {
        tenantProfileCache = Mockito.mock(DefaultTbTenantProfileCache.class);
        rateLimitService = new DefaultRateLimitService(tenantProfileCache, mock(NotificationRuleProcessor.class), 60, 100);
        tenantId = new TenantId(UUID.randomUUID());
    }

    /**
     * 功能：验证频率相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRateLimits() {
        int max = 2;
        String rateLimit = max + ":600";
        DefaultTenantProfileConfiguration profileConfiguration = new DefaultTenantProfileConfiguration();
        profileConfiguration.setTenantEntityExportRateLimit(rateLimit);
        profileConfiguration.setTenantEntityImportRateLimit(rateLimit);
        profileConfiguration.setTenantNotificationRequestsRateLimit(rateLimit);
        profileConfiguration.setTenantNotificationRequestsPerRuleRateLimit(rateLimit);
        profileConfiguration.setTenantServerRestLimitsConfiguration(rateLimit);
        profileConfiguration.setCustomerServerRestLimitsConfiguration(rateLimit);
        profileConfiguration.setWsUpdatesPerSessionRateLimit(rateLimit);
        profileConfiguration.setCassandraQueryTenantRateLimitsConfiguration(rateLimit);
        profileConfiguration.setEdgeEventRateLimits(rateLimit);
        profileConfiguration.setEdgeEventRateLimitsPerEdge(rateLimit);
        profileConfiguration.setEdgeUplinkMessagesRateLimits(rateLimit);
        profileConfiguration.setEdgeUplinkMessagesRateLimitsPerEdge(rateLimit);
        updateTenantProfileConfiguration(profileConfiguration);

        for (LimitedApi limitedApi : List.of(
                LimitedApi.ENTITY_EXPORT,
                LimitedApi.ENTITY_IMPORT,
                LimitedApi.NOTIFICATION_REQUESTS,
                LimitedApi.REST_REQUESTS_PER_CUSTOMER,
                LimitedApi.CASSANDRA_QUERIES,
                LimitedApi.EDGE_EVENTS,
                LimitedApi.EDGE_EVENTS_PER_EDGE,
                LimitedApi.EDGE_UPLINK_MESSAGES,
                LimitedApi.EDGE_UPLINK_MESSAGES_PER_EDGE
        )) {
            testRateLimits(limitedApi, max, tenantId);
        }

        CustomerId customerId = new CustomerId(UUID.randomUUID());
        testRateLimits(LimitedApi.REST_REQUESTS_PER_CUSTOMER, max, customerId);

        NotificationRuleId notificationRuleId = new NotificationRuleId(UUID.randomUUID());
        testRateLimits(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, max, notificationRuleId);

        String wsSessionId = UUID.randomUUID().toString();
        testRateLimits(LimitedApi.WS_UPDATES_PER_SESSION, max, wsSessionId);
    }

    /**
     * 功能：验证频率相关场景。
     * 参数：
     * - `limitedApi`：数量限制。
     * - `max`：`max` 参数。
     * - `level`：`level` 参数。
     * 返回：无。
     */
    private void testRateLimits(LimitedApi limitedApi, int max, Object level) {
        for (int i = 1; i <= max; i++) {
            boolean success = rateLimitService.checkRateLimit(limitedApi, tenantId, level);
            assertTrue(success);
        }
        boolean success = rateLimitService.checkRateLimit(limitedApi, tenantId, level);
        assertFalse(success);
    }

    /**
     * 功能：更新租户。
     * 参数：
     * - `profileConfiguration`：配置对象。
     * 返回：无。
     */
    private void updateTenantProfileConfiguration(DefaultTenantProfileConfiguration profileConfiguration) {
        reset(tenantProfileCache);
        TenantProfile tenantProfile = new TenantProfile();
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(profileConfiguration);
        tenantProfile.setProfileData(profileData);
        when(tenantProfileCache.get(eq(tenantId))).thenReturn(tenantProfile);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RateLimitServiceTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
