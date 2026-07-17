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
 * 1. `LwM2mVersionedModelProvider` 是 ThingsBoard Common Transport 中创建或提供 LwM2M 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `LwM2mModelProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
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
     * 1. `DynamicModel` 是 ThingsBoard Common Transport 中负责 `Dynamic Model` 接入或传输适配的类型。
     * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
     * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
     * 4. 直接依赖的类型边界包括 `LwM2mModel`。
     * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
     * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
