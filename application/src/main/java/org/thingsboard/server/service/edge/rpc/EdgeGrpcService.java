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
package org.thingsboard.server.service.edge.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.EdgeSessionMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
/**
 * 中文说明：
 * 1. 类目的：`EdgeGrpcService` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase implements EdgeRpcService {

    private final ConcurrentMap<EdgeId, EdgeGrpcSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<EdgeId, Lock> sessionNewEventsLocks = new ConcurrentHashMap<>();
    private final Map<EdgeId, Boolean> sessionNewEvents = new HashMap<>();
    private final ConcurrentMap<EdgeId, ScheduledFuture<?>> sessionEdgeEventChecks = new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, Consumer<FromEdgeSyncResponse>> localSyncEdgeRequests = new ConcurrentHashMap<>();

    @Value("${edges.rpc.port}")
    /**
     * 字段说明：
     * 1. 保存 `rpcPort` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int rpcPort;
    @Value("${edges.rpc.ssl.enabled}")
    /**
     * 字段说明：
     * 1. 保存 `sslEnabled` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean sslEnabled;
    @Value("${edges.rpc.ssl.cert}")
    /**
     * 字段说明：
     * 1. 保存 `certFileResource` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String certFileResource;
    @Value("${edges.rpc.ssl.private_key}")
    /**
     * 字段说明：
     * 1. 保存 `privateKeyResource` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String privateKeyResource;
    @Value("${edges.state.persistToTelemetry:false}")
    /**
     * 字段说明：
     * 1. 保存 `persistToTelemetry` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean persistToTelemetry;
    @Value("${edges.rpc.client_max_keep_alive_time_sec:1}")
    /**
     * 字段说明：
     * 1. 保存 `clientMaxKeepAliveTimeSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int clientMaxKeepAliveTimeSec;
    @Value("${edges.rpc.max_inbound_message_size:4194304}")
    /**
     * 字段说明：
     * 1. 保存 `maxInboundMessageSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxInboundMessageSize;
    @Value("${edges.rpc.keep_alive_time_sec:10}")
    /**
     * 字段说明：
     * 1. 保存 `keepAliveTimeSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int keepAliveTimeSec;
    @Value("${edges.rpc.keep_alive_timeout_sec:5}")
    /**
     * 字段说明：
     * 1. 保存 `keepAliveTimeoutSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int keepAliveTimeoutSec;
    @Value("${edges.scheduler_pool_size}")
    /**
     * 字段说明：
     * 1. 保存 `schedulerPoolSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int schedulerPoolSize;

    @Value("${edges.send_scheduler_pool_size}")
    /**
     * 字段说明：
     * 1. 保存 `sendSchedulerPoolSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int sendSchedulerPoolSize;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `ctx` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EdgeContextComponent ctx;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tsSubService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `clusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbClusterService clusterService;

    /**
     * 字段说明：
     * 1. 保存 `server` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Server server;

    /**
     * 字段说明：
     * 1. 保存 `edgeEventProcessingExecutorService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ScheduledExecutorService edgeEventProcessingExecutorService;

    /**
     * 字段说明：
     * 1. 保存 `sendDownlinkExecutorService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ScheduledExecutorService sendDownlinkExecutorService;

    /**
     * 字段说明：
     * 1. 保存 `executorService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ScheduledExecutorService executorService;

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init() {
        log.info("Initializing Edge RPC service!");
        NettyServerBuilder builder = NettyServerBuilder.forPort(rpcPort)
                .permitKeepAliveTime(clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeoutSec, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .maxInboundMessageSize(maxInboundMessageSize)
                .addService(this);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (sslEnabled) {
            try {
                InputStream certFileIs = ResourceUtils.getInputStream(this, certFileResource);
                InputStream privateKeyFileIs = ResourceUtils.getInputStream(this, privateKeyResource);
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                builder.useTransportSecurity(certFileIs, privateKeyFileIs);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
                throw new RuntimeException("Unable to set up SSL context!", e);
            }
        }
        server = builder.build();
        log.info("Going to start Edge RPC server using port: {}", rpcPort);
        try {
            server.start();
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (IOException e) {
            log.error("Failed to start Edge RPC server!", e);
            throw new RuntimeException("Failed to start Edge RPC server!");
        }
        this.edgeEventProcessingExecutorService = Executors.newScheduledThreadPool(schedulerPoolSize, ThingsBoardThreadFactory.forName("edge-event-check-scheduler"));
        this.sendDownlinkExecutorService = Executors.newScheduledThreadPool(sendSchedulerPoolSize, ThingsBoardThreadFactory.forName("edge-send-scheduler"));
        this.executorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("edge-service"));
        log.info("Edge RPC service initialized!");
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `destroy` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void destroy() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (server != null) {
            server.shutdownNow();
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        for (Map.Entry<EdgeId, ScheduledFuture<?>> entry : sessionEdgeEventChecks.entrySet()) {
            EdgeId edgeId = entry.getKey();
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            ScheduledFuture<?> sessionEdgeEventCheck = entry.getValue();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (sessionEdgeEventCheck != null && !sessionEdgeEventCheck.isCancelled() && !sessionEdgeEventCheck.isDone()) {
                sessionEdgeEventCheck.cancel(true);
                sessionEdgeEventChecks.remove(edgeId);
            }
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (edgeEventProcessingExecutorService != null) {
            edgeEventProcessingExecutorService.shutdownNow();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (sendDownlinkExecutorService != null) {
            sendDownlinkExecutorService.shutdownNow();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleMsgs` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> outputStream) {
        return new EdgeGrpcSession(ctx, outputStream, this::onEdgeConnect, this::onEdgeDisconnect, sendDownlinkExecutorService, this.maxInboundMessageSize).getInputStream();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onToEdgeSessionMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onToEdgeSessionMsg(TenantId tenantId, EdgeSessionMsg msg) {
        executorService.execute(() -> {
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (msg.getMsgType()) {
                case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
                    EdgeEventUpdateMsg edgeEventUpdateMsg = (EdgeEventUpdateMsg) msg;
                    log.trace("[{}] onToEdgeSessionMsg [{}]", tenantId, msg);
                    onEdgeEvent(tenantId, edgeEventUpdateMsg.getEdgeId());
                    break;
                case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG:
                    ToEdgeSyncRequest toEdgeSyncRequest = (ToEdgeSyncRequest) msg;
                    log.trace("[{}] toEdgeSyncRequest [{}]", tenantId, msg);
                    startSyncProcess(tenantId, toEdgeSyncRequest.getEdgeId(), toEdgeSyncRequest.getId());
                    break;
                case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG:
                    FromEdgeSyncResponse fromEdgeSyncResponse = (FromEdgeSyncResponse) msg;
                    log.trace("[{}] fromEdgeSyncResponse [{}]", tenantId, msg);
                    processSyncResponse(fromEdgeSyncResponse);
                    break;
            }
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateEdge` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void updateEdge(TenantId tenantId, Edge edge) {
        executorService.execute(() -> {
            EdgeGrpcSession session = sessions.get(edge.getId());
            if (session != null && session.isConnected()) {
                log.debug("[{}] Updating configuration for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
                session.onConfigurationUpdate(edge);
            } else {
                log.debug("[{}] Session doesn't exist for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
            }
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteEdge` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        executorService.execute(() -> {
            EdgeGrpcSession session = sessions.get(edgeId);
            if (session != null && session.isConnected()) {
                log.info("[{}] Closing and removing session for edge [{}]", tenantId, edgeId);
                session.close();
                sessions.remove(edgeId);
                final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
                newEventLock.lock();
                try {
                    sessionNewEvents.remove(edgeId);
                } finally {
                    newEventLock.unlock();
                }
                cancelScheduleEdgeEventsCheck(edgeId);
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onEdgeEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onEdgeEvent(TenantId tenantId, EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            log.trace("[{}] onEdgeEvent [{}]", tenantId, edgeId.getId());
            final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
            newEventLock.lock();
            try {
                if (Boolean.FALSE.equals(sessionNewEvents.get(edgeId))) {
                    log.trace("[{}] set session new events flag to true [{}]", tenantId, edgeId.getId());
                    sessionNewEvents.put(edgeId, true);
                }
            } finally {
                newEventLock.unlock();
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onEdgeConnect` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onEdgeConnect(EdgeId edgeId, EdgeGrpcSession edgeGrpcSession) {
        Edge edge = edgeGrpcSession.getEdge();
        TenantId tenantId = edge.getTenantId();
        log.info("[{}][{}] edge [{}] connected successfully.", tenantId, edgeGrpcSession.getSessionId(), edgeId);
        sessions.put(edgeId, edgeGrpcSession);
        final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
        newEventLock.lock();
        try {
            sessionNewEvents.put(edgeId, true);
        } finally {
            newEventLock.unlock();
        }
        save(tenantId, edgeId, DefaultDeviceStateService.ACTIVITY_STATE, true);
        long lastConnectTs = System.currentTimeMillis();
        save(tenantId, edgeId, DefaultDeviceStateService.LAST_CONNECT_TIME, lastConnectTs);
        pushRuleEngineMessage(tenantId, edge, lastConnectTs, TbMsgType.CONNECT_EVENT);
        cancelScheduleEdgeEventsCheck(edgeId);
        scheduleEdgeEventsCheck(edgeGrpcSession);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startSyncProcess` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void startSyncProcess(TenantId tenantId, EdgeId edgeId, UUID requestId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null) {
            boolean success = false;
            if (session.isConnected()) {
                session.startSyncProcess(true);
                success = true;
            }
            clusterService.pushEdgeSyncResponseToCore(new FromEdgeSyncResponse(requestId, tenantId, edgeId, success));
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processSyncRequest` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void processSyncRequest(ToEdgeSyncRequest request, Consumer<FromEdgeSyncResponse> responseConsumer) {
        log.trace("[{}][{}] Processing sync edge request [{}]", request.getTenantId(), request.getId(), request.getEdgeId());
        UUID requestId = request.getId();
        localSyncEdgeRequests.put(requestId, responseConsumer);
        clusterService.pushEdgeSyncRequestToCore(request);
        scheduleSyncRequestTimeout(request, requestId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `scheduleSyncRequestTimeout` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void scheduleSyncRequestTimeout(ToEdgeSyncRequest request, UUID requestId) {
        log.trace("[{}] scheduling sync edge request", requestId);
        executorService.schedule(() -> {
            log.trace("[{}] checking if sync edge request is not processed...", requestId);
            Consumer<FromEdgeSyncResponse> consumer = localSyncEdgeRequests.remove(requestId);
            if (consumer != null) {
                log.trace("[{}] timeout for processing sync edge request.", requestId);
                consumer.accept(new FromEdgeSyncResponse(requestId, request.getTenantId(), request.getEdgeId(), false));
            }
        }, 20, TimeUnit.SECONDS);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processSyncResponse` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processSyncResponse(FromEdgeSyncResponse response) {
        log.trace("[{}] Received response from sync service: [{}]", response.getId(), response);
        UUID requestId = response.getId();
        Consumer<FromEdgeSyncResponse> consumer = localSyncEdgeRequests.remove(requestId);
        if (consumer != null) {
            consumer.accept(response);
        } else {
            log.trace("[{}] Unknown or stale sync response received [{}]", requestId, response);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `scheduleEdgeEventsCheck` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void scheduleEdgeEventsCheck(EdgeGrpcSession session) {
        EdgeId edgeId = session.getEdge().getId();
        UUID tenantId = session.getEdge().getTenantId().getId();
        if (sessions.containsKey(edgeId)) {
            ScheduledFuture<?> edgeEventCheckTask = edgeEventProcessingExecutorService.schedule(() -> {
                try {
                    final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
                    newEventLock.lock();
                    try {
                        if (Boolean.TRUE.equals(sessionNewEvents.get(edgeId))) {
                            log.trace("[{}][{}] Set session new events flag to false", tenantId, edgeId.getId());
                            sessionNewEvents.put(edgeId, false);
                            Futures.addCallback(session.processEdgeEvents(), new FutureCallback<>() {
                                @Override
                                public void onSuccess(Boolean newEventsAdded) {
                                    if (Boolean.TRUE.equals(newEventsAdded)) {
                                        sessionNewEvents.put(edgeId, true);
                                    }
                                    scheduleEdgeEventsCheck(session);
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, session.getEdge().getId().getId(), t);
                                    scheduleEdgeEventsCheck(session);
                                }
                            }, ctx.getGrpcCallbackExecutorService());
                        } else {
                            scheduleEdgeEventsCheck(session);
                        }
                    } finally {
                        newEventLock.unlock();
                    }
                } catch (Exception e) {
                    log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, session.getEdge().getId().getId(), e);
                }
            }, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval(), TimeUnit.MILLISECONDS);
            sessionEdgeEventChecks.put(edgeId, edgeEventCheckTask);
            log.trace("[{}] Check edge event scheduled for edge [{}]", tenantId, edgeId.getId());
        } else {
            log.debug("[{}] Session was removed and edge event check schedule must not be started [{}]",
                    tenantId, edgeId.getId());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `cancelScheduleEdgeEventsCheck` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void cancelScheduleEdgeEventsCheck(EdgeId edgeId) {
        log.trace("[{}] cancelling edge event check for edge", edgeId);
        if (sessionEdgeEventChecks.containsKey(edgeId)) {
            ScheduledFuture<?> sessionEdgeEventCheck = sessionEdgeEventChecks.get(edgeId);
            if (sessionEdgeEventCheck != null && !sessionEdgeEventCheck.isCancelled() && !sessionEdgeEventCheck.isDone()) {
                sessionEdgeEventCheck.cancel(true);
                sessionEdgeEventChecks.remove(edgeId);
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onEdgeDisconnect` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onEdgeDisconnect(Edge edge, UUID sessionId) {
        EdgeId edgeId = edge.getId();
        log.info("[{}][{}] edge disconnected!", edgeId, sessionId);
        EdgeGrpcSession toRemove = sessions.get(edgeId);
        if (toRemove.getSessionId().equals(sessionId)) {
            toRemove = sessions.remove(edgeId);
            final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
            newEventLock.lock();
            try {
                sessionNewEvents.remove(edgeId);
            } finally {
                newEventLock.unlock();
            }
            TenantId tenantId = toRemove.getEdge().getTenantId();
            save(tenantId, edgeId, DefaultDeviceStateService.ACTIVITY_STATE, false);
            long lastDisconnectTs = System.currentTimeMillis();
            save(tenantId, edgeId, DefaultDeviceStateService.LAST_DISCONNECT_TIME, lastDisconnectTs);
            pushRuleEngineMessage(toRemove.getEdge().getTenantId(), edge, lastDisconnectTs, TbMsgType.DISCONNECT_EVENT);
            cancelScheduleEdgeEventsCheck(edgeId);
        } else {
            log.debug("[{}] edge session [{}] is not available anymore, nothing to remove. most probably this session is already outdated!", edgeId, sessionId);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `save` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void save(TenantId tenantId, EdgeId edgeId, String key, long value) {
        log.debug("[{}][{}] Updating long edge telemetry [{}] [{}]", tenantId, edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    tenantId, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(key, value))),
                    new AttributeSaveCallback(tenantId, edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(tenantId, edgeId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(tenantId, edgeId, key, value));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `save` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void save(TenantId tenantId, EdgeId edgeId, String key, boolean value) {
        log.debug("[{}][{}] Updating boolean edge telemetry [{}] [{}]", tenantId, edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    tenantId, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))),
                    new AttributeSaveCallback(tenantId, edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(tenantId, edgeId, DataConstants.SERVER_SCOPE, key, value, new AttributeSaveCallback(tenantId, edgeId, key, value));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`AttributeSaveCallback` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
     * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
     */
    private static class AttributeSaveCallback implements FutureCallback<Void> {
        /**
         * 字段说明：
         * 1. 保存 `tenantId` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final TenantId tenantId;
        private final EdgeId edgeId;
        /**
         * 字段说明：
         * 1. 保存 `key` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final String key;
        private final Object value;

        AttributeSaveCallback(TenantId tenantId, EdgeId edgeId, String key, Object value) {
            this.tenantId = tenantId;
            this.edgeId = edgeId;
            this.key = key;
            this.value = value;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onSuccess` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onSuccess(@Nullable Void result) {
            log.trace("[{}][{}] Successfully updated attribute [{}] with value [{}]", tenantId, edgeId, key, value);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onFailure` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onFailure(Throwable t) {
            log.warn("[{}][{}] Failed to update attribute [{}] with value [{}]", tenantId, edgeId, key, value, t);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushRuleEngineMessage` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushRuleEngineMessage(TenantId tenantId, Edge edge, long ts, TbMsgType msgType) {
        try {
            EdgeId edgeId = edge.getId();
            ObjectNode edgeState = JacksonUtil.newObjectNode();
            boolean isConnected = TbMsgType.CONNECT_EVENT.equals(msgType);
            if (isConnected) {
                edgeState.put(DefaultDeviceStateService.ACTIVITY_STATE, true);
                edgeState.put(DefaultDeviceStateService.LAST_CONNECT_TIME, ts);
            } else {
                edgeState.put(DefaultDeviceStateService.ACTIVITY_STATE, false);
                edgeState.put(DefaultDeviceStateService.LAST_DISCONNECT_TIME, ts);
            }
            ctx.getNotificationRuleProcessor().process(EdgeConnectionTrigger.builder()
                    .tenantId(tenantId)
                    .customerId(edge.getCustomerId())
                    .edgeId(edgeId)
                    .edgeName(edge.getName())
                    .connected(isConnected).build());
            String data = JacksonUtil.toString(edgeState);
            TbMsgMetaData md = new TbMsgMetaData();
            if (!persistToTelemetry) {
                md.putValue(DataConstants.SCOPE, DataConstants.SERVER_SCOPE);
                md.putValue("edgeName", edge.getName());
                md.putValue("edgeType", edge.getType());
            }
            TbMsg tbMsg = TbMsg.newMsg(msgType, edgeId, md, TbMsgDataType.JSON, data);
            clusterService.pushMsgToRuleEngine(tenantId, edgeId, tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push {}", tenantId, edge.getId(), msgType, e);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeGrpcService` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
