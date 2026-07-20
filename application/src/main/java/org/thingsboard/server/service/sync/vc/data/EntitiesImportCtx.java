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
 * 1. `EntitiesImportCtx` 是 ThingsBoard Application 中围绕 `Entities Import Ctx` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
