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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：
 * 1. `EntitiesExportCtx` 是 ThingsBoard Application 中围绕 `Entities Export Ctx` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `VersionCreateRequest`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
@Data
public abstract class EntitiesExportCtx<R extends VersionCreateRequest> {

    /**
     * 用户对象，用于描述当前业务场景。
     */
    protected final User user;
    protected final CommitGitRequest commit;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    protected final R request;
    private final List<ListenableFuture<Void>> futures;
    /**
     * `externalIdMap`映射关系，用于按键查找对应值。
     */
    private final Map<EntityId, EntityId> externalIdMap;

    /**
     * 功能：创建 `EntitiesExportCtx` 实例，并初始化必要字段。
     * 参数：
     * - `user`：`user` 参数。
     * - `commit`：`commit` 参数。
     * - `request`：请求对象。
     * 返回：新创建的对象实例。
     */
    public EntitiesExportCtx(User user, CommitGitRequest commit, R request) {
        this.user = user;
        this.commit = commit;
        this.request = request;
        this.futures = new ArrayList<>();
        this.externalIdMap = new HashMap<>();
    }

    /**
     * 功能：创建 `EntitiesExportCtx` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    protected <T extends R> EntitiesExportCtx(EntitiesExportCtx<T> other) {
        this.user = other.getUser();
        this.commit = other.getCommit();
        this.request = other.getRequest();
        this.futures = other.getFutures();
        this.externalIdMap = other.getExternalIdMap();
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * 返回：无。
     */
    public void add(ListenableFuture<Void> future) {
        futures.add(future);
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
     * 功能：构建配置。
     * 参数：
     * - `config`：配置对象。
     * 返回：匹配的数据集合。
     */
    protected static EntityExportSettings buildExportSettings(VersionCreateConfig config) {
        return EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .build();
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public abstract EntityExportSettings getSettings();

    /**
     * 功能：获取`External Id`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    public <ID extends EntityId> ID getExternalId(ID internalId) {
        var result = externalIdMap.get(internalId);
        log.debug("[{}][{}] Local cache {} for id", internalId.getEntityType(), internalId.getId(), result != null ? "hit" : "miss");
        return (ID) result;
    }

    /**
     * 功能：执行 `putExternalId` 对应的处理。
     * 参数：
     * - `internalId`：`internalId`ID。
     * - `externalId`：`externalId`ID。
     * 返回：无。
     */
    public void putExternalId(EntityId internalId, EntityId externalId) {
        log.debug("[{}][{}] Local cache put: {}", internalId.getEntityType(), internalId.getId(), externalId);
        externalIdMap.put(internalId, externalId != null ? externalId : internalId);
    }
}
