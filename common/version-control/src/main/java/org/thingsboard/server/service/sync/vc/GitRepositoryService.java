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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.GitRepository.Diff;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`GitRepositoryService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface GitRepositoryService {

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    Set<TenantId> getActiveRepositoryTenants();

    /**
     * 功能：执行 `prepareCommit` 对应的处理。
     * 参数：
     * - `pendingCommit`：`pendingCommit` 参数。
     * 返回：无。
     */
    void prepareCommit(PendingCommit pendingCommit);

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `path`：文件或资源路径。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityVersion> listVersions(TenantId tenantId, String branch, String path, PageLink pageLink) throws Exception;

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * - `path`：文件或资源路径。
     * 返回：匹配的数据集合。
     */
    List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String versionId, String path) throws Exception;

    /**
     * 功能：验证存取组件相关场景。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：无。
     */
    void testRepository(TenantId tenantId, RepositorySettings settings) throws Exception;

    /**
     * 功能：初始化或启动存取组件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：无。
     */
    void initRepository(TenantId tenantId, RepositorySettings settings) throws Exception;

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    RepositorySettings getRepositorySettings(TenantId tenantId) throws Exception;

    /**
     * 功能：删除或清理存取组件。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void clearRepository(TenantId tenantId) throws IOException;

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * - `relativePath`：文件或资源路径。
     * - `entityDataJson`：待处理数据。
     * 返回：无。
     */
    void add(PendingCommit commit, String relativePath, String entityDataJson) throws IOException;

    /**
     * 功能：删除或清理目录。
     * 参数：
     * - `commit`：`commit` 参数。
     * - `relativePath`：文件或资源路径。
     * 返回：无。
     */
    void deleteFolderContent(PendingCommit commit, String relativePath) throws IOException;

    /**
     * 功能：执行 `push` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：处理结果。
     */
    VersionCreationResult push(PendingCommit commit);

    /**
     * 功能：删除或清理`Up`。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：无。
     */
    void cleanUp(PendingCommit commit);

    /**
     * 功能：执行 `abort` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：无。
     */
    void abort(PendingCommit commit);

    /**
     * 功能：获取`Branches`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    List<BranchInfo> listBranches(TenantId tenantId);

    /**
     * 功能：获取文件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relativePath`：文件或资源路径。
     * - `versionId`：版本号ID。
     * 返回：文本结果。
     */
    String getFileContentAtCommit(TenantId tenantId, String relativePath, String versionId) throws IOException;

    /**
     * 功能：获取`Versions Diff List`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `path`：文件或资源路径。
     * - `versionId1`：`versionId1` 参数。
     * - `versionId2`：`versionId2` 参数。
     * 返回：匹配的数据集合。
     */
    List<Diff> getVersionsDiffList(TenantId tenantId, String path, String versionId1, String versionId2) throws IOException;

    /**
     * 功能：获取`Contents Diff`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `content1`：`content1` 参数。
     * - `content2`：`content2` 参数。
     * 返回：文本结果。
     */
    String getContentsDiff(TenantId tenantId, String content1, String content2) throws IOException;

    /**
     * 功能：执行 `fetch` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void fetch(TenantId tenantId) throws GitAPIException;
}

/*
 * 本类总结：
 * 1. 核心职责：`GitRepositoryService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
