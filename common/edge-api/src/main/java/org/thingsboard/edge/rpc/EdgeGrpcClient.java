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
package org.thingsboard.edge.rpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.edge.exception.EdgeConnectionException;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.edge.v1.ConnectRequestMsg;
import org.thingsboard.server.gen.edge.v1.ConnectResponseCode;
import org.thingsboard.server.gen.edge.v1.ConnectResponseMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RequestMsg;
import org.thingsboard.server.gen.edge.v1.RequestMsgType;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.edge.v1.SyncRequestMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`EdgeGrpcClient` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class EdgeGrpcClient implements EdgeRpcClient {

    @Value("${cloud.rpc.host}")
    /**
     * 字段说明：
     * 1. 保存 `rpcHost` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String rpcHost;
    @Value("${cloud.rpc.port}")
    /**
     * 字段说明：
     * 1. 保存 `rpcPort` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int rpcPort;
    @Value("${cloud.rpc.timeout}")
    /**
     * 字段说明：
     * 1. 保存 `timeoutSecs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int timeoutSecs;
    @Value("${cloud.rpc.keep_alive_time_sec:10}")
    /**
     * 字段说明：
     * 1. 保存 `keepAliveTimeSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int keepAliveTimeSec;
    @Value("${cloud.rpc.keep_alive_timeout_sec:5}")
    /**
     * 字段说明：
     * 1. 保存 `keepAliveTimeoutSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int keepAliveTimeoutSec;
    @Value("${cloud.rpc.ssl.enabled}")
    /**
     * 字段说明：
     * 1. 保存 `sslEnabled` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean sslEnabled;
    @Value("${cloud.rpc.ssl.cert:}")
    /**
     * 字段说明：
     * 1. 保存 `certResource` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String certResource;
    @Value("${cloud.rpc.max_inbound_message_size:4194304}")
    /**
     * 字段说明：
     * 1. 保存 `maxInboundMessageSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxInboundMessageSize;
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `serverMaxInboundMessageSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int serverMaxInboundMessageSize;

    /**
     * 字段说明：
     * 1. 保存 `channel` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ManagedChannel channel;

    /**
     * 字段说明：
     * 1. 保存 `inputStream` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private StreamObserver<RequestMsg> inputStream;

    private static final ReentrantLock uplinkMsgLock = new ReentrantLock();

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `connect` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void connect(String edgeKey,
                        String edgeSecret,
                        Consumer<UplinkResponseMsg> onUplinkResponse,
                        Consumer<EdgeConfiguration> onEdgeUpdate,
                        Consumer<DownlinkMsg> onDownlink,
                        Consumer<Exception> onError) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(rpcHost, rpcPort)
                .maxInboundMessageSize(maxInboundMessageSize)
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeoutSec, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (sslEnabled) {
            try {
                SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (StringUtils.isNotEmpty(certResource)) {
                    sslContextBuilder.trustManager(ResourceUtils.getInputStream(this, certResource));
                }
                builder.sslContext(sslContextBuilder.build());
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (SSLException e) {
                log.error("Failed to initialize channel!", e);
                throw new RuntimeException(e);
            }
        } else {
            builder.usePlaintext();
        }
        channel = builder.build();
        EdgeRpcServiceGrpc.EdgeRpcServiceStub stub = EdgeRpcServiceGrpc.newStub(channel);
        log.info("[{}] Sending a connect request to the TB!", edgeKey);
        this.inputStream = stub.withCompression("gzip").handleMsgs(initOutputStream(edgeKey, onUplinkResponse, onEdgeUpdate, onDownlink, onError));
        this.inputStream.onNext(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.CONNECT_RPC_MESSAGE)
                .setConnectRequestMsg(ConnectRequestMsg.newBuilder()
                        .setEdgeRoutingKey(edgeKey)
                        .setEdgeSecret(edgeSecret)
                        .setEdgeVersion(EdgeVersion.V_3_6_4)
                        .setMaxInboundMessageSize(maxInboundMessageSize)
                        .build())
                .build());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initOutputStream` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private StreamObserver<ResponseMsg> initOutputStream(String edgeKey,
                                                         Consumer<UplinkResponseMsg> onUplinkResponse,
                                                         Consumer<EdgeConfiguration> onEdgeUpdate,
                                                         Consumer<DownlinkMsg> onDownlink,
                                                         Consumer<Exception> onError) {
        return new StreamObserver<>() {
            @Override
            public void onNext(ResponseMsg responseMsg) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (responseMsg.hasConnectResponseMsg()) {
                    ConnectResponseMsg connectResponseMsg = responseMsg.getConnectResponseMsg();
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (connectResponseMsg.getResponseCode().equals(ConnectResponseCode.ACCEPTED)) {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (connectResponseMsg.hasMaxInboundMessageSize()) {
                            log.debug("[{}] Server max inbound message size: {}", edgeKey, connectResponseMsg.getMaxInboundMessageSize());
                            serverMaxInboundMessageSize = connectResponseMsg.getMaxInboundMessageSize();
                        }
                        log.info("[{}] Configuration received: {}", edgeKey, connectResponseMsg.getConfiguration());
                        onEdgeUpdate.accept(connectResponseMsg.getConfiguration());
                    } else {
                        log.error("[{}] Failed to establish the connection! Code: {}. Error message: {}.", edgeKey, connectResponseMsg.getResponseCode(), connectResponseMsg.getErrorMsg());
                        try {
                            EdgeGrpcClient.this.disconnect(true);
                        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                        } catch (InterruptedException e) {
                            log.error("[{}] Got interruption during disconnect!", edgeKey, e);
                        }
                        onError.accept(new EdgeConnectionException("Failed to establish the connection! Response code: " + connectResponseMsg.getResponseCode().name()));
                    }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (responseMsg.hasEdgeUpdateMsg()) {
                    log.debug("[{}] Edge update message received {}", edgeKey, responseMsg.getEdgeUpdateMsg());
                    onEdgeUpdate.accept(responseMsg.getEdgeUpdateMsg().getConfiguration());
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (responseMsg.hasUplinkResponseMsg()) {
                    log.debug("[{}] Uplink response message received {}", edgeKey, responseMsg.getUplinkResponseMsg());
                    onUplinkResponse.accept(responseMsg.getUplinkResponseMsg());
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (responseMsg.hasDownlinkMsg()) {
                    log.debug("[{}] Downlink message received {}", edgeKey, responseMsg.getDownlinkMsg());
                    onDownlink.accept(responseMsg.getDownlinkMsg());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("[{}] Stream was terminated due to error:", edgeKey, t);
                try {
                    EdgeGrpcClient.this.disconnect(true);
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                } catch (InterruptedException e) {
                    log.error("[{}] Got interruption during disconnect!", edgeKey, e);
                }
                onError.accept(new RuntimeException(t));
            }

            @Override
            public void onCompleted() {
                log.info("[{}] Stream was closed and completed successfully!", edgeKey);
            }
        };
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `disconnect` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void disconnect(boolean onError) throws InterruptedException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!onError) {
            try {
                if (inputStream != null) {
                    inputStream.onCompleted();
                }
            } catch (Exception e) {
                log.error("Exception during onCompleted", e);
            }
        }
        if (channel != null) {
            channel.shutdown();
            int attempt = 0;
            do {
                try {
                    channel.awaitTermination(timeoutSecs, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Channel await termination was interrupted", e);
                }
                if (attempt > 5) {
                    log.warn("We had reached maximum of termination attempts. Force closing channel");
                    try {
                        channel.shutdownNow();
                    } catch (Exception e) {
                        log.error("Exception during shutdownNow", e);
                    }
                    break;
                }
                attempt++;
            } while (!channel.isTerminated());
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendUplinkMsg` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendUplinkMsg(UplinkMsg msg) {
        uplinkMsgLock.lock();
        try {
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                    .setUplinkMsg(msg)
                    .build());
        } finally {
            uplinkMsgLock.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendSyncRequestMsg` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendSyncRequestMsg(boolean fullSyncRequired) {
        uplinkMsgLock.lock();
        try {
            SyncRequestMsg syncRequestMsg = SyncRequestMsg.newBuilder()
                    .setFullSync(fullSyncRequired)
                    .build();
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMsgType(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)
                    .setSyncRequestMsg(syncRequestMsg)
                    .build());
        } finally {
            uplinkMsgLock.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendDownlinkResponseMsg` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendDownlinkResponseMsg(DownlinkResponseMsg downlinkResponseMsg) {
        uplinkMsgLock.lock();
        try {
            this.inputStream.onNext(RequestMsg.newBuilder()
                    .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                    .setDownlinkResponseMsg(downlinkResponseMsg)
                    .build());
        } finally {
            uplinkMsgLock.unlock();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeGrpcClient` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
