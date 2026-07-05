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
package org.thingsboard.server.queue.discovery;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.OtherServiceShutdownEvent;
import org.thingsboard.server.queue.util.AfterStartUp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;

/**
 * 中文说明：
 * 1. 类目的：`ZkDiscoveryService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ZkDiscoveryService implements DiscoveryService, PathChildrenCacheListener {

    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Value("${zk.url}")
    private String zkUrl;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${zk.retry_interval_ms}")
    private Integer zkRetryInterval;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${zk.connection_timeout_ms}")
    private Integer zkConnectionTimeout;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${zk.session_timeout_ms}")
    private Integer zkSessionTimeout;
    /**
     * `zkDir` 字段，保存当前对象的对应属性。
     */
    @Value("${zk.zk_dir}")
    private String zkDir;
    /**
     * 延迟时间，用于控制时间范围或等待时长。
     */
    @Value("${zk.recalculate_delay:0}")
    private Long recalculateDelay;

    /**
     * `delayedTasks`映射关系，用于按键查找对应值。
     */
    protected final ConcurrentHashMap<String, ScheduledFuture<?>> delayedTasks;

    /**
     * 事件，表示当前对象的对应属性。
     */
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    private final PartitionService partitionService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private ScheduledExecutorService zkExecutorService;
    private CuratorFramework client;
    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private PathChildrenCache cache;
    private String nodePath;
    /**
     * `zkNodesDir` 字段，保存当前对象的对应属性。
     */
    private String zkNodesDir;

    /**
     * 当前处理是否已经停止。
     */
    private volatile boolean stopped = true;

    /**
     * 功能：创建 `ZkDiscoveryService` 实例，并初始化必要字段。
     * 参数：
     * - `applicationEventPublisher`：`applicationEventPublisher` 参数。
     * - `serviceInfoProvider`：服务对象。
     * - `partitionService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public ZkDiscoveryService(ApplicationEventPublisher applicationEventPublisher,
                              TbServiceInfoProvider serviceInfoProvider,
                              PartitionService partitionService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        delayedTasks = new ConcurrentHashMap<>();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        log.info("Initializing...");
        Assert.hasLength(zkUrl, missingProperty("zk.url"));
        Assert.notNull(zkRetryInterval, missingProperty("zk.retry_interval_ms"));
        Assert.notNull(zkConnectionTimeout, missingProperty("zk.connection_timeout_ms"));
        Assert.notNull(zkSessionTimeout, missingProperty("zk.session_timeout_ms"));

        zkExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("zk-discovery"));

        log.info("Initializing discovery service using ZK connect string: {}", zkUrl);

        zkNodesDir = zkDir + "/nodes";
        initZkClient();
    }

    /**
     * 功能：获取`Other Servers`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TransportProtos.ServiceInfo> getOtherServers() {
        return cache.getCurrentData().stream()
                .filter(cd -> !cd.getPath().equals(nodePath))
                .map(cd -> {
                    try {
                        return TransportProtos.ServiceInfo.parseFrom(cd.getData());
                    } catch (NoSuchElementException | InvalidProtocolBufferException e) {
                        log.error("Failed to decode ZK node", e);
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 功能：判断`Monolith`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isMonolith() {
        return false;
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.DISCOVERY_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (stopped) {
            log.debug("Ignoring application ready event. Service is stopped.");
            return;
        } else {
            log.info("Received application ready event. Starting current ZK node.");
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.debug("Ignoring application ready event, ZK client is not started, ZK client state [{}]", client.getState());
            return;
        }
        log.info("Going to publish current server...");
        publishCurrentServer();
        log.info("Going to recalculate partitions...");
        recalculatePartitions();

        zkExecutorService.scheduleAtFixedRate(this::publishCurrentServer, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 功能：发送或提交服务端。
     * 参数：无。
     * 返回：无。
     */
    @SneakyThrows
    public synchronized void publishCurrentServer() {
        TransportProtos.ServiceInfo self = serviceInfoProvider.getServiceInfo();
        if (currentServerExists()) {
            log.trace("[{}] Updating ZK node for current instance: {}", self.getServiceId(), nodePath);
            client.setData().forPath(nodePath, serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo().toByteArray());
        } else {
            try {
                log.info("[{}] Creating ZK node for current instance", self.getServiceId());
                nodePath = client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(zkNodesDir + "/", self.toByteArray());
                log.info("[{}] Created ZK node for current instance: {}", self.getServiceId(), nodePath);
                client.getConnectionStateListenable().addListener(checkReconnect(self));
            } catch (Exception e) {
                log.error("Failed to create ZK node", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 功能：执行 `currentServerExists` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean currentServerExists() {
        if (nodePath == null) {
            return false;
        }
        try {
            TransportProtos.ServiceInfo self = serviceInfoProvider.getServiceInfo();
            TransportProtos.ServiceInfo registeredServerInfo = null;
            registeredServerInfo = TransportProtos.ServiceInfo.parseFrom(client.getData().forPath(nodePath));
            if (self.equals(registeredServerInfo)) {
                return true;
            }
        } catch (KeeperException.NoNodeException e) {
            log.info("ZK node does not exist: {}", nodePath);
        } catch (Exception e) {
            log.error("Couldn't check if ZK node exists", e);
        }
        return false;
    }

    /**
     * 功能：校验`Reconnect`。
     * 参数：
     * - `self`：`self` 参数。
     * 返回：判断结果。
     */
    private ConnectionStateListener checkReconnect(TransportProtos.ServiceInfo self) {
        return (client, newState) -> {
            log.info("[{}] ZK state changed: {}", self.getServiceId(), newState);
            if (newState == ConnectionState.LOST) {
                zkExecutorService.submit(this::reconnect);
            }
        };
    }

    /**
     * 是否满足`reconnectInProgress`条件。
     */
    private volatile boolean reconnectInProgress = false;

    /**
     * 功能：执行 `reconnect` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private synchronized void reconnect() {
        if (!reconnectInProgress) {
            reconnectInProgress = true;
            try {
                destroyZkClient();
                initZkClient();
                publishCurrentServer();
            } catch (Exception e) {
                log.error("Failed to reconnect to ZK: {}", e.getMessage(), e);
            } finally {
                reconnectInProgress = false;
            }
        }
    }

    /**
     * 功能：初始化或启动客户端。
     * 参数：无。
     * 返回：无。
     */
    private void initZkClient() {
        try {
            client = CuratorFrameworkFactory.newClient(zkUrl, zkSessionTimeout, zkConnectionTimeout, new RetryForever(zkRetryInterval));
            client.start();
            client.blockUntilConnected();
            cache = new PathChildrenCache(client, zkNodesDir, true);
            cache.getListenable().addListener(this);
            cache.start();
            stopped = false;
            log.info("ZK client connected");
        } catch (Exception e) {
            log.error("Failed to connect to ZK: {}", e.getMessage(), e);
            CloseableUtils.closeQuietly(cache);
            CloseableUtils.closeQuietly(client);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：执行 `unpublishCurrentServer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void unpublishCurrentServer() {
        try {
            if (nodePath != null) {
                client.delete().forPath(nodePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete ZK node {}", nodePath, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：停止或关闭客户端。
     * 参数：无。
     * 返回：无。
     */
    private void destroyZkClient() {
        stopped = true;
        try {
            unpublishCurrentServer();
        } catch (Exception e) {
        }
        CloseableUtils.closeQuietly(cache);
        CloseableUtils.closeQuietly(client);
        log.info("ZK client disconnected");
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        destroyZkClient();
        zkExecutorService.shutdownNow();
        log.info("Stopped discovery service");
    }

    /**
     * 功能：执行 `missingProperty` 对应的处理。
     * 参数：
     * - `propertyName`：名称。
     * 返回：文本结果。
     */
    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    /**
     * 功能：执行 `childEvent` 对应的处理。
     * 参数：
     * - `curatorFramework`：`curatorFramework` 参数。
     * - `pathChildrenCacheEvent`：文件或资源路径。
     * 返回：无。
     */
    @Override
    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
        if (stopped) {
            log.debug("Ignoring {}. Service is stopped.", pathChildrenCacheEvent);
            return;
        }
        if (client.getState() != CuratorFrameworkState.STARTED) {
            log.debug("Ignoring {}, ZK client is not started, ZK client state [{}]", pathChildrenCacheEvent, client.getState());
            return;
        }
        ChildData data = pathChildrenCacheEvent.getData();
        if (data == null) {
            log.debug("Ignoring {} due to empty child data", pathChildrenCacheEvent);
            return;
        } else if (data.getData() == null) {
            log.debug("Ignoring {} due to empty child's data", pathChildrenCacheEvent);
            return;
        } else if (nodePath != null && nodePath.equals(data.getPath())) {
            if (pathChildrenCacheEvent.getType() == CHILD_REMOVED) {
                log.info("ZK node for current instance is somehow deleted.");
                publishCurrentServer();
            }
            log.debug("Ignoring event about current server {}", pathChildrenCacheEvent);
            return;
        }
        TransportProtos.ServiceInfo instance;
        try {
            instance = TransportProtos.ServiceInfo.parseFrom(data.getData());
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to decode server instance for node {}", data.getPath(), e);
            throw e;
        }

        String serviceId = instance.getServiceId();
        ProtocolStringList serviceTypesList = instance.getServiceTypesList();

        log.trace("Processing [{}] event for [{}]", pathChildrenCacheEvent.getType(), serviceId);
        switch (pathChildrenCacheEvent.getType()) {
            case CHILD_ADDED:
                ScheduledFuture<?> task = delayedTasks.remove(serviceId);
                if (task != null) {
                    if (task.cancel(false)) {
                        log.debug("[{}] Recalculate partitions ignored. Service was restarted in time [{}].",
                                serviceId, serviceTypesList);
                    } else {
                        log.debug("[{}] Going to recalculate partitions. Service was not restarted in time [{}]!",
                                serviceId, serviceTypesList);
                        recalculatePartitions();
                    }
                } else {
                    log.trace("[{}] Going to recalculate partitions due to adding new node [{}].",
                            serviceId, serviceTypesList);
                    recalculatePartitions();
                }
                break;
            case CHILD_REMOVED:
                zkExecutorService.submit(() -> applicationEventPublisher.publishEvent(new OtherServiceShutdownEvent(this, serviceId, serviceTypesList)));
                ScheduledFuture<?> future = zkExecutorService.schedule(() -> {
                    log.debug("[{}] Going to recalculate partitions due to removed node [{}]",
                            serviceId, serviceTypesList);
                    ScheduledFuture<?> removedTask = delayedTasks.remove(serviceId);
                    if (removedTask != null) {
                        recalculatePartitions();
                    }
                }, recalculateDelay, TimeUnit.MILLISECONDS);
                delayedTasks.put(serviceId, future);
                break;
            default:
                break;
        }
    }

    /**
     * A single entry point to recalculate partitions
     * Synchronized to ensure that other servers info is up to date
     * */
    /**
     * 功能：执行 `recalculatePartitions` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    synchronized void recalculatePartitions() {
        delayedTasks.values().forEach(future -> future.cancel(false));
        delayedTasks.clear();
        partitionService.recalculatePartitions(serviceInfoProvider.getServiceInfo(), getOtherServers());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ZkDiscoveryService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
