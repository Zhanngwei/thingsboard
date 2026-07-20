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

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `GitRepository` 是 ThingsBoard Common 中负责 `Git` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
public class GitRepository {

    /**
     * Git 仓库句柄，表示当前对象的对应属性。
     */
    private final Git git;
    private final AuthHandler authHandler;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final RepositorySettings settings;

    /**
     * 目录，用于定位本地文件或目录。
     */
    @Getter
    private final String directory;

    /**
     * `headId`ID，用于定位对应业务对象。
     */
    private ObjectId headId;

    /**
     * 功能：创建 `GitRepository` 实例，并初始化必要字段。
     * 参数：
     * - `git`：`git` 参数。
     * - `settings`：配置对象。
     * - `authHandler`：处理器对象。
     * - `directory`：`directory` 参数。
     * 返回：新创建的对象实例。
     */
    private GitRepository(Git git, RepositorySettings settings, AuthHandler authHandler, String directory) {
        this.git = git;
        this.settings = settings;
        this.authHandler = authHandler;
        this.directory = directory;
    }

    /**
     * 功能：执行 `clone` 对应的处理。
     * 参数：
     * - `settings`：配置对象。
     * - `directory`：`directory` 参数。
     * 返回：处理结果。
     */
    public static GitRepository clone(RepositorySettings settings, File directory) throws GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(settings.getRepositoryUri())
                .setDirectory(directory)
                .setNoCheckout(true);
        AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        authHandler.configureCommand(cloneCommand);
        Git git = cloneCommand.call();
        return new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
    }

    /**
     * 功能：执行 `open` 对应的处理。
     * 参数：
     * - `directory`：`directory` 参数。
     * - `settings`：配置对象。
     * 返回：处理结果。
     */
    public static GitRepository open(File directory, RepositorySettings settings) throws IOException {
        Git git = Git.open(directory);
        AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        return new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
    }

    /**
     * 功能：执行 `test` 对应的处理。
     * 参数：
     * - `settings`：配置对象。
     * - `directory`：`directory` 参数。
     * 返回：无。
     */
    public static void test(RepositorySettings settings, File directory) throws Exception {
        AuthHandler authHandler = AuthHandler.createFor(settings, directory);
        if (settings.isReadOnly()) {
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository().setRemote(settings.getRepositoryUri());
            authHandler.configureCommand(lsRemoteCommand);
            lsRemoteCommand.call();
        } else {
            Files.createDirectories(directory.toPath());
            try {
                Git git = Git.init().setDirectory(directory).call();
                GitRepository repository = new GitRepository(git, settings, authHandler, directory.getAbsolutePath());
                repository.execute(repository.git.remoteAdd()
                        .setName("origin")
                        .setUri(new URIish(settings.getRepositoryUri())));
                repository.push("", UUID.randomUUID().toString()); // trying to delete non-existing branch on remote repo
            } finally {
                try {
                    FileUtils.forceDelete(directory);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 功能：执行 `fetch` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void fetch() throws GitAPIException {
        FetchResult result = execute(git.fetch()
                .setRemoveDeletedRefs(true));
        Ref head = result.getAdvertisedRef(Constants.HEAD);
        if (head != null) {
            this.headId = head.getObjectId();
        }
    }

    /**
     * 功能：删除或清理分支名称。
     * 参数：
     * - `branch`：`branch` 参数。
     * 返回：无。
     */
    public void deleteLocalBranchIfExists(String branch) throws GitAPIException {
        execute(git.branchDelete()
                .setBranchNames(branch)
                .setForce(true));
    }

    /**
     * 功能：执行 `resetAndClean` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void resetAndClean() throws GitAPIException {
        execute(git.reset()
                .setMode(ResetCommand.ResetType.HARD));
        execute(git.clean()
                .setForce(true)
                .setCleanDirectories(true));
    }

    /**
     * 功能：执行 `merge` 对应的处理。
     * 参数：
     * - `branch`：`branch` 参数。
     * 返回：无。
     */
    public void merge(String branch) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            throw new IllegalArgumentException("Branch not found");
        }
        execute(git.merge()
                .include(branchId));
    }

    /**
     * 功能：获取`Remote Branches`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<BranchInfo> listRemoteBranches() throws GitAPIException {
        return execute(git.branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)).stream()
                .filter(ref -> !ref.getName().equals(Constants.HEAD))
                .map(this::toBranchInfo)
                .distinct().collect(Collectors.toList());
    }

    /**
     * 功能：获取`Commits`。
     * 参数：
     * - `branch`：`branch` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Commit> listCommits(String branch, PageLink pageLink) throws IOException, GitAPIException {
        return listCommits(branch, null, pageLink);
    }

    /**
     * 功能：获取`Commits`。
     * 参数：
     * - `branch`：`branch` 参数。
     * - `path`：文件或资源路径。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Commit> listCommits(String branch, String path, PageLink pageLink) throws IOException, GitAPIException {
        ObjectId branchId = resolve("origin/" + branch);
        if (branchId == null) {
            return new PageData<>();
        }
        LogCommand command = git.log()
                .add(branchId);

        command.setRevFilter(new CommitFilter(pageLink.getTextSearch(), settings.isShowMergeCommits()));
        if (StringUtils.isNotEmpty(path)) {
            command.addPath(path);
        }

        Iterable<RevCommit> commits = execute(command);
        return iterableToPageData(commits, this::toCommit, pageLink, revCommitComparatorFunction);
    }

    /**
     * 功能：获取`Files At Commit`。
     * 参数：
     * - `commitId`：`commitId`ID。
     * 返回：匹配的数据集合。
     */
    public List<String> listFilesAtCommit(String commitId) throws IOException {
        return listFilesAtCommit(commitId, null);
    }

    /**
     * 功能：获取`Files At Commit`。
     * 参数：
     * - `commitId`：`commitId`ID。
     * - `path`：文件或资源路径。
     * 返回：匹配的数据集合。
     */
    public List<String> listFilesAtCommit(String commitId, String path) throws IOException {
        List<String> files = new ArrayList<>();
        RevCommit revCommit = resolveCommit(commitId);
        try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
            treeWalk.reset(revCommit.getTree().getId());
            if (StringUtils.isNotEmpty(path)) {
                treeWalk.setFilter(PathFilter.create(path));
            }
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                files.add(treeWalk.getPathString());
            }
        }
        return files;
    }


    /**
     * 功能：获取文件。
     * 参数：
     * - `file`：`file` 参数。
     * - `commitId`：`commitId`ID。
     * 返回：文本结果。
     */
    public String getFileContentAtCommit(String file, String commitId) throws IOException {
        RevCommit revCommit = resolveCommit(commitId);
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), file, revCommit.getTree())) {
            if (treeWalk == null) {
                throw new IllegalArgumentException("File not found");
            }
            ObjectId blobId = treeWalk.getObjectId(0);
            try (ObjectReader objectReader = git.getRepository().newObjectReader()) {
                ObjectLoader objectLoader = objectReader.open(blobId);
                try {
                    byte[] bytes = objectLoader.getBytes();
                    return new String(bytes, StandardCharsets.UTF_8);
                } catch (LargeObjectException e) {
                    throw new RuntimeException("File " + file + " is too big to load");
                }
            }
        }
    }


    /**
     * 功能：保存或创建分支名称。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    public void createAndCheckoutOrphanBranch(String name) throws GitAPIException {
        execute(git.checkout()
                .setOrphan(true)
                .setForced(true)
                .setName(name));
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `filesPattern`：`filesPattern` 参数。
     * 返回：无。
     */
    public void add(String filesPattern) throws GitAPIException {
        execute(git.add().setUpdate(true).addFilepattern(filesPattern));
        execute(git.add().addFilepattern(filesPattern));
    }

    /**
     * 功能：执行 `status` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public Status status() throws GitAPIException {
        org.eclipse.jgit.api.Status status = execute(git.status());
        Set<String> modified = new HashSet<>();
        modified.addAll(status.getModified());
        modified.addAll(status.getChanged());
        return new Status(status.getAdded(), modified, status.getRemoved());
    }

    /**
     * 功能：执行 `commit` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * - `authorName`：名称。
     * - `authorEmail`：`authorEmail` 参数。
     * 返回：处理结果。
     */
    public Commit commit(String message, String authorName, String authorEmail) throws GitAPIException {
        RevCommit revCommit = execute(git.commit()
                .setAuthor(authorName, authorEmail)
                .setMessage(message));
        return toCommit(revCommit);
    }


    /**
     * 功能：执行 `push` 对应的处理。
     * 参数：
     * - `localBranch`：`localBranch` 参数。
     * - `remoteBranch`：`remoteBranch` 参数。
     * 返回：无。
     */
    public void push(String localBranch, String remoteBranch) throws GitAPIException {
        execute(git.push()
                .setRefSpecs(new RefSpec(localBranch + ":" + remoteBranch)));
    }

    /**
     * 功能：获取`Contents Diff`。
     * 参数：
     * - `content1`：`content1` 参数。
     * - `content2`：`content2` 参数。
     * 返回：文本结果。
     */
    public String getContentsDiff(String content1, String content2) throws IOException {
        RawText rawContent1 = new RawText(content1.getBytes());
        RawText rawContent2 = new RawText(content2.getBytes());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(git.getRepository());

        EditList edits = new EditList();
        edits.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, rawContent1, rawContent2));
        diffFormatter.format(edits, rawContent1, rawContent2);
        return out.toString();
    }

    /**
     * 功能：获取`Diff List`。
     * 参数：
     * - `commit1`：`commit1` 参数。
     * - `commit2`：`commit2` 参数。
     * - `path`：文件或资源路径。
     * 返回：匹配的数据集合。
     */
    public List<Diff> getDiffList(String commit1, String commit2, String path) throws IOException {
        ObjectReader reader = git.getRepository().newObjectReader();

        CanonicalTreeParser tree1Iter = new CanonicalTreeParser();
        ObjectId tree1 = resolveCommit(commit1).getTree();
        tree1Iter.reset(reader, tree1);

        CanonicalTreeParser tree2Iter = new CanonicalTreeParser();
        ObjectId tree2 = resolveCommit(commit2).getTree();
        tree2Iter.reset(reader, tree2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(git.getRepository());
        if (StringUtils.isNotEmpty(path)) {
            diffFormatter.setPathFilter(PathFilter.create(path));
        }

        return diffFormatter.scan(tree1, tree2).stream()
                .map(diffEntry -> {
                    Diff diff = new Diff();
                    try {
                        out.reset();
                        diffFormatter.format(diffEntry);
                        diff.setDiffStringValue(out.toString());
                        diff.setFilePath(diffEntry.getChangeType() != DiffEntry.ChangeType.DELETE ? diffEntry.getNewPath() : diffEntry.getOldPath());
                        diff.setChangeType(diffEntry.getChangeType());
                        try {
                            diff.setFileContentAtCommit1(getFileContentAtCommit(diff.getFilePath(), commit1));
                        } catch (IllegalArgumentException ignored) {
                        }
                        try {
                            diff.setFileContentAtCommit2(getFileContentAtCommit(diff.getFilePath(), commit2));
                        } catch (IllegalArgumentException ignored) {
                        }
                        return diff;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 功能：执行 `toBranchInfo` 对应的处理。
     * 参数：
     * - `ref`：`ref` 参数。
     * 返回：处理结果。
     */
    private BranchInfo toBranchInfo(Ref ref) {
        String name = org.eclipse.jgit.lib.Repository.shortenRefName(ref.getName());
        String branchName = StringUtils.removeStart(name, "origin/");
        boolean isDefault = this.headId != null && this.headId.equals(ref.getObjectId());
        return new BranchInfo(branchName, isDefault);
    }

    /**
     * 功能：执行 `toCommit` 对应的处理。
     * 参数：
     * - `revCommit`：`revCommit` 参数。
     * 返回：处理结果。
     */
    private Commit toCommit(RevCommit revCommit) {
        return new Commit(revCommit.getCommitTime() * 1000l, revCommit.getName(),
                revCommit.getFullMessage(), revCommit.getAuthorIdent().getName(), revCommit.getAuthorIdent().getEmailAddress());
    }

    /**
     * 功能：执行 `resolveCommit` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    private RevCommit resolveCommit(String id) throws IOException {
        return git.getRepository().parseCommit(resolve(id));
    }

    /**
     * 功能：执行 `resolve` 对应的处理。
     * 参数：
     * - `rev`：`rev` 参数。
     * 返回：处理结果。
     */
    private ObjectId resolve(String rev) throws IOException {
        ObjectId result = git.getRepository().resolve(rev);
        if (result == null) {
            throw new IllegalArgumentException("Failed to parse git revision string: \"" + rev + "\"");
        }
        return result;
    }

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `command`：`command` 参数。
     * 返回：处理结果。
     */
    private <C extends GitCommand<T>, T> T execute(C command) throws GitAPIException {
        if (command instanceof TransportCommand) {
            authHandler.configureCommand((TransportCommand) command);
        }
        return command.call();
    }

    private static final Function<PageLink, Comparator<RevCommit>> revCommitComparatorFunction = pageLink -> {
        SortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder != null
                && sortOrder.getProperty().equals("timestamp")
                && SortOrder.Direction.ASC.equals(sortOrder.getDirection())) {
            return Comparator.comparingInt(RevCommit::getCommitTime);
        }
        return null;
    };

    /**
     * 功能：执行 `iterableToPageData` 对应的处理。
     * 参数：
     * - `iterable`：`iterable` 参数。
     * - `mapper`：`mapper` 参数。
     * - `pageLink`：`pageLink` 参数。
     * - `comparatorFunction`：`comparatorFunction` 参数。
     * 返回：匹配的数据集合。
     */
    private static <T, R> PageData<R> iterableToPageData(Iterable<T> iterable,
                                                         Function<? super T, ? extends R> mapper,
                                                         PageLink pageLink,
                                                         Function<PageLink, Comparator<T>> comparatorFunction) {
        iterable = Streams.stream(iterable).collect(Collectors.toList());
        int totalElements = Iterables.size(iterable);
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        int limit = startIndex + pageLink.getPageSize();
        if (comparatorFunction != null) {
            Comparator<T> comparator = comparatorFunction.apply(pageLink);
            if (comparator != null) {
                iterable = Ordering.from(comparator).immutableSortedCopy(iterable);
            }
        }
        iterable = Iterables.limit(iterable, limit);
        if (startIndex < totalElements) {
            iterable = Iterables.skip(iterable, startIndex);
        } else {
            iterable = Collections.emptyList();
        }
        List<R> data = Streams.stream(iterable).map(mapper)
                .collect(Collectors.toList());
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + data.size();
        return new PageData<>(data, totalPages, totalElements, hasNext);
    }

    /**
     * 中文说明：
     * 1. `AuthHandler` 是 ThingsBoard Common 中处理认证的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 它直接协作于事件源、上下文对象和后续处理组件。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    @RequiredArgsConstructor
    private static class AuthHandler {
        /**
         * 凭据，用于按场景创建或提供目标对象。
         */
        private final CredentialsProvider credentialsProvider;
        private final SshdSessionFactory sshSessionFactory;

        /**
         * 功能：保存或创建`For`。
         * 参数：
         * - `settings`：配置对象。
         * - `directory`：`directory` 参数。
         * 返回：处理结果。
         */
        protected static AuthHandler createFor(RepositorySettings settings, File directory) {
            CredentialsProvider credentialsProvider = null;
            SshdSessionFactory sshSessionFactory = null;
            if (RepositoryAuthMethod.USERNAME_PASSWORD.equals(settings.getAuthMethod())) {
                credentialsProvider = newCredentialsProvider(settings.getUsername(), settings.getPassword());
            } else if (RepositoryAuthMethod.PRIVATE_KEY.equals(settings.getAuthMethod())) {
                sshSessionFactory = newSshdSessionFactory(settings.getPrivateKey(), settings.getPrivateKeyPassword(), directory);
            }
            return new AuthHandler(credentialsProvider, sshSessionFactory);
        }

        /**
         * 功能：执行 `configureCommand` 对应的处理。
         * 参数：
         * - `command`：`command` 参数。
         * 返回：无。
         */
        protected void configureCommand(TransportCommand command) {
            if (credentialsProvider != null) {
                command.setCredentialsProvider(credentialsProvider);
            }
            if (sshSessionFactory != null) {
                command.setTransportConfigCallback(transport -> {
                    if (transport instanceof SshTransport) {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sshSessionFactory);
                    }
                });
            }
        }

        /**
         * 功能：执行 `newCredentialsProvider` 对应的处理。
         * 参数：
         * - `username`：名称。
         * - `password`：`password` 参数。
         * 返回：处理结果。
         */
        private static CredentialsProvider newCredentialsProvider(String username, String password) {
            return new UsernamePasswordCredentialsProvider(username, password == null ? "" : password);
        }

        /**
         * 功能：执行 `newSshdSessionFactory` 对应的处理。
         * 参数：
         * - `privateKey`：键。
         * - `password`：`password` 参数。
         * - `directory`：`directory` 参数。
         * 返回：处理结果。
         */
        private static SshdSessionFactory newSshdSessionFactory(String privateKey, String password, File directory) {
            SshdSessionFactory sshSessionFactory = null;
            if (StringUtils.isNotBlank(privateKey)) {
                Iterable<KeyPair> keyPairs = loadKeyPairs(privateKey, password);
                sshSessionFactory = new SshdSessionFactoryBuilder()
                        .setPreferredAuthentications("publickey")
                        .setDefaultKeysProvider(file -> keyPairs)
                        .setHomeDirectory(directory)
                        .setSshDirectory(directory)
                        .setServerKeyDatabase((file, file2) -> new ServerKeyDatabase() {
                            @Override
                            public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress, Configuration config) {
                                return Collections.emptyList();
                            }

                            @Override
                            public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey, Configuration config, CredentialsProvider provider) {
                                return true;
                            }
                        })
                        .build(new JGitKeyCache());
            }
            return sshSessionFactory;
        }

        /**
         * 功能：获取键。
         * 参数：
         * - `privateKeyContent`：键。
         * - `password`：`password` 参数。
         * 返回：匹配的数据集合。
         */
        private static Iterable<KeyPair> loadKeyPairs(String privateKeyContent, String password) {
            Iterable<KeyPair> keyPairs = null;
            try {
                keyPairs = SecurityUtils.loadKeyPairIdentities(null,
                        null, new ByteArrayInputStream(privateKeyContent.getBytes()), (session, resourceKey, retryIndex) -> password);
            } catch (Exception e) {}
            if (keyPairs == null) {
                throw new IllegalArgumentException("Failed to load ssh private key");
            }
            return keyPairs;
        }
    }

    /**
     * 中文说明：
     * 1. `CommitFilter` 是 ThingsBoard Common 中处理 `Commit Filter` 的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `RevFilter`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    private static class CommitFilter extends RevFilter {

        /**
         * 搜索文本，表示当前对象的对应属性。
         */
        private final String textSearch;
        private final boolean showMergeCommits;

        CommitFilter(String textSearch, boolean showMergeCommits) {
            this.textSearch = textSearch.toLowerCase();
            this.showMergeCommits = showMergeCommits;
        }

        /**
         * 功能：执行 `include` 对应的处理。
         * 参数：
         * - `walker`：`walker` 参数。
         * - `c`：`c` 参数。
         * 返回：判断结果。
         */
        @Override
        public boolean include(RevWalk walker, RevCommit c) {
            return (showMergeCommits || c.getParentCount() < 2) && (StringUtils.isEmpty(textSearch)
                    || c.getFullMessage().toLowerCase().contains(textSearch));
        }

        /**
         * 功能：执行 `clone` 对应的处理。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public RevFilter clone() {
            return this;
        }

        /**
         * 功能：执行 `requiresCommitBody` 对应的处理。
         * 参数：无。
         * 返回：判断结果。
         */
        @Override
        public boolean requiresCommitBody() {
            return false;
        }

    }

    /**
     * 中文说明：
     * 1. `Commit` 是 ThingsBoard Common 中围绕 `Commit` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Data
    public static class Commit {
        /**
         * 时间戳，用于标识当前数据或事件发生的时间。
         */
        private final long timestamp;
        private final String id;
        /**
         * 消息，承载当前步骤需要处理的内容。
         */
        private final String message;
        private final String authorName;
        /**
         * 作者邮箱，用于展示或标识当前对象。
         */
        private final String authorEmail;
    }

    /**
     * 中文说明：
     * 1. `Status` 是 ThingsBoard Common 中围绕状态提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Data
    public static class Status {
        /**
         * `added`集合，用于去重保存或快速判断对象是否存在。
         */
        private final Set<String> added;
        private final Set<String> modified;
        /**
         * `removed`集合，用于去重保存或快速判断对象是否存在。
         */
        private final Set<String> removed;
    }

    /**
     * 中文说明：
     * 1. `Diff` 是 ThingsBoard Common 中围绕 `Diff` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Data
    public static class Diff {
        /**
         * 文件路径，用于定位本地文件或目录。
         */
        private String filePath;
        private DiffEntry.ChangeType changeType;
        /**
         * 文件，用于定位本地文件或目录。
         */
        private String fileContentAtCommit1;
        private String fileContentAtCommit2;
        /**
         * 值，保存当前处理得到的具体内容。
         */
        private String diffStringValue;
    }

}
