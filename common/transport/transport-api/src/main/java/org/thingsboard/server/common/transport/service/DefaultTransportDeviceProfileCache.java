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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. `DefaultTransportDeviceProfileCache` 是 ThingsBoard Common Transport 中管理设备配置缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `TransportDeviceProfileCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Slf4j
@Component
@TbTransportComponent
public class DefaultTransportDeviceProfileCache implements TransportDeviceProfileCache {

    private final Lock deviceProfileFetchLock = new ReentrantLock();
    private final ConcurrentMap<DeviceProfileId, DeviceProfile> deviceProfiles = new ConcurrentHashMap<>();
    /**
     * 数据，提供当前类调用的业务操作。
     */
    private final DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private TransportService transportService;

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
     * 功能：创建 `DefaultTransportDeviceProfileCache` 实例，并初始化必要字段。
     * 参数：
     * - `dataDecodingEncodingService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public DefaultTransportDeviceProfileCache(DataDecodingEncodingService dataDecodingEncodingService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    /**
     * 功能：获取`Or Create`。
     * 参数：
     * - `id`：`id`ID。
     * - `profileBody`：`profileBody` 参数。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile getOrCreate(DeviceProfileId id, ByteString profileBody) {
        DeviceProfile profile = deviceProfiles.get(id);
        if (profile == null) {
            Optional<DeviceProfile> deviceProfile = dataDecodingEncodingService.decode(profileBody.toByteArray());
            if (deviceProfile.isPresent()) {
                profile = deviceProfile.get();
                deviceProfiles.put(id, profile);
            }
        }
        return profile;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile get(DeviceProfileId id) {
        return this.getDeviceProfile(id);
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    @Override
    public void put(DeviceProfile profile) {
        deviceProfiles.put(profile.getId(), profile);
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `profileBody`：`profileBody` 参数。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile put(ByteString profileBody) {
        Optional<DeviceProfile> deviceProfile = dataDecodingEncodingService.decode(profileBody.toByteArray());
        if (deviceProfile.isPresent()) {
            put(deviceProfile.get());
            return deviceProfile.get();
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public void evict(DeviceProfileId id) {
        deviceProfiles.remove(id);
    }


    /**
     * 功能：获取设备配置。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    private DeviceProfile getDeviceProfile(DeviceProfileId id) {
        DeviceProfile profile = deviceProfiles.get(id);
        if (profile == null) {
            deviceProfileFetchLock.lock();
            try {
                TransportProtos.GetEntityProfileRequestMsg msg = TransportProtos.GetEntityProfileRequestMsg.newBuilder()
                        .setEntityType(EntityType.DEVICE_PROFILE.name())
                        .setEntityIdMSB(id.getId().getMostSignificantBits())
                        .setEntityIdLSB(id.getId().getLeastSignificantBits())
                        .build();
                TransportProtos.GetEntityProfileResponseMsg entityProfileMsg = transportService.getEntityProfile(msg);
                Optional<DeviceProfile> profileOpt = dataDecodingEncodingService.decode(entityProfileMsg.getData().toByteArray());
                if (profileOpt.isPresent()) {
                    profile = profileOpt.get();
                    this.put(profile);
                } else {
                    log.warn("[{}] Can't find device profile: {}", id, entityProfileMsg.getData());
                    throw new RuntimeException("Can't find device profile!");
                }
            } finally {
                deviceProfileFetchLock.unlock();
            }
        }
        return profile;
    }
}
