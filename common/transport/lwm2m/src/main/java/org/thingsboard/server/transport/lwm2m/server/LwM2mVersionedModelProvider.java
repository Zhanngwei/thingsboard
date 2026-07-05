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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.data.ResourceType.LWM2M_MODEL;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mVersionedModelProvider` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2mVersionedModelProvider implements LwM2mModelProvider {

    /**
     * 上下文，用于发起外部调用或协议交互。
     */
    private final LwM2mClientContext lwM2mClientContext;
    private final LwM2mTransportServerHelper helper;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final LwM2mTransportContext context;
    private final ConcurrentMap<TenantId, ConcurrentMap<String, ObjectModel>> models;

    /**
     * 功能：创建 `LwM2mVersionedModelProvider` 实例，并初始化必要字段。
     * 参数：
     * - `lwM2mClientContext`：处理上下文。
     * - `helper`：`helper` 参数。
     * - `context`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public LwM2mVersionedModelProvider(@Lazy LwM2mClientContext lwM2mClientContext, LwM2mTransportServerHelper helper, LwM2mTransportContext context) {
        this.lwM2mClientContext = lwM2mClientContext;
        this.helper = helper;
        this.context = context;
        this.models = new ConcurrentHashMap<>();
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `objectId`：`objectId`ID。
     * - `version`：`version` 参数。
     * 返回：文本结果。
     */
    private String getKeyIdVer(Integer objectId, String version) {
        return objectId != null ? objectId + LWM2M_SEPARATOR_KEY + ((version == null || version.isEmpty()) ? ObjectModel.DEFAULT_VERSION : version) : null;
    }

    /**
     * 功能：获取`Object Model`。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2mModel getObjectModel(Registration registration) {
        return new DynamicModel(registration);
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：无。
     */
    public void evict(TenantId tenantId, String key) {
        if (tenantId.isNullUid()) {
            models.values().forEach(m -> m.remove(key));
        } else {
            models.get(tenantId).remove(key);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`DynamicModel` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private class DynamicModel implements LwM2mModel {
        /**
         * `registration` 字段，保存当前对象的对应属性。
         */
        private final Registration registration;
        private final TenantId tenantId;
        /**
         * 锁，用于保护并发读写的共享状态。
         */
        private final Lock modelsLock;

        /**
         * 功能：创建 `LwM2mVersionedModelProvider` 实例，并初始化必要字段。
         * 参数：
         * - `registration`：`registration` 参数。
         * 返回：新创建的对象实例。
         */
        public DynamicModel(Registration registration) {
            this.registration = registration;
            this.tenantId = lwM2mClientContext.getClientByEndpoint(registration.getEndpoint()).getTenantId();
            this.modelsLock = new ReentrantLock();
            if (tenantId != null) {
                models.computeIfAbsent(tenantId, t -> new ConcurrentHashMap<>());
            }
        }

        /**
         * 功能：获取`Resource Model`。
         * 参数：
         * - `objectId`：`objectId`ID。
         * - `resourceId`：`resourceId`ID。
         * 返回：处理结果。
         */
        @Override
        public ResourceModel getResourceModel(int objectId, int resourceId) {
            try {
                ObjectModel objectModel = getObjectModel(objectId);
                if (objectModel != null)
                    return objectModel.resources.get(resourceId);
                else
                    log.trace("Tenant hasn't such the TbResources: Object model with id [{}/0/{}].", objectId, resourceId);
                return null;
            } catch (Exception e) {
                log.error("", e);
                return null;
            }
        }

        /**
         * 功能：获取`Object Model`。
         * 参数：
         * - `objectId`：`objectId`ID。
         * 返回：处理结果。
         */
        @Override
        public ObjectModel getObjectModel(int objectId) {
            String version = registration.getSupportedVersion(objectId);
            if (version != null) {
                return this.getObjectModelDynamic(objectId, version);
            }
            return null;
        }

        /**
         * 功能：获取`Object Models`。
         * 参数：无。
         * 返回：匹配的数据集合。
         */
        @Override
        public Collection<ObjectModel> getObjectModels() {
            Map<Integer, String> supportedObjects = this.registration.getSupportedObject();
            Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            for (Map.Entry<Integer, String> supportedObject : supportedObjects.entrySet()) {
                ObjectModel objectModel = this.getObjectModelDynamic(supportedObject.getKey(), supportedObject.getValue());
                if (objectModel != null) {
                    result.add(objectModel);
                }
            }
            return result;
        }

        /**
         * 功能：获取`Object Model Dynamic`。
         * 参数：
         * - `objectId`：`objectId`ID。
         * - `version`：`version` 参数。
         * 返回：处理结果。
         */
        private ObjectModel getObjectModelDynamic(Integer objectId, String version) {
            String key = getKeyIdVer(objectId, version);
            ObjectModel objectModel = tenantId != null ? models.get(tenantId).get(key) : null;
            if (tenantId != null && objectModel == null) {
                modelsLock.lock();
                try {
                    objectModel = models.get(tenantId).get(key);
                    if (objectModel == null) {
                        objectModel = getObjectModel(key);
                    }
                    if (objectModel != null) {
                        models.get(tenantId).put(key, objectModel);
                    } else {
                        log.error("Tenant hasn't such the resource: Object model with id [{}] version [{}].", objectId, version);
                    }
                } finally {
                    modelsLock.unlock();
                }
            }

            return objectModel;
        }

        /**
         * 功能：获取`Object Model`。
         * 参数：
         * - `key`：键。
         * 返回：处理结果。
         */
        private ObjectModel getObjectModel(String key) {
            Optional<TbResource> tbResource = context.getTransportResourceCache().get(this.tenantId, LWM2M_MODEL, key);
            return tbResource.map(resource -> helper.parseFromXmlToObjectModel(resource.getData(),
                    key + ".xml")).orElse(null);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mVersionedModelProvider` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
