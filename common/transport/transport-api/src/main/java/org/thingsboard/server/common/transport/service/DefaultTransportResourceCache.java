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
 * 1. `DefaultTransportResourceCache` 是 ThingsBoard Common Transport 中管理传输层缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `TransportResourceCache`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
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
     * 1. `ResourceCompositeKey` 是 ThingsBoard Common Transport 中负责资源接入或传输适配的类型。
     * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
     * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
     * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
     * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
     * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
