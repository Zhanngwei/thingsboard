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
 * 1. `DefaultLwM2MSessionManager` 是 ThingsBoard Common Transport 中负责会话的协调管理组件。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `LwM2MSessionManager`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
