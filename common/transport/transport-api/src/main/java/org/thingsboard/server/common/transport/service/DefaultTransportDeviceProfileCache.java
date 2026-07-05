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
 * 1. 类目的：`DefaultTransportDeviceProfileCache` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTransportDeviceProfileCache` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
