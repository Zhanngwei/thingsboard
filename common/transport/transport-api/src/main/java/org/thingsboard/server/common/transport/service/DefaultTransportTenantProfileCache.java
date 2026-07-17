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
package org.thingsboard.server.common.transport.service;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. `DefaultTransportTenantProfileCache` 是 ThingsBoard Common Transport 中管理租户缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `TransportTenantProfileCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Component
@TbTransportComponent
@Slf4j
public class DefaultTransportTenantProfileCache implements TransportTenantProfileCache {

    private final Lock tenantProfileFetchLock = new ReentrantLock();
    private final ConcurrentMap<TenantProfileId, TenantProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantProfileId, Set<TenantId>> tenantProfileIds = new ConcurrentHashMap<>();
    /**
     * 数据，提供当前类调用的业务操作。
     */
    private final DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private TransportRateLimitService rateLimitService;
    private TransportService transportService;

    /**
     * 功能：更新服务。
     * 参数：
     * - `rateLimitService`：服务对象。
     * 返回：无。
     */
    @Lazy
    @Autowired
    public void setRateLimitService(TransportRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * 功能：更新服务。
     * 参数：
     * - `transportService`：服务对象。
     * 返回：无。
     */
    @Lazy
    @Autowired
    public void setTransportService(TransportService transportService) {
        this.transportService = transportService;
    }

    /**
     * 功能：创建 `DefaultTransportTenantProfileCache` 实例，并初始化必要字段。
     * 参数：
     * - `dataDecodingEncodingService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public DefaultTransportTenantProfileCache(DataDecodingEncodingService dataDecodingEncodingService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile get(TenantId tenantId) {
        return getTenantProfile(tenantId);
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `profileBody`：`profileBody` 参数。
     * 返回：处理结果。
     */
    @Override
    public TenantProfileUpdateResult put(ByteString profileBody) {
        Optional<TenantProfile> profileOpt = dataDecodingEncodingService.decode(profileBody.toByteArray());
        if (profileOpt.isPresent()) {
            TenantProfile newProfile = profileOpt.get();
            log.trace("[{}] put: {}", newProfile.getId(), newProfile);
            profiles.put(newProfile.getId(), newProfile);
            Set<TenantId> affectedTenants = tenantProfileIds.get(newProfile.getId());
            return new TenantProfileUpdateResult(newProfile, affectedTenants != null ? affectedTenants : Collections.emptySet());
        } else {
            log.warn("Failed to decode profile: {}", profileBody.toString());
            return new TenantProfileUpdateResult(null, Collections.emptySet());
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * 返回：判断结果。
     */
    @Override
    public boolean put(TenantId tenantId, TenantProfileId profileId) {
        log.trace("[{}] put: {}", tenantId, profileId);
        TenantProfileId oldProfileId = tenantIds.get(tenantId);
        if (oldProfileId != null && !oldProfileId.equals(profileId)) {
            tenantProfileIds.computeIfAbsent(oldProfileId, id -> ConcurrentHashMap.newKeySet()).remove(tenantId);
            tenantIds.put(tenantId, profileId);
            tenantProfileIds.computeIfAbsent(profileId, id -> ConcurrentHashMap.newKeySet()).add(tenantId);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `profileId`：配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<TenantId> remove(TenantProfileId profileId) {
        Set<TenantId> tenants = tenantProfileIds.remove(profileId);
        if (tenants != null) {
            tenants.forEach(tenantIds::remove);
        }
        profiles.remove(profileId);
        return tenants;
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    private TenantProfile getTenantProfile(TenantId tenantId) {
        TenantProfile profile = null;
        TenantProfileId tenantProfileId = tenantIds.get(tenantId);
        if (tenantProfileId != null) {
            profile = profiles.get(tenantProfileId);
        }
        if (profile == null) {
            tenantProfileFetchLock.lock();
            try {
                tenantProfileId = tenantIds.get(tenantId);
                if (tenantProfileId != null) {
                    profile = profiles.get(tenantProfileId);
                }
                if (profile == null) {
                    TransportProtos.GetEntityProfileRequestMsg msg = TransportProtos.GetEntityProfileRequestMsg.newBuilder()
                            .setEntityType(EntityType.TENANT.name())
                            .setEntityIdMSB(tenantId.getId().getMostSignificantBits())
                            .setEntityIdLSB(tenantId.getId().getLeastSignificantBits())
                            .build();
                    TransportProtos.GetEntityProfileResponseMsg entityProfileMsg = transportService.getEntityProfile(msg);
                    Optional<TenantProfile> profileOpt = dataDecodingEncodingService.decode(entityProfileMsg.getData().toByteArray());
                    if (profileOpt.isPresent()) {
                        profile = profileOpt.get();
                        TenantProfile existingProfile = profiles.get(profile.getId());
                        if (existingProfile != null) {
                            profile = existingProfile;
                        } else {
                            profiles.put(profile.getId(), profile);
                        }
                        tenantProfileIds.computeIfAbsent(profile.getId(), id -> ConcurrentHashMap.newKeySet()).add(tenantId);
                        tenantIds.put(tenantId, profile.getId());
                    } else {
                        log.warn("[{}] Can't decode tenant profile: {}", tenantId, entityProfileMsg.getData());
                        throw new RuntimeException("Can't decode tenant profile!");
                    }
                    Optional<ApiUsageState> apiStateOpt = dataDecodingEncodingService.decode(entityProfileMsg.getApiState().toByteArray());
                    apiStateOpt.ifPresent(apiUsageState -> rateLimitService.update(tenantId, apiUsageState.isTransportEnabled()));
                }
            } finally {
                tenantProfileFetchLock.unlock();
            }
        }
        return profile;
    }


}
