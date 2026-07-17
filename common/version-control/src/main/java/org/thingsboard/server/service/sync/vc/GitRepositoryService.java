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
 * 1. `GitRepositoryService` 是 ThingsBoard Common 中定义 `Git` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
