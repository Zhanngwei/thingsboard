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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.errors.LargeObjectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AddMsg;
import org.thingsboard.server.gen.transport.TransportProtos.BranchInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.CommitRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.CommitResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeleteMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntitiesContentRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntitiesContentResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntityContentRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntityContentResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntityVersionProto;
import org.thingsboard.server.gen.transport.TransportProtos.ListBranchesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListBranchesResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListEntitiesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListEntitiesResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListVersionsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListVersionsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PrepareMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.VersionControlResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.VersionedEntityInfoProto;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbVersionControlQueueFactory;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbVersionControlComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.sync.vc.DefaultGitRepositoryService.fromRelativePath;

@Slf4j
@TbVersionControlComponent
@Service
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultClusterVersionControlService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class DefaultClusterVersionControlService extends TbApplicationEventListener<PartitionChangeEvent> implements ClusterVersionControlService {

    /**
     * 字段说明：
     * 1. 保存 `partitionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final PartitionService partitionService;
    private final TbQueueProducerProvider producerProvider;
    /**
     * 字段说明：
     * 1. 保存 `queueFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbVersionControlQueueFactory queueFactory;
    private final DataDecodingEncodingService encodingService;
    /**
     * 字段说明：
     * 1. 保存 `vcService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final GitRepositoryService vcService;
    private final TopicService topicService;

    private final ConcurrentMap<TenantId, Lock> tenantRepoLocks = new ConcurrentHashMap<>();
    private final Map<TenantId, PendingCommit> pendingCommitMap = new HashMap<>();

    /**
     * 字段说明：
     * 1. 保存 `consumerExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private volatile ExecutorService consumerExecutor;
    private volatile TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> consumer;
    /**
     * 字段说明：
     * 1. 保存 `producer` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private volatile TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> producer;
    private volatile boolean stopped = false;

    @Value("${queue.vc.poll-interval:25}")
    /**
     * 字段说明：
     * 1. 保存 `pollDuration` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long pollDuration;
    @Value("${queue.vc.pack-processing-timeout:180000}")
    /**
     * 字段说明：
     * 1. 保存 `packProcessingTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long packProcessingTimeout;
    @Value("${vc.git.io_pool_size:3}")
    /**
     * 字段说明：
     * 1. 保存 `ioPoolSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int ioPoolSize;
    @Value("${queue.vc.msg-chunk-size:250000}")
    /**
     * 字段说明：
     * 1. 保存 `msgChunkSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int msgChunkSize;

    //We need to manually manage the threads since tasks for particular tenant need to be processed sequentially.
    private final List<ListeningExecutorService> ioThreads = new ArrayList<>();


    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init() {
        consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("vc-consumer"));
        var threadFactory = ThingsBoardThreadFactory.forName("vc-io-thread");
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < ioPoolSize; i++) {
            ioThreads.add(MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(threadFactory)));
        }
        producer = producerProvider.getTbCoreNotificationsMsgProducer();
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        consumer = queueFactory.createToVersionControlMsgConsumer();
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `stop` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stop() {
        stopped = true;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (consumer != null) {
            consumer.unsubscribe();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
        ioThreads.forEach(ExecutorService::shutdownNow);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onTbApplicationEvent` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (TenantId tenantId : vcService.getActiveRepositoryTenants()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!partitionService.isMyPartition(ServiceType.TB_VC_EXECUTOR, tenantId, tenantId)) {
                var lock = getRepoLock(tenantId);
                lock.lock();
                try {
                    pendingCommitMap.remove(tenantId);
                    vcService.clearRepository(tenantId);
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                } catch (Exception e) {
                    log.warn("[{}] Failed to cleanup the tenant repository", tenantId, e);
                } finally {
                    lock.unlock();
                }
            }
        }
        consumer.subscribe(event.getPartitions());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `filterTbApplicationEvent` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return ServiceType.TB_VC_EXECUTOR.equals(event.getServiceType());
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    /**
     * 方法说明：
     * 1. 职责：执行 `onApplicationEvent` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onApplicationEvent(ApplicationReadyEvent event) {
        consumerExecutor.execute(() -> consumerLoop(consumer));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `consumerLoop` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> consumer) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        while (!stopped && !consumer.isStopped()) {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            List<ListenableFuture<?>> futures = new ArrayList<>();
            try {
                List<TbProtoQueueMsg<ToVersionControlServiceMsg>> msgs = consumer.poll(pollDuration);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (msgs.isEmpty()) {
                    continue;
                }
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (TbProtoQueueMsg<ToVersionControlServiceMsg> msgWrapper : msgs) {
                    ToVersionControlServiceMsg msg = msgWrapper.getValue();
                    var ctx = new VersionControlRequestCtx(msg, msg.hasClearRepositoryRequest() ? null : getEntitiesVersionControlSettings(msg));
                    long startTs = System.currentTimeMillis();
                    log.trace("[{}][{}] RECEIVED task: {}", ctx.getTenantId(), ctx.getRequestId(), msg);
                    int threadIdx = Math.abs(ctx.getTenantId().hashCode() % ioPoolSize);
                    // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                    ListenableFuture<Void> future = ioThreads.get(threadIdx).submit(() -> processMessage(ctx, msg));
                    logTaskExecution(ctx, future, startTs);
                    futures.add(future);
                }
                try {
                    Futures.allAsList(futures).get(packProcessingTimeout, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    log.info("Timeout for processing the version control tasks.", e);
                }
                consumer.commit();
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("Failed to obtain version control requests from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new version control messages", e2);
                    }
                }
            }
        }
        log.info("TB Version Control request consumer stopped.");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processMessage` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Void processMessage(VersionControlRequestCtx ctx, ToVersionControlServiceMsg msg) {
        var lock = getRepoLock(ctx.getTenantId());
        lock.lock();
        try {
            if (msg.hasClearRepositoryRequest()) {
                handleClearRepositoryCommand(ctx);
            } else {
                if (msg.hasTestRepositoryRequest()) {
                    handleTestRepositoryCommand(ctx);
                } else if (msg.hasInitRepositoryRequest()) {
                    handleInitRepositoryCommand(ctx);
                } else {
                    var currentSettings = vcService.getRepositorySettings(ctx.getTenantId());
                    var newSettings = ctx.getSettings();
                    if (!newSettings.equals(currentSettings)) {
                        vcService.initRepository(ctx.getTenantId(), ctx.getSettings());
                    }
                    if (msg.hasCommitRequest()) {
                        handleCommitRequest(ctx, msg.getCommitRequest());
                    } else if (msg.hasListBranchesRequest()) {
                        vcService.fetch(ctx.getTenantId());
                        handleListBranches(ctx, msg.getListBranchesRequest());
                    } else if (msg.hasListEntitiesRequest()) {
                        handleListEntities(ctx, msg.getListEntitiesRequest());
                    } else if (msg.hasListVersionRequest()) {
                        vcService.fetch(ctx.getTenantId());
                        handleListVersions(ctx, msg.getListVersionRequest());
                    } else if (msg.hasEntityContentRequest()) {
                        handleEntityContentRequest(ctx, msg.getEntityContentRequest());
                    } else if (msg.hasEntitiesContentRequest()) {
                        handleEntitiesContentRequest(ctx, msg.getEntitiesContentRequest());
                    } else if (msg.hasVersionsDiffRequest()) {
                        handleVersionsDiffRequest(ctx, msg.getVersionsDiffRequest());
                    }
                }
            }
        } catch (Exception e) {
            reply(ctx, Optional.of(handleError(e)));
        } finally {
            lock.unlock();
        }
        return null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleEntitiesContentRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleEntitiesContentRequest(VersionControlRequestCtx ctx, EntitiesContentRequestMsg request) throws Exception {
        var entityType = EntityType.valueOf(request.getEntityType());
        String path = getRelativePath(entityType, null);
        var ids = vcService.listEntitiesAtVersion(ctx.getTenantId(), request.getVersionId(), path)
                .stream().skip(request.getOffset()).limit(request.getLimit()).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            for (int i = 0; i < ids.size(); i++){
                VersionedEntityInfo info = ids.get(i);
                var data = vcService.getFileContentAtCommit(ctx.getTenantId(),
                        getRelativePath(info.getExternalId().getEntityType(), info.getExternalId().getId().toString()), request.getVersionId());
                Iterable<String> dataChunks = StringUtils.split(data, msgChunkSize);
                int chunksCount = Iterables.size(dataChunks);
                AtomicInteger chunkIndex = new AtomicInteger();
                int itemIdx = i;
                dataChunks.forEach(chunk -> {
                    EntitiesContentResponseMsg.Builder response = EntitiesContentResponseMsg.newBuilder()
                            .setItemsCount(ids.size())
                            .setItemIdx(itemIdx)
                            .setItem(EntityContentResponseMsg.newBuilder()
                                    .setData(chunk)
                                    .setChunksCount(chunksCount)
                                    .setChunkIndex(chunkIndex.getAndIncrement())
                                    .build());
                    reply(ctx, Optional.empty(), builder -> builder.setEntitiesContentResponse(response));
                });
            }
        } else {
            reply(ctx, Optional.empty(), builder -> builder.setEntitiesContentResponse(
                    EntitiesContentResponseMsg.newBuilder()
                            .setItemsCount(0)));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleEntityContentRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleEntityContentRequest(VersionControlRequestCtx ctx, EntityContentRequestMsg request) throws IOException {
        String path = getRelativePath(EntityType.valueOf(request.getEntityType()), new UUID(request.getEntityIdMSB(), request.getEntityIdLSB()).toString());
        log.debug("Executing handleEntityContentRequest [{}][{}]", ctx.getTenantId(), path);
        String data = vcService.getFileContentAtCommit(ctx.getTenantId(), path, request.getVersionId());

        Iterable<String> dataChunks = StringUtils.split(data, msgChunkSize);
        String chunkedMsgId = UUID.randomUUID().toString();
        int chunksCount = Iterables.size(dataChunks);

        AtomicInteger chunkIndex = new AtomicInteger();
        dataChunks.forEach(chunk -> {
            log.trace("[{}] sending chunk {} for 'getEntity'", chunkedMsgId, chunkIndex.get());
            reply(ctx, Optional.empty(), builder -> builder.setEntityContentResponse(EntityContentResponseMsg.newBuilder()
                    .setData(chunk).setChunksCount(chunksCount)
                    .setChunkIndex(chunkIndex.getAndIncrement())));
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleListVersions` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleListVersions(VersionControlRequestCtx ctx, ListVersionsRequestMsg request) throws Exception {
        String path;
        if (StringUtils.isNotEmpty(request.getEntityType())) {
            var entityType = EntityType.valueOf(request.getEntityType());
            if (request.getEntityIdLSB() != 0 || request.getEntityIdMSB() != 0) {
                path = getRelativePath(entityType, new UUID(request.getEntityIdMSB(), request.getEntityIdLSB()).toString());
            } else {
                path = getRelativePath(entityType, null);
            }
        } else {
            path = null;
        }
        SortOrder sortOrder = null;
        if (StringUtils.isNotEmpty(request.getSortProperty())) {
            var direction = SortOrder.Direction.DESC;
            if (StringUtils.isNotEmpty(request.getSortDirection())) {
                direction = SortOrder.Direction.valueOf(request.getSortDirection());
            }
            sortOrder = new SortOrder(request.getSortProperty(), direction);
        }
        var data = vcService.listVersions(ctx.getTenantId(), request.getBranchName(), path,
                new PageLink(request.getPageSize(), request.getPage(), request.getTextSearch(), sortOrder));
        reply(ctx, Optional.empty(), builder ->
                builder.setListVersionsResponse(ListVersionsResponseMsg.newBuilder()
                        .setTotalPages(data.getTotalPages())
                        .setTotalElements(data.getTotalElements())
                        .setHasNext(data.hasNext())
                        .addAllVersions(data.getData().stream().map(
                                v -> EntityVersionProto.newBuilder().setTs(v.getTimestamp()).setId(v.getId()).setName(v.getName()).setAuthor(v.getAuthor()).build()
                        ).collect(Collectors.toList())))
        );
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleListEntities` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleListEntities(VersionControlRequestCtx ctx, ListEntitiesRequestMsg request) throws Exception {
        EntityType entityType = StringUtils.isNotEmpty(request.getEntityType()) ? EntityType.valueOf(request.getEntityType()) : null;
        var path = entityType != null ? getRelativePath(entityType, null) : null;
        var data = vcService.listEntitiesAtVersion(ctx.getTenantId(), request.getVersionId(), path);
        reply(ctx, Optional.empty(), builder ->
                builder.setListEntitiesResponse(ListEntitiesResponseMsg.newBuilder()
                        .addAllEntities(data.stream().map(VersionedEntityInfo::getExternalId).map(
                                id -> VersionedEntityInfoProto.newBuilder()
                                        .setEntityType(id.getEntityType().name())
                                        .setEntityIdMSB(id.getId().getMostSignificantBits())
                                        .setEntityIdLSB(id.getId().getLeastSignificantBits()).build()
                        ).collect(Collectors.toList()))));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleListBranches` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleListBranches(VersionControlRequestCtx ctx, ListBranchesRequestMsg request) {
        var branches = vcService.listBranches(ctx.getTenantId()).stream()
                .map(branchInfo -> BranchInfoProto.newBuilder()
                        .setName(branchInfo.getName())
                        .setIsDefault(branchInfo.isDefault()).build()).collect(Collectors.toList());
        reply(ctx, Optional.empty(), builder -> builder.setListBranchesResponse(ListBranchesResponseMsg.newBuilder().addAllBranches(branches)));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleVersionsDiffRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleVersionsDiffRequest(VersionControlRequestCtx ctx, TransportProtos.VersionsDiffRequestMsg request) throws IOException {
        List<TransportProtos.EntityVersionsDiff> diffList = vcService.getVersionsDiffList(ctx.getTenantId(), request.getPath(), request.getVersionId1(), request.getVersionId2()).stream()
                .map(diff -> {
                    EntityId entityId = fromRelativePath(diff.getFilePath());
                    return TransportProtos.EntityVersionsDiff.newBuilder()
                            .setEntityType(entityId.getEntityType().name())
                            .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                            .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                            .setEntityDataAtVersion1(diff.getFileContentAtCommit1())
                            .setEntityDataAtVersion2(diff.getFileContentAtCommit2())
                            .setRawDiff(diff.getDiffStringValue())
                            .build();
                })
                .collect(Collectors.toList());

        reply(ctx, builder -> builder.setVersionsDiffResponse(TransportProtos.VersionsDiffResponseMsg.newBuilder()
                .addAllDiff(diffList)));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleCommitRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleCommitRequest(VersionControlRequestCtx ctx, CommitRequestMsg request) throws Exception {
        log.debug("Executing handleCommitRequest [{}][{}]", ctx.getTenantId(), ctx.getRequestId());
        var tenantId = ctx.getTenantId();
        UUID txId = UUID.fromString(request.getTxId());
        if (request.hasPrepareMsg()) {
            vcService.fetch(ctx.getTenantId());
            prepareCommit(ctx, txId, request.getPrepareMsg());
        } else if (request.hasAbortMsg()) {
            PendingCommit current = pendingCommitMap.get(tenantId);
            if (current != null && current.getTxId().equals(txId)) {
                doAbortCurrentCommit(tenantId, current);
            }
        } else {
            PendingCommit current = pendingCommitMap.get(tenantId);
            if (current != null && current.getTxId().equals(txId)) {
                try {
                    if (request.hasAddMsg()) {
                        addToCommit(ctx, current, request.getAddMsg());
                    } else if (request.hasDeleteMsg()) {
                        deleteFromCommit(ctx, current, request.getDeleteMsg());
                    } else if (request.hasPushMsg()) {
                        var result = vcService.push(current);
                        pendingCommitMap.remove(ctx.getTenantId());
                        reply(ctx, result);
                    }
                } catch (Exception e) {
                    doAbortCurrentCommit(tenantId, current, e);
                    throw e;
                }
            } else {
                log.debug("[{}] Ignore request due to stale commit: {}", txId, request);
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `prepareCommit` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void prepareCommit(VersionControlRequestCtx ctx, UUID txId, PrepareMsg prepareMsg) {
        var tenantId = ctx.getTenantId();
        var pendingCommit = new PendingCommit(tenantId, ctx.getNodeId(), txId, prepareMsg.getBranchName(),
                prepareMsg.getCommitMsg(), prepareMsg.getAuthorName(), prepareMsg.getAuthorEmail());
        PendingCommit old = pendingCommitMap.get(tenantId);
        if (old != null) {
            doAbortCurrentCommit(tenantId, old);
        }
        pendingCommitMap.put(tenantId, pendingCommit);
        vcService.prepareCommit(pendingCommit);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `deleteFromCommit` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void deleteFromCommit(VersionControlRequestCtx ctx, PendingCommit commit, DeleteMsg deleteMsg) throws IOException {
        vcService.deleteFolderContent(commit, deleteMsg.getRelativePath());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addToCommit` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addToCommit(VersionControlRequestCtx ctx, PendingCommit commit, AddMsg addMsg) throws IOException {
        log.debug("Executing addToCommit [{}][{}]", ctx.getTenantId(), ctx.getRequestId());
        log.trace("[{}] received chunk {} for 'addToCommit'", addMsg.getChunkedMsgId(), addMsg.getChunkIndex());
        Map<String, String[]> chunkedMsgs = commit.getChunkedMsgs();
        String[] msgChunks = chunkedMsgs.computeIfAbsent(addMsg.getChunkedMsgId(), id -> new String[addMsg.getChunksCount()]);
        msgChunks[addMsg.getChunkIndex()] = addMsg.getEntityDataJsonChunk();
        if (CollectionsUtil.countNonNull(msgChunks) == msgChunks.length) {
            log.trace("[{}] collected all chunks for 'addToCommit'", addMsg.getChunkedMsgId());
            String entityDataJson = String.join("", msgChunks);
            chunkedMsgs.remove(addMsg.getChunkedMsgId());
            vcService.add(commit, addMsg.getRelativePath(), entityDataJson);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `doAbortCurrentCommit` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void doAbortCurrentCommit(TenantId tenantId, PendingCommit current) {
        doAbortCurrentCommit(tenantId, current, null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `doAbortCurrentCommit` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void doAbortCurrentCommit(TenantId tenantId, PendingCommit current, Exception e) {
        vcService.abort(current);
        pendingCommitMap.remove(tenantId);
        //TODO: push notification to core using old.getNodeId() to cancel old commit processing on the caller side.
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleClearRepositoryCommand` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleClearRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.clearRepository(ctx.getTenantId());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleInitRepositoryCommand` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleInitRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.initRepository(ctx.getTenantId(), ctx.getSettings());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `handleTestRepositoryCommand` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleTestRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.testRepository(ctx.getTenantId(), ctx.getSettings());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleError` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Exception handleError(Exception e) {
        if (e instanceof LargeObjectException) {
            return new RuntimeException("Version is too big");
        }
        return e;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reply` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void reply(VersionControlRequestCtx ctx, VersionCreationResult result) {
        var responseBuilder = CommitResponseMsg.newBuilder().setAdded(result.getAdded())
                .setModified(result.getModified())
                .setRemoved(result.getRemoved());

        if (result.getVersion() != null) {
            responseBuilder.setTs(result.getVersion().getTimestamp())
                    .setCommitId(result.getVersion().getId())
                    .setName(result.getVersion().getName())
                    .setAuthor(result.getVersion().getAuthor());
        }

        reply(ctx, Optional.empty(), builder -> builder.setCommitResponse(responseBuilder));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reply` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void reply(VersionControlRequestCtx ctx, Optional<Exception> e) {
        reply(ctx, e, null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reply` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void reply(VersionControlRequestCtx ctx, Function<VersionControlResponseMsg.Builder, VersionControlResponseMsg.Builder> enrichFunction) {
        reply(ctx, Optional.empty(), enrichFunction);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reply` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void reply(VersionControlRequestCtx ctx, Optional<Exception> e, Function<VersionControlResponseMsg.Builder, VersionControlResponseMsg.Builder> enrichFunction) {
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, ctx.getNodeId());
        VersionControlResponseMsg.Builder builder = VersionControlResponseMsg.newBuilder()
                .setRequestIdMSB(ctx.getRequestId().getMostSignificantBits())
                .setRequestIdLSB(ctx.getRequestId().getLeastSignificantBits());
        if (e.isPresent()) {
            log.debug("[{}][{}] Failed to process task", ctx.getTenantId(), ctx.getRequestId(), e.get());
            var message = e.get().getMessage();
            builder.setError(message != null ? message : e.get().getClass().getSimpleName());
        } else {
            if (enrichFunction != null) {
                builder = enrichFunction.apply(builder);
            } else {
                builder.setGenericResponse(TransportProtos.GenericRepositoryResponseMsg.newBuilder().build());
            }
            log.debug("[{}][{}] Processed task", ctx.getTenantId(), ctx.getRequestId());
        }

        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setVcResponseMsg(builder).build();
        log.trace("[{}][{}] PUSHING reply: {} to: {}", ctx.getTenantId(), ctx.getRequestId(), msg, tpi);
        producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntitiesVersionControlSettings` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RepositorySettings getEntitiesVersionControlSettings(ToVersionControlServiceMsg msg) {
        Optional<RepositorySettings> settingsOpt = encodingService.decode(msg.getVcSettings().toByteArray());
        if (settingsOpt.isPresent()) {
            return settingsOpt.get();
        } else {
            log.warn("Failed to parse VC settings: {}", msg.getVcSettings());
            throw new RuntimeException("Failed to parse vc settings!");
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getRelativePath` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getRelativePath(EntityType entityType, String entityId) {
        String path = entityType.name().toLowerCase();
        if (entityId != null) {
            path += "/" + entityId + ".json";
        }
        return path;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getRepoLock` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Lock getRepoLock(TenantId tenantId) {
        return tenantRepoLocks.computeIfAbsent(tenantId, t -> new ReentrantLock(true));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logTaskExecution` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logTaskExecution(VersionControlRequestCtx ctx, ListenableFuture<Void> future, long startTs) {
        if (log.isTraceEnabled()) {
            Futures.addCallback(future, new FutureCallback<Object>() {

                @Override
                public void onSuccess(@Nullable Object result) {
                    log.trace("[{}][{}] Task processing took: {}ms", ctx.getTenantId(), ctx.getRequestId(), (System.currentTimeMillis() - startTs));
                }

                @Override
                public void onFailure(Throwable t) {
                    log.trace("[{}][{}] Task failed: ", ctx.getTenantId(), ctx.getRequestId(), t);
                }
            }, MoreExecutors.directExecutor());
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultClusterVersionControlService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
