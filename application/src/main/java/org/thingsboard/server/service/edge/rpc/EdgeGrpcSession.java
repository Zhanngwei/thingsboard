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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeCommunicationFailureTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.fetch.GeneralEdgeEventFetcher;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.profile.AssetProfileProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationProcessor;
import org.thingsboard.server.service.edge.rpc.processor.resource.ResourceProcessor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

@Slf4j
@Data
/**
 * 中文说明：
 * 1. 类目的：`EdgeGrpcSession` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public final class EdgeGrpcSession implements Closeable {

    private static final ReentrantLock downlinkMsgLock = new ReentrantLock();

    /**
     * 字段说明：
     * 1. 保存 `MAX_DOWNLINK_ATTEMPTS` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int MAX_DOWNLINK_ATTEMPTS = 10; // max number of attemps to send downlink message if edge connected

    /**
     * 字段说明：
     * 1. 保存 `QUEUE_START_TS_ATTR_KEY` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String QUEUE_START_TS_ATTR_KEY = "queueStartTs";
    private static final String QUEUE_START_SEQ_ID_ATTR_KEY = "queueStartSeqId";
    /**
     * 字段说明：
     * 1. 保存 `RATE_LIMIT_REACHED` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final String RATE_LIMIT_REACHED = "Rate limit reached";

    /**
     * 字段说明：
     * 1. 保存 `sessionId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final UUID sessionId;
    private final BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener;
    /**
     * 字段说明：
     * 1. 保存 `sessionCloseListener` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final BiConsumer<Edge, UUID> sessionCloseListener;

    private final EdgeSessionState sessionState = new EdgeSessionState();

    /**
     * 字段说明：
     * 1. 保存 `ctx` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EdgeContextComponent ctx;
    private Edge edge;
    /**
     * 字段说明：
     * 1. 保存 `tenantId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TenantId tenantId;
    private StreamObserver<RequestMsg> inputStream;
    /**
     * 字段说明：
     * 1. 保存 `outputStream` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private StreamObserver<ResponseMsg> outputStream;
    private boolean connected;
    /**
     * 字段说明：
     * 1. 保存 `syncCompleted` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean syncCompleted;

    /**
     * 字段说明：
     * 1. 保存 `newStartTs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Long newStartTs;
    private Long previousStartTs;
    /**
     * 字段说明：
     * 1. 保存 `newStartSeqId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Long newStartSeqId;
    private Long previousStartSeqId;
    /**
     * 字段说明：
     * 1. 保存 `seqIdEnd` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Long seqIdEnd;

    /**
     * 字段说明：
     * 1. 保存 `edgeVersion` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EdgeVersion edgeVersion;

    /**
     * 字段说明：
     * 1. 保存 `maxInboundMessageSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxInboundMessageSize;
    private int clientMaxInboundMessageSize;

    /**
     * 字段说明：
     * 1. 保存 `sendDownlinkExecutorService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ScheduledExecutorService sendDownlinkExecutorService;

    EdgeGrpcSession(EdgeContextComponent ctx, StreamObserver<ResponseMsg> outputStream, BiConsumer<EdgeId, EdgeGrpcSession> sessionOpenListener,
                    BiConsumer<Edge, UUID> sessionCloseListener, ScheduledExecutorService sendDownlinkExecutorService, int maxInboundMessageSize) {
        this.sessionId = UUID.randomUUID();
        this.ctx = ctx;
        this.outputStream = outputStream;
        this.sessionOpenListener = sessionOpenListener;
        this.sessionCloseListener = sessionCloseListener;
        this.sendDownlinkExecutorService = sendDownlinkExecutorService;
        this.maxInboundMessageSize = maxInboundMessageSize;
        initInputStream();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initInputStream` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void initInputStream() {
        this.inputStream = new StreamObserver<>() {
            @Override
            public void onNext(RequestMsg requestMsg) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!connected && requestMsg.getMsgType().equals(RequestMsgType.CONNECT_RPC_MESSAGE)) {
                    ConnectResponseMsg responseMsg = processConnect(requestMsg.getConnectRequestMsg());
                    outputStream.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(responseMsg)
                            .build());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (ConnectResponseCode.ACCEPTED != responseMsg.getResponseCode()) {
                        outputStream.onError(new RuntimeException(responseMsg.getErrorMsg()));
                    } else {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (requestMsg.getConnectRequestMsg().hasMaxInboundMessageSize()) {
                            log.debug("[{}][{}] Client max inbound message size: {}", tenantId, sessionId, requestMsg.getConnectRequestMsg().getMaxInboundMessageSize());
                            clientMaxInboundMessageSize = requestMsg.getConnectRequestMsg().getMaxInboundMessageSize();
                        }
                        connected = true;
                    }
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (connected) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (requestMsg.getMsgType().equals(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)) {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (requestMsg.hasSyncRequestMsg()) {
                            boolean fullSync = false;
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            if (requestMsg.getSyncRequestMsg().hasFullSync()) {
                                fullSync = requestMsg.getSyncRequestMsg().getFullSync();
                            }
                            startSyncProcess(fullSync);
                        } else {
                            syncCompleted = true;
                        }
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (requestMsg.getMsgType().equals(RequestMsgType.UPLINK_RPC_MESSAGE)) {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (requestMsg.hasUplinkMsg()) {
                            onUplinkMsg(requestMsg.getUplinkMsg());
                        }
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (requestMsg.hasDownlinkResponseMsg()) {
                            onDownlinkResponse(requestMsg.getDownlinkResponseMsg());
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("[{}][{}] Stream was terminated due to error:", tenantId, sessionId, t);
                closeSession();
            }

            @Override
            public void onCompleted() {
                log.info("[{}][{}] Stream was closed and completed successfully!", tenantId, sessionId);
                closeSession();
            }

            private void closeSession() {
                connected = false;
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (edge != null) {
                    try {
                        sessionCloseListener.accept(edge, sessionId);
                    // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                    } catch (Exception ignored) {
                    }
                }
                try {
                    outputStream.onCompleted();
                } catch (Exception ignored) {
                }
            }
        };
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
    public void startSyncProcess(boolean fullSync) {
        log.trace("[{}][{}][{}] Staring edge sync process", this.tenantId, edge.getId(), this.sessionId);
        syncCompleted = false;
        interruptGeneralProcessingOnSync();
        doSync(new EdgeSyncCursor(ctx, edge, fullSync));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `doSync` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void doSync(EdgeSyncCursor cursor) {
        if (cursor.hasNext()) {
            EdgeEventFetcher next = cursor.getNext();
            log.info("[{}][{}] starting sync process, cursor current idx = {}, class = {}",
                    this.tenantId, edge.getId(), cursor.getCurrentIdx(), next.getClass().getSimpleName());
            ListenableFuture<Pair<Long, Long>> future = startProcessingEdgeEvents(next);
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Pair<Long, Long> result) {
                    doSync(cursor);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Exception during sync process", tenantId, edge.getId(), t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            DownlinkMsg syncCompleteDownlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .setSyncCompletedMsg(SyncCompletedMsg.newBuilder().build())
                    .build();
            Futures.addCallback(sendDownlinkMsgsPack(Collections.singletonList(syncCompleteDownlinkMsg)), new FutureCallback<>() {
                @Override
                public void onSuccess(Boolean isInterrupted) {
                    syncCompleted = true;
                    ctx.getClusterService().onEdgeEventUpdate(edge.getTenantId(), edge.getId());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Exception during sending sync complete", tenantId, edge.getId(), t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onUplinkMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onUplinkMsg(UplinkMsg uplinkMsg) {
        if (isRateLimitViolated(uplinkMsg)) {
            return;
        }
        ListenableFuture<List<Void>> future = processUplinkMsg(uplinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                sendResponseMessage(uplinkMsg.getUplinkMsgId(), true, null);
            }

            @Override
            public void onFailure(Throwable t) {
                String errorMsg = EdgeUtils.createErrorMsgFromRootCauseAndStackTrace(t);
                sendResponseMessage(uplinkMsg.getUplinkMsgId(), false, errorMsg);
            }
        }, ctx.getGrpcCallbackExecutorService());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isRateLimitViolated` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isRateLimitViolated(UplinkMsg uplinkMsg) {
        if (!ctx.getRateLimitService().checkRateLimit(LimitedApi.EDGE_UPLINK_MESSAGES, tenantId) ||
                !ctx.getRateLimitService().checkRateLimit(LimitedApi.EDGE_UPLINK_MESSAGES_PER_EDGE, tenantId, edge.getId())) {
            String errorMsg = String.format("Failed to process uplink message. %s", RATE_LIMIT_REACHED);
            sendResponseMessage(uplinkMsg.getUplinkMsgId(), false, errorMsg);
            return true;
        }
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendResponseMessage` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void sendResponseMessage(int uplinkMsgId, boolean success, String errorMsg) {
        UplinkResponseMsg.Builder responseBuilder = UplinkResponseMsg.newBuilder()
                .setUplinkMsgId(uplinkMsgId)
                .setSuccess(success);
        if (errorMsg != null) {
            responseBuilder.setErrorMsg(errorMsg);
        }
        sendDownlinkMsg(ResponseMsg.newBuilder()
                .setUplinkResponseMsg(responseBuilder.build())
                .build());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDownlinkResponse` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onDownlinkResponse(DownlinkResponseMsg msg) {
        try {
            if (msg.getSuccess()) {
                sessionState.getPendingMsgsMap().remove(msg.getDownlinkMsgId());
                log.debug("[{}][{}] Msg has been processed successfully! Msg Id: [{}], Msg: {}", this.tenantId, edge.getRoutingKey(), msg.getDownlinkMsgId(), msg);
            } else {
                log.error("[{}][{}] Msg processing failed! Msg Id: [{}], Error msg: {}", this.tenantId, edge.getRoutingKey(), msg.getDownlinkMsgId(), msg.getErrorMsg());
            }
            if (sessionState.getPendingMsgsMap().isEmpty()) {
                log.debug("[{}][{}] Pending msgs map is empty. Stopping current iteration", this.tenantId, edge.getRoutingKey());
                stopCurrentSendDownlinkMsgsTask(false);
            }
        } catch (Exception e) {
            log.error("[{}][{}] Can't process downlink response message [{}]", this.tenantId, this.sessionId, msg, e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendDownlinkMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void sendDownlinkMsg(ResponseMsg downlinkMsg) {
        log.trace("[{}][{}] Sending downlink msg [{}]", this.tenantId, this.sessionId, downlinkMsg);
        if (isConnected()) {
            downlinkMsgLock.lock();
            try {
                outputStream.onNext(downlinkMsg);
            } catch (Exception e) {
                log.error("[{}][{}] Failed to send downlink message [{}]", this.tenantId, this.sessionId, downlinkMsg, e);
                connected = false;
                sessionCloseListener.accept(edge, sessionId);
            } finally {
                downlinkMsgLock.unlock();
            }
            log.trace("[{}][{}] Response msg successfully sent [{}]", this.tenantId, this.sessionId, downlinkMsg);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onConfigurationUpdate` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void onConfigurationUpdate(Edge edge) {
        log.debug("[{}] onConfigurationUpdate [{}]", this.sessionId, edge);
        this.edge = edge;
        this.tenantId = edge.getTenantId();
        EdgeUpdateMsg edgeConfig = EdgeUpdateMsg.newBuilder()
                .setConfiguration(ctx.getEdgeMsgConstructor().constructEdgeConfiguration(edge)).build();
        ResponseMsg edgeConfigMsg = ResponseMsg.newBuilder()
                .setEdgeUpdateMsg(edgeConfig)
                .build();
        sendDownlinkMsg(edgeConfigMsg);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processEdgeEvents` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    ListenableFuture<Boolean> processEdgeEvents() throws Exception {
        SettableFuture<Boolean> result = SettableFuture.create();
        log.trace("[{}][{}] starting processing edge events", this.tenantId, this.sessionId);
        if (isConnected() && isSyncCompleted()) {
            Pair<Long, Long> startTsAndSeqId = getQueueStartTsAndSeqId().get();
            this.previousStartTs = startTsAndSeqId.getFirst();
            this.previousStartSeqId = startTsAndSeqId.getSecond();
            GeneralEdgeEventFetcher fetcher = new GeneralEdgeEventFetcher(
                    this.previousStartTs,
                    this.previousStartSeqId,
                    this.seqIdEnd,
                    false,
                    Integer.toUnsignedLong(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount()),
                    ctx.getEdgeEventService());
            Futures.addCallback(startProcessingEdgeEvents(fetcher), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Pair<Long, Long> newStartTsAndSeqId) {
                    if (newStartTsAndSeqId != null) {
                        ListenableFuture<List<String>> updateFuture = updateQueueStartTsAndSeqId(newStartTsAndSeqId);
                        Futures.addCallback(updateFuture, new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable List<String> list) {
                                log.debug("[{}][{}] queue offset was updated [{}]", tenantId, sessionId, newStartTsAndSeqId);
                                if (fetcher.isSeqIdNewCycleStarted()) {
                                    seqIdEnd = fetcher.getSeqIdEnd();
                                    boolean newEventsAvailable = isNewEdgeEventsAvailable();
                                    result.set(newEventsAvailable);
                                } else {
                                    seqIdEnd = null;
                                    boolean newEventsAvailable = isSeqIdStartedNewCycle();
                                    if (!newEventsAvailable) {
                                        newEventsAvailable = isNewEdgeEventsAvailable();
                                    }
                                    result.set(newEventsAvailable);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.error("[{}][{}] Failed to update queue offset [{}]", tenantId, sessionId, newStartTsAndSeqId, t);
                                result.setException(t);
                            }
                        }, ctx.getGrpcCallbackExecutorService());
                    } else {
                        log.trace("[{}][{}] newStartTsAndSeqId is null. Skipping iteration without db update", tenantId, sessionId);
                        result.set(null);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to process events", tenantId, sessionId, t);
                    result.setException(t);
                }
            }, ctx.getGrpcCallbackExecutorService());
        } else {
            log.trace("[{}][{}] edge is not connected or sync is not completed. Skipping iteration", tenantId, sessionId);
            result.set(null);
        }
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `startProcessingEdgeEvents` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Pair<Long, Long>> startProcessingEdgeEvents(EdgeEventFetcher fetcher) {
        SettableFuture<Pair<Long, Long>> result = SettableFuture.create();
        PageLink pageLink = fetcher.getPageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount());
        processEdgeEvents(fetcher, pageLink, result);
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processEdgeEvents` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processEdgeEvents(EdgeEventFetcher fetcher, PageLink pageLink, SettableFuture<Pair<Long, Long>> result) {
        try {
            PageData<EdgeEvent> pageData = fetcher.fetchEdgeEvents(edge.getTenantId(), edge, pageLink);
            if (isConnected() && !pageData.getData().isEmpty()) {
                log.trace("[{}][{}][{}] event(s) are going to be processed.", this.tenantId, this.sessionId, pageData.getData().size());
                List<DownlinkMsg> downlinkMsgsPack = convertToDownlinkMsgsPack(pageData.getData());
                Futures.addCallback(sendDownlinkMsgsPack(downlinkMsgsPack), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Boolean isInterrupted) {
                        if (Boolean.TRUE.equals(isInterrupted)) {
                            log.debug("[{}][{}][{}] Send downlink messages task was interrupted", tenantId, edge.getId(), sessionId);
                            result.set(null);
                        } else {
                            if (isConnected() && pageData.hasNext()) {
                                processEdgeEvents(fetcher, pageLink.nextPageLink(), result);
                            } else {
                                EdgeEvent latestEdgeEvent = pageData.getData().get(pageData.getData().size() - 1);
                                UUID idOffset = latestEdgeEvent.getUuidId();
                                if (idOffset != null) {
                                    Long newStartTs = Uuids.unixTimestamp(idOffset);
                                    long newStartSeqId = latestEdgeEvent.getSeqId();
                                    result.set(Pair.of(newStartTs, newStartSeqId));
                                } else {
                                    result.set(null);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to send downlink msgs pack", sessionId, t);
                        result.setException(t);
                    }
                }, ctx.getGrpcCallbackExecutorService());
            } else {
                log.trace("[{}] no event(s) found. Stop processing edge events", this.sessionId);
                result.set(null);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to fetch edge events", this.sessionId, e);
            result.setException(e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendDownlinkMsgsPack` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Boolean> sendDownlinkMsgsPack(List<DownlinkMsg> downlinkMsgsPack) {
        interruptPreviousSendDownlinkMsgsTask();

        sessionState.setSendDownlinkMsgsFuture(SettableFuture.create());
        sessionState.getPendingMsgsMap().clear();

        downlinkMsgsPack.forEach(msg -> sessionState.getPendingMsgsMap().put(msg.getDownlinkMsgId(), msg));
        scheduleDownlinkMsgsPackSend(1);

        return sessionState.getSendDownlinkMsgsFuture();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `scheduleDownlinkMsgsPackSend` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void scheduleDownlinkMsgsPackSend(int attempt) {
        Runnable sendDownlinkMsgsTask = () -> {
            try {
                if (isConnected() && sessionState.getPendingMsgsMap().values().size() > 0) {
                    List<DownlinkMsg> copy = new ArrayList<>(sessionState.getPendingMsgsMap().values());
                    if (attempt > 1) {
                        String error = "Failed to deliver the batch";
                        String failureMsg = String.format("{%s}: {%s}", error, copy);
                        if (attempt == 2) {
                            // Send a failure notification only on the second attempt.
                            // This ensures that failure alerts are sent just once to avoid redundant notifications.
                            ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId)
                                    .edgeId(edge.getId()).customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(error).build());
                        }
                        log.warn("[{}][{}] {}, attempt: {}", this.tenantId, this.sessionId, failureMsg, attempt);
                    }
                    log.trace("[{}][{}][{}] downlink msg(s) are going to be send.", this.tenantId, this.sessionId, copy.size());
                    for (DownlinkMsg downlinkMsg : copy) {
                        if (this.clientMaxInboundMessageSize != 0 && downlinkMsg.getSerializedSize() > this.clientMaxInboundMessageSize) {
                            String error = String.format("Client max inbound message size [{%s}] is exceeded. Please increase value of CLOUD_RPC_MAX_INBOUND_MESSAGE_SIZE " +
                                    "env variable on the edge and restart it.", this.clientMaxInboundMessageSize);
                            String message = String.format("Downlink msg size [{%s}] exceeds client max inbound message size [{%s}]. " +
                                    "Please increase value of CLOUD_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the edge and restart it.", downlinkMsg.getSerializedSize(), this.clientMaxInboundMessageSize);
                            log.error("[{}][{}][{}] {} Message {}", this.tenantId, edge.getId(), this.sessionId, message, downlinkMsg);
                            ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId)
                                    .edgeId(edge.getId()).customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(message).error(error).build());
                            sessionState.getPendingMsgsMap().remove(downlinkMsg.getDownlinkMsgId());
                        } else {
                            sendDownlinkMsg(ResponseMsg.newBuilder()
                                    .setDownlinkMsg(downlinkMsg)
                                    .build());
                        }
                    }
                    if (attempt < MAX_DOWNLINK_ATTEMPTS) {
                        scheduleDownlinkMsgsPackSend(attempt + 1);
                    } else {
                        String failureMsg = String.format("Failed to deliver messages: %s", copy);
                        log.warn("[{}][{}] Failed to deliver the batch after {} attempts. Next messages are going to be discarded {}",
                                this.tenantId, this.sessionId, MAX_DOWNLINK_ATTEMPTS, copy);
                        ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                                .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg)
                                .error("Failed to deliver messages after " + MAX_DOWNLINK_ATTEMPTS + " attempts").build());
                        stopCurrentSendDownlinkMsgsTask(false);
                    }
                } else {
                    stopCurrentSendDownlinkMsgsTask(false);
                }
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to send downlink msgs. Error msg {}", this.tenantId, this.sessionId, e.getMessage(), e);
                stopCurrentSendDownlinkMsgsTask(true);
            }
        };

        if (attempt == 1) {
            sendDownlinkExecutorService.submit(sendDownlinkMsgsTask);
        } else {
            sessionState.setScheduledSendDownlinkTask(
                    sendDownlinkExecutorService.schedule(
                            sendDownlinkMsgsTask,
                            ctx.getEdgeEventStorageSettings().getSleepIntervalBetweenBatches(),
                            TimeUnit.MILLISECONDS)
            );
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertToDownlinkMsgsPack` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<DownlinkMsg> convertToDownlinkMsgsPack(List<EdgeEvent> edgeEvents) {
        List<DownlinkMsg> result = new ArrayList<>();
        for (EdgeEvent edgeEvent : edgeEvents) {
            log.trace("[{}][{}] converting edge event to downlink msg [{}]", this.tenantId, this.sessionId, edgeEvent);
            DownlinkMsg downlinkMsg = null;
            try {
                switch (edgeEvent.getAction()) {
                    case UPDATED:
                    case ADDED:
                    case DELETED:
                    case ASSIGNED_TO_EDGE:
                    case UNASSIGNED_FROM_EDGE:
                    case ALARM_ACK:
                    case ALARM_CLEAR:
                    case ALARM_DELETE:
                    case CREDENTIALS_UPDATED:
                    case RELATION_ADD_OR_UPDATE:
                    case RELATION_DELETED:
                    case CREDENTIALS_REQUEST:
                    case RPC_CALL:
                    case ASSIGNED_TO_CUSTOMER:
                    case UNASSIGNED_FROM_CUSTOMER:
                    case ADDED_COMMENT:
                    case UPDATED_COMMENT:
                    case DELETED_COMMENT:
                        downlinkMsg = convertEntityEventToDownlink(edgeEvent);
                        log.trace("[{}][{}] entity message processed [{}]", this.tenantId, this.sessionId, downlinkMsg);
                        break;
                    case ATTRIBUTES_UPDATED:
                    case POST_ATTRIBUTES:
                    case ATTRIBUTES_DELETED:
                    case TIMESERIES_UPDATED:
                        downlinkMsg = ctx.getTelemetryProcessor().convertTelemetryEventToDownlink(edgeEvent);
                        break;
                    default:
                        log.warn("[{}][{}] Unsupported action type [{}]", this.tenantId, this.sessionId, edgeEvent.getAction());
                }
            } catch (Exception e) {
                log.error("[{}][{}] Exception during converting edge event to downlink msg", this.tenantId, this.sessionId, e);
            }
            if (downlinkMsg != null) {
                result.add(downlinkMsg);
            }
        }
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getQueueStartTsAndSeqId` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Pair<Long, Long>> getQueueStartTsAndSeqId() {
        ListenableFuture<List<AttributeKvEntry>> future =
                ctx.getAttributesService().find(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, Arrays.asList(QUEUE_START_TS_ATTR_KEY, QUEUE_START_SEQ_ID_ATTR_KEY));
        return Futures.transform(future, attributeKvEntries -> {
            long startTs = 0L;
            long startSeqId = 0L;
            for (AttributeKvEntry attributeKvEntry : attributeKvEntries) {
                if (QUEUE_START_TS_ATTR_KEY.equals(attributeKvEntry.getKey())) {
                    startTs = attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
                }
                if (QUEUE_START_SEQ_ID_ATTR_KEY.equals(attributeKvEntry.getKey())) {
                    startSeqId = attributeKvEntry.getLongValue().isPresent() ? attributeKvEntry.getLongValue().get() : 0L;
                }
            }
            if (startSeqId == 0L) {
                startSeqId = findStartSeqIdFromOldestEventIfAny();
            }
            return Pair.of(startTs, startSeqId);
        }, ctx.getGrpcCallbackExecutorService());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isSeqIdStartedNewCycle` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isSeqIdStartedNewCycle() {
        try {
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, this.newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), 0L, this.previousStartSeqId == 0 ? null : this.previousStartSeqId - 1, pageLink);
            return !edgeEvents.getData().isEmpty();
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isSeqIdStartedNewCycle", this.tenantId, edge.getId(), sessionId, e);
        }
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isNewEdgeEventsAvailable` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isNewEdgeEventsAvailable() {
        try {
            TimePageLink pageLink = new TimePageLink(ctx.getEdgeEventStorageSettings().getMaxReadRecordsCount(), 0, null, null, this.newStartTs, System.currentTimeMillis());
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), this.newStartSeqId, null, pageLink);
            return !edgeEvents.getData().isEmpty();
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute isNewEdgeEventsAvailable", this.tenantId, edge.getId(), sessionId, e);
        }
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `findStartSeqIdFromOldestEventIfAny` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private long findStartSeqIdFromOldestEventIfAny() {
        long startSeqId = 0L;
        try {
            TimePageLink pageLink = new TimePageLink(1, 0, null, new SortOrder("createdTime"), null, null);
            PageData<EdgeEvent> edgeEvents = ctx.getEdgeEventService().findEdgeEvents(edge.getTenantId(), edge.getId(), null, null, pageLink);
            if (!edgeEvents.getData().isEmpty()) {
                startSeqId = edgeEvents.getData().get(0).getSeqId() - 1;
            }
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to execute findStartSeqIdFromOldestEventIfAny", this.tenantId, edge.getId(), sessionId, e);
        }
        return startSeqId;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateQueueStartTsAndSeqId` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<List<String>> updateQueueStartTsAndSeqId(Pair<Long, Long> pair) {
        this.newStartTs = pair.getFirst();
        this.newStartSeqId = pair.getSecond();
        log.trace("[{}] updateQueueStartTsAndSeqId [{}][{}][{}]", this.sessionId, edge.getId(), this.newStartTs, this.newStartSeqId);
        List<AttributeKvEntry> attributes = Arrays.asList(
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_TS_ATTR_KEY, this.newStartTs), System.currentTimeMillis()),
                new BaseAttributeKvEntry(new LongDataEntry(QUEUE_START_SEQ_ID_ATTR_KEY, this.newStartSeqId), System.currentTimeMillis()));
        return ctx.getAttributesService().save(edge.getTenantId(), edge.getId(), DataConstants.SERVER_SCOPE, attributes);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertEntityEventToDownlink` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private DownlinkMsg convertEntityEventToDownlink(EdgeEvent edgeEvent) {
        log.trace("[{}] Executing convertEntityEventToDownlink, edgeEvent [{}], action [{}]", this.tenantId, edgeEvent, edgeEvent.getAction());
        switch (edgeEvent.getType()) {
            case EDGE:
                return ctx.getEdgeProcessor().convertEdgeEventToDownlink(edgeEvent);
            case DEVICE:
                return ctx.getDeviceProcessor().convertDeviceEventToDownlink(edgeEvent, this.edge.getId(), this.edgeVersion);
            case DEVICE_PROFILE:
                return ctx.getDeviceProfileProcessor().convertDeviceProfileEventToDownlink(edgeEvent, this.edge.getId(), this.edgeVersion);
            case ASSET_PROFILE:
                return ctx.getAssetProfileProcessor().convertAssetProfileEventToDownlink(edgeEvent, this.edge.getId(), this.edgeVersion);
            case ASSET:
                return ctx.getAssetProcessor().convertAssetEventToDownlink(edgeEvent, this.edge.getId(), this.edgeVersion);
            case ENTITY_VIEW:
                return ctx.getEntityViewProcessor().convertEntityViewEventToDownlink(edgeEvent, this.edgeVersion);
            case DASHBOARD:
                return ctx.getDashboardProcessor().convertDashboardEventToDownlink(edgeEvent, this.edgeVersion);
            case CUSTOMER:
                return ctx.getCustomerProcessor().convertCustomerEventToDownlink(edgeEvent, this.edgeVersion);
            case RULE_CHAIN:
                return ctx.getRuleChainProcessor().convertRuleChainEventToDownlink(edgeEvent, this.edgeVersion);
            case RULE_CHAIN_METADATA:
                return ctx.getRuleChainProcessor().convertRuleChainMetadataEventToDownlink(edgeEvent, this.edgeVersion);
            case ALARM:
                return ctx.getAlarmProcessor().convertAlarmEventToDownlink(edgeEvent, this.edgeVersion);
            case ALARM_COMMENT:
                return ctx.getAlarmProcessor().convertAlarmCommentEventToDownlink(edgeEvent, this.edgeVersion);
            case USER:
                return ctx.getUserProcessor().convertUserEventToDownlink(edgeEvent, this.edgeVersion);
            case RELATION:
                return ctx.getRelationProcessor().convertRelationEventToDownlink(edgeEvent, this.edgeVersion);
            case WIDGETS_BUNDLE:
                return ctx.getWidgetBundleProcessor().convertWidgetsBundleEventToDownlink(edgeEvent, this.edgeVersion);
            case WIDGET_TYPE:
                return ctx.getWidgetTypeProcessor().convertWidgetTypeEventToDownlink(edgeEvent, this.edgeVersion);
            case ADMIN_SETTINGS:
                return ctx.getAdminSettingsProcessor().convertAdminSettingsEventToDownlink(edgeEvent, this.edgeVersion);
            case OTA_PACKAGE:
                return ctx.getOtaPackageEdgeProcessor().convertOtaPackageEventToDownlink(edgeEvent, this.edgeVersion);
            case TB_RESOURCE:
                return ctx.getResourceEdgeProcessor().convertResourceEventToDownlink(edgeEvent, this.edgeVersion);
            case QUEUE:
                return ctx.getQueueEdgeProcessor().convertQueueEventToDownlink(edgeEvent, this.edgeVersion);
            case TENANT:
                return ctx.getTenantEdgeProcessor().convertTenantEventToDownlink(edgeEvent, this.edgeVersion);
            case TENANT_PROFILE:
                return ctx.getTenantProfileEdgeProcessor().convertTenantProfileEventToDownlink(edgeEvent, this.edgeVersion);
            default:
                log.warn("[{}] Unsupported edge event type [{}]", this.tenantId, edgeEvent);
                return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processUplinkMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<List<Void>> processUplinkMsg(UplinkMsg uplinkMsg) {
        List<ListenableFuture<Void>> result = new ArrayList<>();
        try {
            if (uplinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
                for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : uplinkMsg.getDeviceProfileUpdateMsgList()) {
                    result.add(((DeviceProfileProcessor) ctx.getDeviceProfileEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processDeviceProfileMsgFromEdge(edge.getTenantId(), edge, deviceProfileUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceUpdateMsgCount() > 0) {
                for (DeviceUpdateMsg deviceUpdateMsg : uplinkMsg.getDeviceUpdateMsgList()) {
                    result.add(((DeviceProcessor) ctx.getDeviceEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processDeviceMsgFromEdge(edge.getTenantId(), edge, deviceUpdateMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
                for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : uplinkMsg.getDeviceCredentialsUpdateMsgList()) {
                    result.add(((DeviceProcessor) ctx.getDeviceEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processDeviceCredentialsMsgFromEdge(edge.getTenantId(), edge.getId(), deviceCredentialsUpdateMsg));
                }
            }
            if (uplinkMsg.getAssetProfileUpdateMsgCount() > 0) {
                for (AssetProfileUpdateMsg assetProfileUpdateMsg : uplinkMsg.getAssetProfileUpdateMsgList()) {
                    result.add(((AssetProfileProcessor) ctx.getAssetProfileEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processAssetProfileMsgFromEdge(edge.getTenantId(), edge, assetProfileUpdateMsg));
                }
            }
            if (uplinkMsg.getAssetUpdateMsgCount() > 0) {
                for (AssetUpdateMsg assetUpdateMsg : uplinkMsg.getAssetUpdateMsgList()) {
                    result.add(((AssetProcessor) ctx.getAssetEdgeProcessorFactory().getProcessorByEdgeVersion(edgeVersion))
                            .processAssetMsgFromEdge(edge.getTenantId(), edge, assetUpdateMsg));
                }
            }
            if (uplinkMsg.getEntityViewUpdateMsgCount() > 0) {
                for (EntityViewUpdateMsg entityViewUpdateMsg : uplinkMsg.getEntityViewUpdateMsgList()) {
                    result.add(((EntityViewProcessor) ctx.getEntityViewProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processEntityViewMsgFromEdge(edge.getTenantId(), edge, entityViewUpdateMsg));
                }
            }
            if (uplinkMsg.getEntityDataCount() > 0) {
                for (EntityDataProto entityData : uplinkMsg.getEntityDataList()) {
                    result.addAll(ctx.getTelemetryProcessor().processTelemetryMsg(edge.getTenantId(), entityData));
                }
            }
            if (uplinkMsg.getAlarmUpdateMsgCount() > 0) {
                for (AlarmUpdateMsg alarmUpdateMsg : uplinkMsg.getAlarmUpdateMsgList()) {
                    result.add(((AlarmProcessor) ctx.getAlarmEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processAlarmMsgFromEdge(edge.getTenantId(), edge.getId(), alarmUpdateMsg));
                }
            }
            if (uplinkMsg.getAlarmCommentUpdateMsgCount() > 0) {
                for (AlarmCommentUpdateMsg alarmCommentUpdateMsg : uplinkMsg.getAlarmCommentUpdateMsgList()) {
                    result.add(((AlarmProcessor) ctx.getAlarmEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processAlarmCommentMsgFromEdge(edge.getTenantId(), edge.getId(), alarmCommentUpdateMsg));
                }
            }
            if (uplinkMsg.getRelationUpdateMsgCount() > 0) {
                for (RelationUpdateMsg relationUpdateMsg : uplinkMsg.getRelationUpdateMsgList()) {
                    result.add(((RelationProcessor) ctx.getRelationEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processRelationMsgFromEdge(edge.getTenantId(), edge, relationUpdateMsg));
                }
            }
            if (uplinkMsg.getDashboardUpdateMsgCount() > 0) {
                for (DashboardUpdateMsg dashboardUpdateMsg : uplinkMsg.getDashboardUpdateMsgList()) {
                    result.add(((DashboardProcessor) ctx.getDashboardEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processDashboardMsgFromEdge(edge.getTenantId(), edge, dashboardUpdateMsg));
                }
            }
            if (uplinkMsg.getResourceUpdateMsgCount() > 0) {
                for (ResourceUpdateMsg resourceUpdateMsg : uplinkMsg.getResourceUpdateMsgList()) {
                    result.add(((ResourceProcessor) ctx.getResourceEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processResourceMsgFromEdge(edge.getTenantId(), edge, resourceUpdateMsg));
                }
            }
            if (uplinkMsg.getRuleChainMetadataRequestMsgCount() > 0) {
                for (RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg : uplinkMsg.getRuleChainMetadataRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRuleChainMetadataRequestMsg(edge.getTenantId(), edge, ruleChainMetadataRequestMsg));
                }
            }
            if (uplinkMsg.getAttributesRequestMsgCount() > 0) {
                for (AttributesRequestMsg attributesRequestMsg : uplinkMsg.getAttributesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processAttributesRequestMsg(edge.getTenantId(), edge, attributesRequestMsg));
                }
            }
            if (uplinkMsg.getRelationRequestMsgCount() > 0) {
                for (RelationRequestMsg relationRequestMsg : uplinkMsg.getRelationRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processRelationRequestMsg(edge.getTenantId(), edge, relationRequestMsg));
                }
            }
            if (uplinkMsg.getUserCredentialsRequestMsgCount() > 0) {
                for (UserCredentialsRequestMsg userCredentialsRequestMsg : uplinkMsg.getUserCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processUserCredentialsRequestMsg(edge.getTenantId(), edge, userCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
                for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : uplinkMsg.getDeviceCredentialsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processDeviceCredentialsRequestMsg(edge.getTenantId(), edge, deviceCredentialsRequestMsg));
                }
            }
            if (uplinkMsg.getDeviceRpcCallMsgCount() > 0) {
                for (DeviceRpcCallMsg deviceRpcCallMsg : uplinkMsg.getDeviceRpcCallMsgList()) {
                    result.add(((DeviceProcessor) ctx.getDeviceEdgeProcessorFactory().getProcessorByEdgeVersion(this.edgeVersion))
                            .processDeviceRpcCallFromEdge(edge.getTenantId(), edge, deviceRpcCallMsg));
                }
            }
            if (uplinkMsg.getWidgetBundleTypesRequestMsgCount() > 0) {
                for (WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg : uplinkMsg.getWidgetBundleTypesRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processWidgetBundleTypesRequestMsg(edge.getTenantId(), edge, widgetBundleTypesRequestMsg));
                }
            }
            if (uplinkMsg.getEntityViewsRequestMsgCount() > 0) {
                for (EntityViewsRequestMsg entityViewRequestMsg : uplinkMsg.getEntityViewsRequestMsgList()) {
                    result.add(ctx.getEdgeRequestsService().processEntityViewsRequestMsg(edge.getTenantId(), edge, entityViewRequestMsg));
                }
            }
        } catch (Exception e) {
            String failureMsg = String.format("Can't process uplink msg [%s] from edge", uplinkMsg);
            log.error("[{}][{}] Can't process uplink msg [{}]", this.tenantId, this.sessionId, uplinkMsg, e);
            ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                    .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(e.getMessage()).build());
            return Futures.immediateFailedFuture(e);
        }
        return Futures.allAsList(result);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processConnect` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ConnectResponseMsg processConnect(ConnectRequestMsg request) {
        log.trace("[{}] processConnect [{}]", this.sessionId, request);
        Optional<Edge> optional = ctx.getEdgeService().findEdgeByRoutingKey(TenantId.SYS_TENANT_ID, request.getEdgeRoutingKey());
        if (optional.isPresent()) {
            edge = optional.get();
            this.tenantId = edge.getTenantId();
            try {
                if (edge.getSecret().equals(request.getEdgeSecret())) {
                    sessionOpenListener.accept(edge.getId(), this);
                    this.edgeVersion = request.getEdgeVersion();
                    processSaveEdgeVersionAsAttribute(request.getEdgeVersion().name());
                    return ConnectResponseMsg.newBuilder()
                            .setResponseCode(ConnectResponseCode.ACCEPTED)
                            .setErrorMsg("")
                            .setConfiguration(ctx.getEdgeMsgConstructor().constructEdgeConfiguration(edge))
                            .setMaxInboundMessageSize(maxInboundMessageSize)
                            .build();
                }
                String error = "Failed to validate the edge!";
                String failureMsg = String.format("{%s} Provided request secret: %s", error, request.getEdgeSecret());
                ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                        .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(error).build());
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                        .setErrorMsg(failureMsg)
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            } catch (Exception e) {
                String failureMsg = "Failed to process edge connection!";
                ctx.getNotificationRuleProcessor().process(EdgeCommunicationFailureTrigger.builder().tenantId(tenantId).edgeId(edge.getId())
                        .customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(failureMsg).error(e.getMessage()).build());
                log.error(failureMsg, e);
                return ConnectResponseMsg.newBuilder()
                        .setResponseCode(ConnectResponseCode.SERVER_UNAVAILABLE)
                        .setErrorMsg(failureMsg)
                        .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
            }
        }
        return ConnectResponseMsg.newBuilder()
                .setResponseCode(ConnectResponseCode.BAD_CREDENTIALS)
                .setErrorMsg("Failed to find the edge! Routing key: " + request.getEdgeRoutingKey())
                .setConfiguration(EdgeConfiguration.getDefaultInstance()).build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processSaveEdgeVersionAsAttribute` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processSaveEdgeVersionAsAttribute(String edgeVersion) {
        AttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(new StringDataEntry(DataConstants.EDGE_VERSION_ATTR_KEY, edgeVersion), System.currentTimeMillis());
        ctx.getAttributesService().save(this.tenantId, this.edge.getId(), DataConstants.SERVER_SCOPE, attributeKvEntry);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `close` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void close() {
        log.debug("[{}][{}] Closing session", this.tenantId, sessionId);
        connected = false;
        try {
            outputStream.onCompleted();
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to close output stream: {}", this.tenantId, sessionId, e.getMessage());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `interruptPreviousSendDownlinkMsgsTask` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void interruptPreviousSendDownlinkMsgsTask() {
        if (sessionState.getSendDownlinkMsgsFuture() != null && !sessionState.getSendDownlinkMsgsFuture().isDone()
                || sessionState.getScheduledSendDownlinkTask() != null && !sessionState.getScheduledSendDownlinkTask().isCancelled()) {
            log.debug("[{}][{}][{}] Previous send downlink future was not properly completed, stopping it now!", this.tenantId, edge.getId(), this.sessionId);
            stopCurrentSendDownlinkMsgsTask(true);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `interruptGeneralProcessingOnSync` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void interruptGeneralProcessingOnSync() {
        log.debug("[{}][{}][{}] Sync process started. General processing interrupted!", this.tenantId, edge.getId(), this.sessionId);
        stopCurrentSendDownlinkMsgsTask(true);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `stopCurrentSendDownlinkMsgsTask` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stopCurrentSendDownlinkMsgsTask(Boolean isInterrupted) {
        if (sessionState.getSendDownlinkMsgsFuture() != null && !sessionState.getSendDownlinkMsgsFuture().isDone()) {
            sessionState.getSendDownlinkMsgsFuture().set(isInterrupted);
        }
        if (sessionState.getScheduledSendDownlinkTask() != null) {
            sessionState.getScheduledSendDownlinkTask().cancel(true);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeGrpcSession` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
