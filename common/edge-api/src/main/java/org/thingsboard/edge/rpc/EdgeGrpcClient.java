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
@Service
@Slf4j
public class EdgeGrpcClient implements EdgeRpcClient {

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${cloud.rpc.host}")
    private String rpcHost;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${cloud.rpc.port}")
    private int rpcPort;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${cloud.rpc.timeout}")
    private int timeoutSecs;
    /**
     * 时间，用于控制时间范围或等待时长。
     */
    @Value("${cloud.rpc.keep_alive_time_sec:10}")
    private int keepAliveTimeSec;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${cloud.rpc.keep_alive_timeout_sec:5}")
    private int keepAliveTimeoutSec;
    /**
     * 是否启用 SSL。
     */
    @Value("${cloud.rpc.ssl.enabled}")
    private boolean sslEnabled;
    /**
     * `certResource` 字段，保存当前对象的对应属性。
     */
    @Value("${cloud.rpc.ssl.cert:}")
    private String certResource;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Value("${cloud.rpc.max_inbound_message_size:4194304}")
    private int maxInboundMessageSize;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Getter
    private int serverMaxInboundMessageSize;

    /**
     * 网络通道，表示当前网络连接使用的通道。
     */
    private ManagedChannel channel;

    /**
     * `inputStream` 字段，保存当前对象的对应属性。
     */
    private StreamObserver<RequestMsg> inputStream;

    private static final ReentrantLock uplinkMsgLock = new ReentrantLock();

    /**
     * 功能：执行 `connect` 对应的处理。
     * 参数：
     * - `edgeKey`：键。
     * - `edgeSecret`：`edgeSecret` 参数。
     * - `onUplinkResponse`：响应对象。
     * - `onEdgeUpdate`：`onEdgeUpdate` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
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
        if (sslEnabled) {
            try {
                SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
                if (StringUtils.isNotEmpty(certResource)) {
                    sslContextBuilder.trustManager(ResourceUtils.getInputStream(this, certResource));
                }
                builder.sslContext(sslContextBuilder.build());
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
     * 功能：初始化或启动`Output Stream`。
     * 参数：
     * - `edgeKey`：键。
     * - `onUplinkResponse`：响应对象。
     * - `onEdgeUpdate`：`onEdgeUpdate` 参数。
     * - `onDownlink`：`onDownlink` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private StreamObserver<ResponseMsg> initOutputStream(String edgeKey,
                                                         Consumer<UplinkResponseMsg> onUplinkResponse,
                                                         Consumer<EdgeConfiguration> onEdgeUpdate,
                                                         Consumer<DownlinkMsg> onDownlink,
                                                         Consumer<Exception> onError) {
        return new StreamObserver<>() {
            @Override
            public void onNext(ResponseMsg responseMsg) {
                if (responseMsg.hasConnectResponseMsg()) {
                    ConnectResponseMsg connectResponseMsg = responseMsg.getConnectResponseMsg();
                    if (connectResponseMsg.getResponseCode().equals(ConnectResponseCode.ACCEPTED)) {
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
                        } catch (InterruptedException e) {
                            log.error("[{}] Got interruption during disconnect!", edgeKey, e);
                        }
                        onError.accept(new EdgeConnectionException("Failed to establish the connection! Response code: " + connectResponseMsg.getResponseCode().name()));
                    }
                } else if (responseMsg.hasEdgeUpdateMsg()) {
                    log.debug("[{}] Edge update message received {}", edgeKey, responseMsg.getEdgeUpdateMsg());
                    onEdgeUpdate.accept(responseMsg.getEdgeUpdateMsg().getConfiguration());
                } else if (responseMsg.hasUplinkResponseMsg()) {
                    log.debug("[{}] Uplink response message received {}", edgeKey, responseMsg.getUplinkResponseMsg());
                    onUplinkResponse.accept(responseMsg.getUplinkResponseMsg());
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

    /**
     * 功能：执行 `disconnect` 对应的处理。
     * 参数：
     * - `onError`：错误信息。
     * 返回：无。
     */
    @Override
    public void disconnect(boolean onError) throws InterruptedException {
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

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
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

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `fullSyncRequired`：`fullSyncRequired` 参数。
     * 返回：无。
     */
    @Override
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

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `downlinkResponseMsg`：响应对象。
     * 返回：无。
     */
    @Override
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
