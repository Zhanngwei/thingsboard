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
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.EntityVersionsDiff;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.gen.transport.TransportProtos.VersionControlResponseMsg;
import org.thingsboard.server.service.sync.vc.data.CommitGitRequest;

import java.util.List;

/**
 * 中文说明：
 * 1. `GitVersionControlQueueService` 是 ThingsBoard Application 中定义队列能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface GitVersionControlQueueService {

    /**
     * 功能：执行 `prepareCommit` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `request`：请求对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<CommitGitRequest> prepareCommit(User user, VersionCreateRequest request);

    /**
     * 功能：保存或创建`To Commit`。
     * 参数：
     * - `commit`：`commit` 参数。
     * - `entityData`：待处理数据。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> addToCommit(CommitGitRequest commit, EntityExportData<ExportableEntity<EntityId>> entityData);

    /**
     * 功能：删除或清理`All`。
     * 参数：
     * - `pendingCommit`：`pendingCommit` 参数。
     * - `entityType`：实体对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> deleteAll(CommitGitRequest pendingCommit, EntityType entityType);

    /**
     * 功能：执行 `push` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<VersionCreationResult> push(CommitGitRequest commit);

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink);

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `entityType`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink);

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `entityId`：实体IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityId entityId, PageLink pageLink);

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * - `entityType`：实体对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType);

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId);

    /**
     * 功能：获取`Branches`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId);

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<EntityExportData> getEntity(TenantId tenantId, String versionId, EntityId entityId);

    /**
     * 功能：获取`Entities`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * - `entityType`：实体对象。
     * - `offset`：偏移量。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityExportData>> getEntities(TenantId tenantId, String versionId, EntityType entityType, int offset, int limit);

    /**
     * 功能：获取`Versions Diff`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `externalId`：`externalId`ID。
     * - `versionId1`：`versionId1` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntityVersionsDiff>> getVersionsDiff(TenantId tenantId, EntityType entityType, EntityId externalId, String versionId1, String versionId2);

    /**
     * 功能：初始化或启动存取组件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> initRepository(TenantId tenantId, RepositorySettings settings);

    /**
     * 功能：验证存取组件相关场景。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> testRepository(TenantId tenantId, RepositorySettings settings);

    /**
     * 功能：删除或清理存取组件。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> clearRepository(TenantId tenantId);

    /**
     * 功能：处理响应。
     * 参数：
     * - `vcResponseMsg`：响应对象。
     * 返回：无。
     */
    void processResponse(VersionControlResponseMsg vcResponseMsg);
}
