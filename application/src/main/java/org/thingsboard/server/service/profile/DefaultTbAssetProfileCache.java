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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbAssetProfileCache` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
public class DefaultTbAssetProfileCache implements TbAssetProfileCache {

    private final Lock assetProfileFetchLock = new ReentrantLock();
    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AssetProfileService assetProfileService;
    private final AssetService assetService;

    private final ConcurrentMap<AssetProfileId, AssetProfile> assetProfilesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<AssetId, AssetProfileId> assetsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, Consumer<AssetProfile>>> profileListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>>> assetProfileListeners = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `DefaultTbAssetProfileCache` 实例，并初始化必要字段。
     * 参数：
     * - `assetProfileService`：服务对象。
     * - `assetService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public DefaultTbAssetProfileCache(AssetProfileService assetProfileService, AssetService assetService) {
        this.assetProfileService = assetProfileService;
        this.assetService = assetService;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile get(TenantId tenantId, AssetProfileId assetProfileId) {
        AssetProfile profile = assetProfilesMap.get(assetProfileId);
        if (profile == null) {
            assetProfileFetchLock.lock();
            try {
                profile = assetProfilesMap.get(assetProfileId);
                if (profile == null) {
                    profile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
                    if (profile != null) {
                        assetProfilesMap.put(assetProfileId, profile);
                        log.debug("[{}] Fetch asset profile into cache: {}", profile.getId(), profile);
                    }
                }
            } finally {
                assetProfileFetchLock.unlock();
            }
        }
        log.trace("[{}] Found asset profile in cache: {}", assetProfileId, profile);
        return profile;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile get(TenantId tenantId, AssetId assetId) {
        AssetProfileId profileId = assetsMap.get(assetId);
        if (profileId == null) {
            Asset asset = assetService.findAssetById(tenantId, assetId);
            if (asset != null) {
                profileId = asset.getAssetProfileId();
                assetsMap.put(assetId, profileId);
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
    public void evict(TenantId tenantId, AssetProfileId profileId) {
        AssetProfile oldProfile = assetProfilesMap.remove(profileId);
        log.debug("[{}] evict asset profile from cache: {}", profileId, oldProfile);
        AssetProfile newProfile = get(tenantId, profileId);
        if (newProfile != null) {
            notifyProfileListeners(newProfile);
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：无。
     */
    @Override
    public void evict(TenantId tenantId, AssetId assetId) {
        AssetProfileId old = assetsMap.remove(assetId);
        if (old != null) {
            AssetProfile newProfile = get(tenantId, assetId);
            if (newProfile == null || !old.equals(newProfile.getId())) {
                notifyAssetListeners(tenantId, assetId, newProfile);
            }
        }
    }

    /**
     * 功能：保存或创建监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * - `profileListener`：`profileListener` 参数。
     * - `assetListener`：`assetListener` 参数。
     * 返回：无。
     */
    @Override
    public void addListener(TenantId tenantId, EntityId listenerId,
                            Consumer<AssetProfile> profileListener,
                            BiConsumer<AssetId, AssetProfile> assetListener) {
        if (profileListener != null) {
            profileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, profileListener);
        }
        if (assetListener != null) {
            assetProfileListeners.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(listenerId, assetListener);
        }
    }

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile find(AssetProfileId assetProfileId) {
        return assetProfileService.findAssetProfileById(TenantId.SYS_TENANT_ID, assetProfileId);
    }

    /**
     * 功能：获取资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileName`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile findOrCreateAssetProfile(TenantId tenantId, String profileName) {
        return assetProfileService.findOrCreateAssetProfile(tenantId, profileName);
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
        ConcurrentMap<EntityId, Consumer<AssetProfile>> tenantListeners = profileListeners.get(tenantId);
        if (tenantListeners != null) {
            tenantListeners.remove(listenerId);
        }
        ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>> assetListeners = assetProfileListeners.get(tenantId);
        if (assetListeners != null) {
            assetListeners.remove(listenerId);
        }
    }

    /**
     * 功能：通知配置。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    private void notifyProfileListeners(AssetProfile profile) {
        ConcurrentMap<EntityId, Consumer<AssetProfile>> tenantListeners = profileListeners.get(profile.getTenantId());
        if (tenantListeners != null) {
            tenantListeners.forEach((id, listener) -> listener.accept(profile));
        }
    }

    /**
     * 功能：通知资产。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    private void notifyAssetListeners(TenantId tenantId, AssetId assetId, AssetProfile profile) {
        if (profile != null) {
            ConcurrentMap<EntityId, BiConsumer<AssetId, AssetProfile>> tenantListeners = assetProfileListeners.get(tenantId);
            if (tenantListeners != null) {
                tenantListeners.forEach((id, listener) -> listener.accept(assetId, profile));
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbAssetProfileCache` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
