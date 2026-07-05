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
package org.thingsboard.server.transport.lwm2m.server.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.attributes.LwM2MAttributesService;
import org.thingsboard.server.transport.lwm2m.server.rpc.LwM2MRpcRequestHandler;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG;

/**
 * 中文说明：
 * 1. 类目的：`DefaultLwM2MSessionManager` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Service
@TbLwM2mTransportComponent
public class DefaultLwM2MSessionManager implements LwM2MSessionManager {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TransportService transportService;
    private final LwM2MAttributesService attributesService;
    /**
     * RPC，负责处理对应任务或消息。
     */
    private final LwM2MRpcRequestHandler rpcHandler;
    private final LwM2mUplinkMsgHandler uplinkHandler;

    /**
     * 功能：创建 `DefaultLwM2MSessionManager` 实例，并初始化必要字段。
     * 参数：
     * - `transportService`：服务对象。
     * - `attributesService`：服务对象。
     * - `rpcHandler`：处理器对象。
     * - `uplinkHandler`：处理器对象。
     * 返回：新创建的对象实例。
     */
    public DefaultLwM2MSessionManager(TransportService transportService,
                                      @Lazy LwM2MAttributesService attributesService,
                                      @Lazy LwM2MRpcRequestHandler rpcHandler,
                                      @Lazy LwM2mUplinkMsgHandler uplinkHandler) {
        this.transportService = transportService;
        this.attributesService = attributesService;
        this.rpcHandler = rpcHandler;
        this.uplinkHandler = uplinkHandler;
    }

    /**
     * 功能：执行 `register` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    @Override
    public void register(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(uplinkHandler, attributesService, rpcHandler, sessionInfo, transportService));
        TransportProtos.TransportToDeviceActorMsg msg = TransportProtos.TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(sessionInfo)
                .setSessionEvent(SESSION_EVENT_MSG_OPEN)
                .setSubscribeToAttributes(SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG)
                .setSubscribeToRPC(SUBSCRIBE_TO_RPC_ASYNC_MSG)
                .build();
        transportService.process(msg, null);
    }

    /**
     * 功能：执行 `deregister` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    @Override
    public void deregister(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
        transportService.deregisterSession(sessionInfo);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultLwM2MSessionManager` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
