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
package org.thingsboard.server.actors.device;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

/**
 * 中文说明：
 * 1. 类目的：`DeviceActor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@Slf4j
public class DeviceActor extends ContextAwareActor {

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final DeviceActorMessageProcessor processor;

    DeviceActor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}][{}] Starting device actor.", processor.tenantId, processor.deviceId);
        try {
            processor.init(ctx);
            log.debug("[{}][{}] Device actor started.", processor.tenantId, processor.deviceId);
        } catch (Exception e) {
            log.warn("[{}][{}] Unknown failure", processor.tenantId, processor.deviceId, e);
            throw new TbActorException("Failed to initialize device actor", e);
        }
    }

    /**
     * 功能：执行 `doProcess` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    protected boolean doProcess(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                processor.process((TransportToDeviceActorMsgWrapper) msg);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processAttributesUpdate((DeviceAttributesEventNotificationMsg) msg);
                break;
            case DEVICE_DELETE_TO_DEVICE_ACTOR_MSG:
                ctx.stop(ctx.getSelf());
                break;
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processCredentialsUpdate(msg);
                break;
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processNameOrTypeUpdate((DeviceNameOrTypeUpdateMsg) msg);
                break;
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
                processor.processRpcRequest(ctx, (ToDeviceRpcRequestActorMsg) msg);
                break;
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
                processor.processRpcResponsesFromEdge((FromDeviceRpcResponseActorMsg) msg);
                break;
            case DEVICE_ACTOR_SERVER_SIDE_RPC_TIMEOUT_MSG:
                processor.processServerSideRpcTimeout((DeviceActorServerSideRpcTimeoutMsg) msg);
                break;
            case SESSION_TIMEOUT_MSG:
                processor.checkSessionsTimeout();
                break;
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
                processor.processEdgeUpdate((DeviceEdgeUpdateMsg) msg);
                break;
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                processor.processRemoveRpc((RemoveRpcActorMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceActor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
