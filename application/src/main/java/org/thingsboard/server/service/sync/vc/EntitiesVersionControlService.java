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
package org.thingsboard.server.service.sync.vc;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityDataDiff;
import org.thingsboard.server.common.data.sync.vc.EntityDataInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `EntitiesVersionControlService` 是 ThingsBoard Application 中定义版本控制能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EntitiesVersionControlService {

    /**
     * 功能：保存或创建版本号。
     * 参数：
     * - `user`：`user` 参数。
     * - `request`：请求对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<UUID> saveEntitiesVersion(User user, VersionCreateRequest request) throws Exception;

    /**
     * 功能：获取状态。
     * 参数：
     * - `user`：`user` 参数。
     * - `requestId`：请求ID。
     * 返回：处理结果。
     */
    VersionCreationResult getVersionCreateStatus(User user, UUID requestId) throws ThingsboardException;

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `externalId`：`externalId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<PageData<EntityVersion>> listEntityVersions(TenantId tenantId, String branch, EntityId externalId, PageLink pageLink) throws Exception;

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `entityType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<PageData<EntityVersion>> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) throws Exception;

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink) throws Exception;

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * - `entityType`：实体对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType) throws Exception;

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<VersionedEntityInfo>> listAllEntitiesAtVersion(TenantId tenantId, String versionId) throws Exception;

    /**
     * 功能：获取版本号。
     * 参数：
     * - `user`：`user` 参数。
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    UUID loadEntitiesVersion(User user, VersionLoadRequest request) throws Exception;

    /**
     * 功能：获取状态。
     * 参数：
     * - `user`：`user` 参数。
     * - `requestId`：请求ID。
     * 返回：处理结果。
     */
    VersionLoadResult getVersionLoadStatus(User user, UUID requestId) throws ThingsboardException;

    /**
     * 功能：执行 `compareEntityDataToVersion` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<EntityDataDiff> compareEntityDataToVersion(User user, EntityId entityId, String versionId) throws Exception;

    /**
     * 功能：获取`Branches`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId) throws Exception;

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    RepositorySettings getVersionControlSettings(TenantId tenantId);

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionControlSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<RepositorySettings> saveVersionControlSettings(TenantId tenantId, RepositorySettings versionControlSettings);

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> deleteVersionControlSettings(TenantId tenantId) throws Exception;

    /**
     * 功能：校验版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：判断结果。
     */
    ListenableFuture<Void> checkVersionControlAccess(TenantId tenantId, RepositorySettings settings) throws Exception;

    /**
     * 功能：执行 `autoCommit` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception;

    /**
     * 功能：执行 `autoCommit` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception;

    /**
     * 功能：获取实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<EntityDataInfo> getEntityDataInfo(User user, EntityId entityId, String versionId);

}
