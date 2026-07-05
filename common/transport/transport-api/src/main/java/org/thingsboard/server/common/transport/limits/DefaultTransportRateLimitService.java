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
package org.thingsboard.server.common.transport.limits;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTransportRateLimitService` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
@Service
@TbTransportComponent
@Slf4j
public class DefaultTransportRateLimitService implements TransportRateLimitService {

    private final static DummyTransportRateLimit ALLOW = new DummyTransportRateLimit();
    private final ConcurrentMap<TenantId, Boolean> tenantAllowed = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Set<DeviceId>> tenantDevices = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, EntityTransportRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, EntityTransportRateLimits> perDeviceLimits = new ConcurrentHashMap<>();
    private final Map<InetAddress, InetAddressRateLimitStats> ipMap = new ConcurrentHashMap<>();

    /**
     * 租户对象，用于描述当前业务场景。
     */
    private final TransportTenantProfileCache tenantProfileCache;

    /**
     * 是否启用频率。
     */
    @Value("${transport.rate_limits.ip_limits_enabled:false}")
    private boolean ipRateLimitsEnabled;
    /**
     * 凭据，用于认证或安全校验。
     */
    @Value("${transport.rate_limits.max_wrong_credentials_per_ip:10}")
    private int maxWrongCredentialsPerIp;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${transport.rate_limits.ip_block_timeout:60000}")
    private long ipBlockTimeout;

    /**
     * 功能：创建 `DefaultTransportRateLimitService` 实例，并初始化必要字段。
     * 参数：
     * - `tenantProfileCache`：租户信息或租户标识。
     * 返回：新创建的对象实例。
     */
    public DefaultTransportRateLimitService(TransportTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    /**
     * 功能：校验`Limits`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `dataPoints`：待处理数据。
     * 返回：判断结果。
     */
    @Override
    public EntityType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints) {
        if (!tenantAllowed.getOrDefault(tenantId, Boolean.TRUE)) {
            return EntityType.API_USAGE_STATE;
        }
        if (!checkEntityRateLimit(dataPoints, getTenantRateLimits(tenantId))) {
            return EntityType.TENANT;
        }
        if (!checkEntityRateLimit(dataPoints, getDeviceRateLimits(tenantId, deviceId))) {
            return EntityType.DEVICE;
        }
        return null;
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `dataPoints`：待处理数据。
     * - `limits`：数量限制。
     * 返回：判断结果。
     */
    private boolean checkEntityRateLimit(int dataPoints, EntityTransportRateLimits limits) {
        if (dataPoints > 0) {
            return limits.getTelemetryMsgRateLimit().tryConsume() && limits.getTelemetryDataPointsRateLimit().tryConsume(dataPoints);
        } else {
            return limits.getRegularMsgRateLimit().tryConsume();
        }
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `update`：`update` 参数。
     * 返回：无。
     */
    @Override
    public void update(TenantProfileUpdateResult update) {
        log.info("Received tenant profile update: {}", update.getProfile());
        EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(update.getProfile(), true);
        EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(update.getProfile(), false);
        for (TenantId tenantId : update.getAffectedTenants()) {
            mergeLimits(tenantId, tenantRateLimitPrototype, perTenantLimits::get, perTenantLimits::put);
            getTenantDevices(tenantId).forEach(deviceId -> {
                mergeLimits(deviceId, deviceRateLimitPrototype, perDeviceLimits::get, perDeviceLimits::put);
            });
        }
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void update(TenantId tenantId) {
        EntityTransportRateLimits tenantRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), true);
        EntityTransportRateLimits deviceRateLimitPrototype = createRateLimits(tenantProfileCache.get(tenantId), false);
        mergeLimits(tenantId, tenantRateLimitPrototype, perTenantLimits::get, perTenantLimits::put);
        getTenantDevices(tenantId).forEach(deviceId -> {
            mergeLimits(deviceId, deviceRateLimitPrototype, perDeviceLimits::get, perDeviceLimits::put);
        });
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void remove(TenantId tenantId) {
        perTenantLimits.remove(tenantId);
        tenantDevices.remove(tenantId);
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    @Override
    public void remove(DeviceId deviceId) {
        perDeviceLimits.remove(deviceId);
        tenantDevices.values().forEach(set -> set.remove(deviceId));
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `allowed`：`allowed` 参数。
     * 返回：无。
     */
    @Override
    public void update(TenantId tenantId, boolean allowed) {
        tenantAllowed.put(tenantId, allowed);
    }

    /**
     * 功能：校验`Address`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean checkAddress(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return true;
        }
        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        return !stats.isBlocked() || (stats.getLastActivityTs() + ipBlockTimeout < System.currentTimeMillis());
    }

    /**
     * 功能：处理`on Auth Success`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    @Override
    public void onAuthSuccess(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            stats.setFailureCount(0);
            if (stats.isBlocked()) {
                stats.setBlocked(false);
                log.info("[{}] IP address un-blocked due to correct credentials.", address.getAddress());
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    @Override
    public void onAuthFailure(InetSocketAddress address) {
        if (!ipRateLimitsEnabled) {
            return;
        }

        var stats = ipMap.computeIfAbsent(address.getAddress(), a -> new InetAddressRateLimitStats());
        stats.getLock().lock();
        try {
            stats.setLastActivityTs(System.currentTimeMillis());
            int failureCount = stats.getFailureCount() + 1;
            stats.setFailureCount(failureCount);
            if (failureCount >= maxWrongCredentialsPerIp) {
                log.info("[{}] IP address blocked due to constantly wrong credentials.", address.getAddress());
                stats.setBlocked(true);
            }
        } finally {
            stats.getLock().unlock();
        }
    }

    /**
     * 功能：执行 `invalidateRateLimitsIpTable` 对应的处理。
     * 参数：
     * - `sessionInactivityTimeout`：会话对象。
     * 返回：无。
     */
    @Override
    public void invalidateRateLimitsIpTable(long sessionInactivityTimeout) {
        if (!ipRateLimitsEnabled) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long expTime = currentTime - Math.max(sessionInactivityTimeout, ipBlockTimeout);
        for (var entry : ipMap.entrySet()) {
            var stats = entry.getValue();
            if (stats.getLastActivityTs() < expTime) {
                log.debug("[{}] IP address removed due to session inactivity timeout.", entry.getKey());
                ipMap.remove(entry.getKey());
            } else if (stats.isBlocked() && (stats.getLastActivityTs() + ipBlockTimeout < currentTime)) {
                log.info("[{}] IP address unblocked due ip block timeout.", entry.getKey());
                stats.setBlocked(false);
            }
        }
    }

    /**
     * 功能：执行 `mergeLimits` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `newRateLimits`：数量限制。
     * - `getFunction`：`getFunction` 参数。
     * - `putFunction`：`putFunction` 参数。
     * 返回：无。
     */
    private <T extends EntityId> void mergeLimits(T entityId, EntityTransportRateLimits newRateLimits,
                                                  Function<T, EntityTransportRateLimits> getFunction,
                                                  BiConsumer<T, EntityTransportRateLimits> putFunction) {
        EntityTransportRateLimits oldRateLimits = getFunction.apply(entityId);
        if (oldRateLimits == null) {
            if (EntityType.TENANT.equals(entityId.getEntityType())) {
                log.info("[{}] New rate limits: {}", entityId, newRateLimits);
            } else {
                log.debug("[{}] New rate limits: {}", entityId, newRateLimits);
            }
            putFunction.accept(entityId, newRateLimits);
        } else {
            EntityTransportRateLimits updated = merge(oldRateLimits, newRateLimits);
            if (updated != null) {
                if (EntityType.TENANT.equals(entityId.getEntityType())) {
                    log.info("[{}] Updated rate limits: {}", entityId, updated);
                } else {
                    log.debug("[{}] Updated rate limits: {}", entityId, updated);
                }
                putFunction.accept(entityId, updated);
            }
        }
    }

    /**
     * 功能：执行 `merge` 对应的处理。
     * 参数：
     * - `oldRateLimits`：数量限制。
     * - `newRateLimits`：数量限制。
     * 返回：处理结果。
     */
    private EntityTransportRateLimits merge(EntityTransportRateLimits oldRateLimits, EntityTransportRateLimits newRateLimits) {
        boolean regularUpdate = !oldRateLimits.getRegularMsgRateLimit().getConfiguration().equals(newRateLimits.getRegularMsgRateLimit().getConfiguration());
        boolean telemetryMsgRateUpdate = !oldRateLimits.getTelemetryMsgRateLimit().getConfiguration().equals(newRateLimits.getTelemetryMsgRateLimit().getConfiguration());
        boolean telemetryDataPointUpdate = !oldRateLimits.getTelemetryDataPointsRateLimit().getConfiguration().equals(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration());
        if (regularUpdate || telemetryMsgRateUpdate || telemetryDataPointUpdate) {
            return new EntityTransportRateLimits(
                    regularUpdate ? newLimit(newRateLimits.getRegularMsgRateLimit().getConfiguration()) : oldRateLimits.getRegularMsgRateLimit(),
                    telemetryMsgRateUpdate ? newLimit(newRateLimits.getTelemetryMsgRateLimit().getConfiguration()) : oldRateLimits.getTelemetryMsgRateLimit(),
                    telemetryDataPointUpdate ? newLimit(newRateLimits.getTelemetryDataPointsRateLimit().getConfiguration()) : oldRateLimits.getTelemetryDataPointsRateLimit());
        } else {
            return null;
        }
    }

    /**
     * 功能：保存或创建频率。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * - `tenant`：租户信息或租户标识。
     * 返回：处理结果。
     */
    private EntityTransportRateLimits createRateLimits(TenantProfile tenantProfile, boolean tenant) {
        TenantProfileData profileData = tenantProfile.getProfileData();
        DefaultTenantProfileConfiguration profile = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
        if (profile == null) {
            return new EntityTransportRateLimits(ALLOW, ALLOW, ALLOW);
        } else {
            TransportRateLimit regularMsgRateLimit = newLimit(tenant ? profile.getTransportTenantMsgRateLimit() : profile.getTransportDeviceMsgRateLimit());
            TransportRateLimit telemetryMsgRateLimit = newLimit(tenant ? profile.getTransportTenantTelemetryMsgRateLimit() : profile.getTransportDeviceTelemetryMsgRateLimit());
            TransportRateLimit telemetryDpRateLimit = newLimit(tenant ? profile.getTransportTenantTelemetryDataPointsRateLimit() : profile.getTransportDeviceTelemetryDataPointsRateLimit());
            return new EntityTransportRateLimits(regularMsgRateLimit, telemetryMsgRateLimit, telemetryDpRateLimit);
        }
    }

    /**
     * 功能：执行 `newLimit` 对应的处理。
     * 参数：
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    private static TransportRateLimit newLimit(String config) {
        return StringUtils.isEmpty(config) ? ALLOW : new SimpleTransportRateLimit(config);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    private EntityTransportRateLimits getTenantRateLimits(TenantId tenantId) {
        return perTenantLimits.computeIfAbsent(tenantId, k -> {
            return createRateLimits(tenantProfileCache.get(tenantId), true);
        });
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    private EntityTransportRateLimits getDeviceRateLimits(TenantId tenantId, DeviceId deviceId) {
        return perDeviceLimits.computeIfAbsent(deviceId, k -> {
            EntityTransportRateLimits limits = createRateLimits(tenantProfileCache.get(tenantId), false);
            getTenantDevices(tenantId).add(deviceId);
            return limits;
        });
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    private Set<DeviceId> getTenantDevices(TenantId tenantId) {
        return tenantDevices.computeIfAbsent(tenantId, id -> ConcurrentHashMap.newKeySet());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTransportRateLimitService` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
