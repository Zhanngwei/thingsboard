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
package org.thingsboard.server.service.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbDeviceProfileCache` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
public class DefaultTbDeviceProfileCache implements TbDeviceProfileCache {

    private final Lock deviceProfileFetchLock = new ReentrantLock();
    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    private final DeviceProfileService deviceProfileService;
    private final DeviceService deviceService;

    private final ConcurrentMap<DeviceProfileId, DeviceProfile> deviceProfilesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, DeviceProfileId> devicesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, Consumer<DeviceProfile>>> profileListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, BiConsumer<DeviceId, DeviceProfile>>> deviceProfileListeners = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `DefaultTbDeviceProfileCache` 实例，并初始化必要字段。
     * 参数：
     * - `deviceProfileService`：设备信息或设备标识。
     * - `deviceService`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public DefaultTbDeviceProfileCache(DeviceProfileService deviceProfileService, DeviceService deviceService) {
        this.deviceProfileService = deviceProfileService;
        this.deviceService = deviceService;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile get(TenantId tenantId, DeviceProfileId deviceProfileId) {
        DeviceProfile profile = deviceProfilesMap.get(deviceProfileId);
        if (profile == null) {
            deviceProfileFetchLock.lock();
            try {
                profile = deviceProfilesMap.get(deviceProfileId);
                if (profile == null) {
                    profile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
                    if (profile != null) {
                        deviceProfilesMap.put(deviceProfileId, profile);
                        log.debug("[{}] Fetch device profile into cache: {}", profile.getId(), profile);
                    }
                }
            } finally {
                deviceProfileFetchLock.unlock();
            }
        }
        log.trace("[{}] Found device profile in cache: {}", deviceProfileId, profile);
        return profile;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile get(TenantId tenantId, DeviceId deviceId) {
        DeviceProfileId profileId = devicesMap.get(deviceId);
        if (profileId == null) {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            if (device != null) {
                profileId = device.getDeviceProfileId();
                devicesMap.put(deviceId, profileId);
            } else {
                return null;
            }
        }
        return get(tenantId, profileId);
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * 返回：无。
     */
    @Override
    public void evict(TenantId tenantId, DeviceProfileId profileId) {
        DeviceProfile oldProfile = deviceProfilesMap.remove(profileId);
        log.debug("[{}] evict device profile from cache: {}", profileId, oldProfile);
        DeviceProfile newProfile = get(tenantId, profileId);
        if (newProfile != null) {
            notifyProfileListeners(newProfile);
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    @Override
    public void evict(TenantId tenantId, DeviceId deviceId) {
        DeviceProfileId old = devicesMap.remove(deviceId);
        if (old != null) {
            DeviceProfile newProfile = get(tenantId, deviceId);
            if (newProfile == null || !old.equals(newProfile.getId())) {
                notifyDeviceListeners(tenantId, deviceId, newProfile);
            }
        }
    }

    /**
     * 功能：保存或创建监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * - `profileListener`：`profileListener` 参数。
     * - `deviceListener`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void addListener(TenantId tenantId, EntityId listenerId,
                            Consumer<DeviceProfile> profileListener,
                            BiConsumer<DeviceId, DeviceProfile> deviceListener) {
        if (profileListener != null) {
            profileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, profileListener);
        }
        if (deviceListener != null) {
            deviceProfileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, deviceListener);
        }
    }

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile find(DeviceProfileId deviceProfileId) {
        return deviceProfileService.findDeviceProfileById(TenantId.SYS_TENANT_ID, deviceProfileId);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String profileName) {
        return deviceProfileService.findOrCreateDeviceProfile(tenantId, profileName);
    }

    /**
     * 功能：删除或清理监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * 返回：无。
     */
    @Override
    public void removeListener(TenantId tenantId, EntityId listenerId) {
        ConcurrentMap<EntityId, Consumer<DeviceProfile>> tenantListeners = profileListeners.get(tenantId);
        if (tenantListeners != null) {
            tenantListeners.remove(listenerId);
        }
        ConcurrentMap<EntityId, BiConsumer<DeviceId, DeviceProfile>> deviceListeners = deviceProfileListeners.get(tenantId);
        if (deviceListeners != null) {
            deviceListeners.remove(listenerId);
        }
    }

    /**
     * 功能：通知配置。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    private void notifyProfileListeners(DeviceProfile profile) {
        ConcurrentMap<EntityId, Consumer<DeviceProfile>> tenantListeners = profileListeners.get(profile.getTenantId());
        if (tenantListeners != null) {
            tenantListeners.forEach((id, listener) -> listener.accept(profile));
        }
    }

    /**
     * 功能：通知设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    private void notifyDeviceListeners(TenantId tenantId, DeviceId deviceId, DeviceProfile profile) {
        if (profile != null) {
            ConcurrentMap<EntityId, BiConsumer<DeviceId, DeviceProfile>> tenantListeners = deviceProfileListeners.get(tenantId);
            if (tenantListeners != null) {
                tenantListeners.forEach((id, listener) -> listener.accept(deviceId, profile));
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbDeviceProfileCache` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
