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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.GitRepository.Diff;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultGitRepositoryService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@ConditionalOnProperty(prefix = "vc", value = "git.service", havingValue = "local", matchIfMissing = true)
@Service
public class DefaultGitRepositoryService implements GitRepositoryService {

    /**
     * 目录，用于定位本地文件或目录。
     */
    @Value("${java.io.tmpdir}/repositories")
    private String defaultFolder;

    /**
     * 目录，用于定位本地文件或目录。
     */
    @Value("${vc.git.repositories-folder:${java.io.tmpdir}/repositories}")
    private String repositoriesFolder;

    private final Map<TenantId, GitRepository> repositories = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(repositoriesFolder)) {
            repositoriesFolder = defaultFolder;
        }
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<TenantId> getActiveRepositoryTenants() {
        return new HashSet<>(repositories.keySet());
    }

    /**
     * 功能：执行 `prepareCommit` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：无。
     */
    @Override
    public void prepareCommit(PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        String branch = commit.getBranch();
        try {
            repository.fetch();

            repository.createAndCheckoutOrphanBranch(commit.getWorkingBranch());
            repository.resetAndClean();

            if (repository.listRemoteBranches().contains(new BranchInfo(branch, false))) {
                repository.merge(branch);
            }
        } catch (IOException | GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    /**
     * 功能：删除或清理目录。
     * 参数：
     * - `commit`：`commit` 参数。
     * - `relativePath`：文件或资源路径。
     * 返回：无。
     */
    @Override
    public void deleteFolderContent(PendingCommit commit, String relativePath) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        FileUtils.deleteDirectory(Path.of(repository.getDirectory(), relativePath).toFile());
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * - `relativePath`：文件或资源路径。
     * - `entityDataJson`：待处理数据。
     * 返回：无。
     */
    @Override
    public void add(PendingCommit commit, String relativePath, String entityDataJson) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        FileUtils.write(Path.of(repository.getDirectory(), relativePath).toFile(), entityDataJson, StandardCharsets.UTF_8);
    }

    /**
     * 功能：执行 `push` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：处理结果。
     */
    @Override
    public VersionCreationResult push(PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        try {
            repository.add(".");

            VersionCreationResult result = new VersionCreationResult();
            GitRepository.Status status = repository.status();
            result.setAdded(status.getAdded().size());
            result.setModified(status.getModified().size());
            result.setRemoved(status.getRemoved().size());

            if (result.getAdded() > 0 || result.getModified() > 0 || result.getRemoved() > 0) {
                GitRepository.Commit gitCommit = repository.commit(commit.getVersionName(), commit.getAuthorName(), commit.getAuthorEmail());
                repository.push(commit.getWorkingBranch(), commit.getBranch());
                result.setVersion(toVersion(gitCommit));
            }
            return result;
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        } finally {
            cleanUp(commit);
        }
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：无。
     */
    @SneakyThrows
    @Override
    public void cleanUp(PendingCommit commit) {
        log.debug("[{}] Cleanup tenant repository started.", commit.getTenantId());
        GitRepository repository = checkRepository(commit.getTenantId());
        try {
            repository.createAndCheckoutOrphanBranch(EntityId.NULL_UUID.toString());
        } catch (Exception e) {
            if (!e.getMessage().contains("NO_CHANGE")) {
                throw e;
            }
        }
        repository.resetAndClean();
        repository.deleteLocalBranchIfExists(commit.getWorkingBranch());
        log.debug("[{}] Cleanup tenant repository completed.", commit.getTenantId());
    }

    /**
     * 功能：执行 `abort` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：无。
     */
    @Override
    public void abort(PendingCommit commit) {
        cleanUp(commit);
    }

    /**
     * 功能：执行 `fetch` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void fetch(TenantId tenantId) throws GitAPIException {
        var repository = checkRepository(tenantId);
        log.debug("[{}] Fetching tenant repository.", tenantId);
        repository.fetch();
        log.debug("[{}] Fetched tenant repository.", tenantId);
    }

    /**
     * 功能：获取文件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relativePath`：文件或资源路径。
     * - `versionId`：版本号ID。
     * 返回：文本结果。
     */
    @Override
    public String getFileContentAtCommit(TenantId tenantId, String relativePath, String versionId) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getFileContentAtCommit(relativePath, versionId);
    }

    /**
     * 功能：获取`Versions Diff List`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `path`：文件或资源路径。
     * - `versionId1`：`versionId1` 参数。
     * - `versionId2`：`versionId2` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Diff> getVersionsDiffList(TenantId tenantId, String path, String versionId1, String versionId2) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getDiffList(versionId1, versionId2, path);
    }

    /**
     * 功能：获取`Contents Diff`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `content1`：`content1` 参数。
     * - `content2`：`content2` 参数。
     * 返回：文本结果。
     */
    @Override
    public String getContentsDiff(TenantId tenantId, String content1, String content2) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getContentsDiff(content1, content2);
    }

    /**
     * 功能：获取`Branches`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<BranchInfo> listBranches(TenantId tenantId) {
        GitRepository repository = checkRepository(tenantId);
        try {
            return repository.listRemoteBranches();
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    /**
     * 功能：校验存取组件。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    private GitRepository checkRepository(TenantId tenantId) {
        GitRepository gitRepository = Optional.ofNullable(repositories.get(tenantId))
                .orElseThrow(() -> new IllegalStateException("Repository is not initialized"));

        if (!Files.exists(Path.of(gitRepository.getDirectory()))) {
            try {
                return cloneRepository(tenantId, gitRepository.getSettings());
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialize the repository: " + e.getMessage(), e);
            }
        }
        return gitRepository;
    }

    /**
     * 功能：获取`Versions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `branch`：`branch` 参数。
     * - `path`：文件或资源路径。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EntityVersion> listVersions(TenantId tenantId, String branch, String path, PageLink pageLink) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listCommits(branch, path, pageLink).mapData(this::toVersion);
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionId`：版本号ID。
     * - `path`：文件或资源路径。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String versionId, String path) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listFilesAtCommit(versionId, path).stream()
                .map(filePath -> {
                    EntityId entityId = fromRelativePath(filePath);
                    VersionedEntityInfo info = new VersionedEntityInfo();
                    info.setExternalId(entityId);
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * 功能：验证存取组件相关场景。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：无。
     */
    @Override
    public void testRepository(TenantId tenantId, RepositorySettings settings) throws Exception {
        Path testDirectory = Path.of(repositoriesFolder, "repo-test-" + UUID.randomUUID());
        GitRepository.test(settings, testDirectory.toFile());
    }

    /**
     * 功能：初始化或启动存取组件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：无。
     */
    @Override
    public void initRepository(TenantId tenantId, RepositorySettings settings) throws Exception {
        testRepository(tenantId, settings);

        clearRepository(tenantId);
        cloneRepository(tenantId, settings);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public RepositorySettings getRepositorySettings(TenantId tenantId) throws Exception {
        var gitRepository = repositories.get(tenantId);
        return gitRepository != null ? gitRepository.getSettings() : null;
    }

    /**
     * 功能：删除或清理存取组件。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void clearRepository(TenantId tenantId) throws IOException {
        GitRepository repository = repositories.get(tenantId);
        if (repository != null) {
            log.debug("[{}] Clear tenant repository started.", tenantId);
            FileUtils.deleteDirectory(new File(repository.getDirectory()));
            repositories.remove(tenantId);
            log.debug("[{}] Clear tenant repository completed.", tenantId);
        }
    }

    /**
     * 功能：执行 `toVersion` 对应的处理。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：处理结果。
     */
    private EntityVersion toVersion(GitRepository.Commit commit) {
        return new EntityVersion(commit.getTimestamp(), commit.getId(), commit.getMessage(), this.getAuthor(commit));
    }

    /**
     * 功能：获取`Author`。
     * 参数：
     * - `commit`：`commit` 参数。
     * 返回：文本结果。
     */
    private String getAuthor(GitRepository.Commit commit) {
        String author = String.format("<%s>", commit.getAuthorEmail());
        if (StringUtils.isNotBlank(commit.getAuthorName())) {
            author = String.format("%s %s", commit.getAuthorName(), author);
        }
        return author;
    }

    /**
     * 功能：执行 `fromRelativePath` 对应的处理。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：处理结果。
     */
    public static EntityId fromRelativePath(String path) {
        EntityType entityType = EntityType.valueOf(StringUtils.substringBefore(path, "/").toUpperCase());
        String entityId = StringUtils.substringBetween(path, "/", ".json");
        return EntityIdFactory.getByTypeAndUuid(entityType, entityId);
    }

    /**
     * 功能：执行 `cloneRepository` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `settings`：配置对象。
     * 返回：处理结果。
     */
    private GitRepository cloneRepository(TenantId tenantId, RepositorySettings settings) throws Exception {
        log.debug("[{}] Init tenant repository started.", tenantId);
        Path repositoryDirectory = Path.of(repositoriesFolder, tenantId.getId().toString());

        if (Files.exists(repositoryDirectory)) {
            FileUtils.forceDelete(repositoryDirectory.toFile());
        }
        Files.createDirectories(repositoryDirectory);
        GitRepository repository = GitRepository.clone(settings, repositoryDirectory.toFile());
        repositories.put(tenantId, repository);
        log.debug("[{}] Init tenant repository completed.", tenantId);
        return repository;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultGitRepositoryService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
