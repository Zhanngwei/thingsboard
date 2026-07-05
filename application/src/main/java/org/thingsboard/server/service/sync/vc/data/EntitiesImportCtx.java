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
package org.thingsboard.server.service.sync.vc.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.EntityTypeLoadResult;
import org.thingsboard.server.common.data.util.ThrowingRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`EntitiesImportCtx` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
@Slf4j
@Data
public class EntitiesImportCtx {

    /**
     * 请求ID，用于定位对应业务对象。
     */
    private final UUID requestId;
    private final User user;
    /**
     * 版本号ID，用于定位对应业务对象。
     */
    private final String versionId;

    private final Map<EntityType, EntityTypeLoadResult> results = new HashMap<>();
    private final Map<EntityType, Set<EntityId>> importedEntities = new HashMap<>();
    private final Map<EntityId, ReimportTask> toReimport = new HashMap<>();
    private final Map<EntityId, ThrowingRunnable> referenceCallbacks = new HashMap<>();
    private final List<ThrowingRunnable> eventCallbacks = new ArrayList<>();
    private final Map<EntityId, EntityId> externalToInternalIdMap = new HashMap<>();
    private final Set<EntityId> notFoundIds = new HashSet<>();

    private final Set<EntityRelation> relations = new LinkedHashSet<>();

    /**
     * 是否满足`finalImportAttempt`条件。
     */
    private boolean finalImportAttempt = false;
    private EntityImportSettings settings;
    /**
     * `currentImportResult` 字段，保存当前对象的对应属性。
     */
    private EntityImportResult<?> currentImportResult;

    /**
     * 功能：创建 `EntitiesImportCtx` 实例，并初始化必要字段。
     * 参数：
     * - `requestId`：请求ID。
     * - `user`：`user` 参数。
     * - `versionId`：版本号ID。
     * 返回：新创建的对象实例。
     */
    public EntitiesImportCtx(UUID requestId, User user, String versionId) {
        this(requestId, user, versionId, null);
    }

    /**
     * 功能：创建 `EntitiesImportCtx` 实例，并初始化必要字段。
     * 参数：
     * - `requestId`：请求ID。
     * - `user`：`user` 参数。
     * - `versionId`：版本号ID。
     * - `settings`：配置对象。
     * 返回：新创建的对象实例。
     */
    public EntitiesImportCtx(UUID requestId, User user, String versionId, EntityImportSettings settings) {
        this.requestId = requestId;
        this.user = user;
        this.versionId = versionId;
        this.settings = settings;
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantId getTenantId() {
        return user.getTenantId();
    }

    /**
     * 功能：判断名称。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isFindExistingByName() {
        return getSettings().isFindExistingByName();
    }

    /**
     * 功能：判断`Update Relations`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isUpdateRelations() {
        return getSettings().isUpdateRelations();
    }

    /**
     * 功能：判断`Save Attributes`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSaveAttributes() {
        return getSettings().isSaveAttributes();
    }

    /**
     * 功能：判断凭据。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSaveCredentials() {
        return getSettings().isSaveCredentials();
    }

    /**
     * 功能：获取`Internal Id`。
     * 参数：
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    public EntityId getInternalId(EntityId externalId) {
        var result = externalToInternalIdMap.get(externalId);
        log.debug("[{}][{}] Local cache {} for id", externalId.getEntityType(), externalId.getId(), result != null ? "hit" : "miss");
        return result;
    }

    /**
     * 功能：执行 `putInternalId` 对应的处理。
     * 参数：
     * - `externalId`：`externalId`ID。
     * - `internalId`：`internalId`ID。
     * 返回：无。
     */
    public void putInternalId(EntityId externalId, EntityId internalId) {
        log.debug("[{}][{}] Local cache put: {}", externalId.getEntityType(), externalId.getId(), internalId);
        externalToInternalIdMap.put(externalId, internalId);
    }

    /**
     * 功能：保存或创建`Result`。
     * 参数：
     * - `entityType`：实体对象。
     * - `created`：`created` 参数。
     * 返回：无。
     */
    public void registerResult(EntityType entityType, boolean created) {
        EntityTypeLoadResult result = results.computeIfAbsent(entityType, EntityTypeLoadResult::new);
        if (created) {
            result.setCreated(result.getCreated() + 1);
        } else {
            result.setUpdated(result.getUpdated() + 1);
        }
    }

    /**
     * 功能：保存或创建`Deleted`。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：无。
     */
    public void registerDeleted(EntityType entityType) {
        EntityTypeLoadResult result = results.computeIfAbsent(entityType, EntityTypeLoadResult::new);
        result.setDeleted(result.getDeleted() + 1);
    }

    /**
     * 功能：保存或创建`Relations`。
     * 参数：
     * - `values`：值。
     * 返回：无。
     */
    public void addRelations(Collection<EntityRelation> values) {
        relations.addAll(values);
    }

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `externalId`：`externalId`ID。
     * - `tr`：`tr` 参数。
     * 返回：无。
     */
    public void addReferenceCallback(EntityId externalId, ThrowingRunnable tr) {
        if (tr != null) {
            referenceCallbacks.put(externalId, tr);
        }
    }

    /**
     * 功能：保存或创建事件。
     * 参数：
     * - `tr`：`tr` 参数。
     * 返回：无。
     */
    public void addEventCallback(ThrowingRunnable tr) {
        if (tr != null) {
            eventCallbacks.add(tr);
        }
    }

    /**
     * 功能：保存或创建`Not Found`。
     * 参数：
     * - `externalId`：`externalId`ID。
     * 返回：无。
     */
    public void registerNotFound(EntityId externalId) {
        notFoundIds.add(externalId);
    }

    /**
     * 功能：判断`Not Found`。
     * 参数：
     * - `externalId`：`externalId`ID。
     * 返回：判断结果。
     */
    public boolean isNotFound(EntityId externalId) {
        return notFoundIds.contains(externalId);
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`EntitiesImportCtx` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
