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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportResourceCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTransportResourceCache` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
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
public class DefaultTransportResourceCache implements TransportResourceCache {

    private final Lock resourceFetchLock = new ReentrantLock();
    private final ConcurrentMap<ResourceCompositeKey, TbResource> resources = new ConcurrentHashMap<>();
    private final Set<ResourceCompositeKey> keys = ConcurrentHashMap.newKeySet();
    /**
     * 数据，提供当前类调用的业务操作。
     */
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final TransportService transportService;

    /**
     * 功能：创建 `DefaultTransportResourceCache` 实例，并初始化必要字段。
     * 参数：
     * - `dataDecodingEncodingService`：服务对象。
     * - `transportService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public DefaultTransportResourceCache(DataDecodingEncodingService dataDecodingEncodingService, @Lazy TransportService transportService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
        this.transportService = transportService;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<TbResource> get(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        TbResource resource;

        if (keys.contains(compositeKey)) {
            resource = resources.get(compositeKey);
            if (resource == null) {
                resource = resources.get(compositeKey.getSystemKey());
            }
        } else {
            resourceFetchLock.lock();
            try {
                if (keys.contains(compositeKey)) {
                    resource = resources.get(compositeKey);
                    if (resource == null) {
                        resource = resources.get(compositeKey.getSystemKey());
                    }
                } else {
                    resource = fetchResource(compositeKey);
                    keys.add(compositeKey);
                }
            } finally {
                resourceFetchLock.unlock();
            }
        }

        return Optional.ofNullable(resource);
    }

    /**
     * 功能：获取`Resource`。
     * 参数：
     * - `compositeKey`：键。
     * 返回：处理结果。
     */
    private TbResource fetchResource(ResourceCompositeKey compositeKey) {
        UUID tenantId = compositeKey.getTenantId().getId();
        TransportProtos.GetResourceRequestMsg.Builder builder = TransportProtos.GetResourceRequestMsg.newBuilder();
        builder
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setResourceType(compositeKey.resourceType.name())
                .setResourceKey(compositeKey.resourceKey);
        TransportProtos.GetResourceResponseMsg responseMsg = transportService.getResource(builder.build());

        Optional<TbResource> optionalResource = dataDecodingEncodingService.decode(responseMsg.getResource().toByteArray());
        if (optionalResource.isPresent()) {
            TbResource resource = optionalResource.get();
            resources.put(new ResourceCompositeKey(resource.getTenantId(), resource.getResourceType(), resource.getResourceKey()), resource);
            return resource;
        }

        return null;
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：无。
     */
    @Override
    public void update(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        if (keys.contains(compositeKey) || resources.containsKey(compositeKey)) {
            fetchResource(compositeKey);
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceType`：类型。
     * - `resourceKey`：键。
     * 返回：无。
     */
    @Override
    public void evict(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        ResourceCompositeKey compositeKey = new ResourceCompositeKey(tenantId, resourceType, resourceKey);
        keys.remove(compositeKey);
        resources.remove(compositeKey);
    }

    /**
     * 中文说明：
     * 1. 类目的：`ResourceCompositeKey` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
     * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Adapter / Strategy / Command。
     */
    @Data
    private static class ResourceCompositeKey {
        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final ResourceType resourceType;
        /**
         * 键，用于定位映射、配置或数据项。
         */
        private final String resourceKey;

        /**
         * 功能：获取键。
         * 参数：无。
         * 返回：处理结果。
         */
        public ResourceCompositeKey getSystemKey() {
            return new ResourceCompositeKey(TenantId.SYS_TENANT_ID, resourceType, resourceKey);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTransportResourceCache` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
