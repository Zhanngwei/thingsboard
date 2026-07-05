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
 * 1. 类目的：`GitVersionControlQueueService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
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

/*
 * 本类总结：
 * 1. 核心职责：`GitVersionControlQueueService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
