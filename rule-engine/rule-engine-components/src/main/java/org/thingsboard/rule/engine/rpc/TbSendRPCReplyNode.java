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
package org.thingsboard.rule.engine.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

/**
 * `TbSendRPCReplyNode` 类，封装当前模块中的一组相关职责。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rpc call reply",
        configClazz = TbSendRpcReplyNodeConfiguration.class,
        nodeDescription = "Sends reply to RPC call from device",
        nodeDetails = "Expects messages with any message type. Will forward message body to the device.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRpcReplyConfig",
        icon = "call_merge"
)
public class TbSendRPCReplyNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbSendRpcReplyNodeConfiguration config;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendRpcReplyNodeConfiguration.class);
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String serviceIdStr = msg.getMetaData().getValue(config.getServiceIdMetaDataAttribute());
        String sessionIdStr = msg.getMetaData().getValue(config.getSessionIdMetaDataAttribute());
        String requestIdStr = msg.getMetaData().getValue(config.getRequestIdMetaDataAttribute());
        if (msg.getOriginator().getEntityType() != EntityType.DEVICE) {
            ctx.tellFailure(msg, new RuntimeException("Message originator is not a device entity!"));
        } else if (StringUtils.isEmpty(requestIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Request id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(serviceIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Service id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(sessionIdStr)) {
            ctx.tellFailure(msg, new RuntimeException("Session id is not present in the metadata!"));
        } else if (StringUtils.isEmpty(msg.getData())) {
            ctx.tellFailure(msg, new RuntimeException("Request body is empty!"));
        } else {
            if (StringUtils.isNotBlank(msg.getMetaData().getValue(DataConstants.EDGE_ID))) {
                saveRpcResponseToEdgeQueue(ctx, msg, serviceIdStr, sessionIdStr, requestIdStr);
            } else {
                ctx.getRpcService().sendRpcReplyToDevice(serviceIdStr, UUID.fromString(sessionIdStr), Integer.parseInt(requestIdStr), msg.getData());
                ctx.tellSuccess(msg);
            }
        }
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `serviceIdStr`：服务对象。
     * - `sessionIdStr`：会话对象。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void saveRpcResponseToEdgeQueue(TbContext ctx, TbMsg msg, String serviceIdStr, String sessionIdStr, String requestIdStr) {
        EdgeId edgeId;
        DeviceId deviceId;
        try {
            edgeId = new EdgeId(UUID.fromString(msg.getMetaData().getValue(DataConstants.EDGE_ID)));
            deviceId = new DeviceId(UUID.fromString(msg.getMetaData().getValue(DataConstants.DEVICE_ID)));
        } catch (Exception e) {
            String errMsg = String.format("[%s] Failed to parse edgeId or deviceId from metadata %s!", ctx.getTenantId(), msg.getMetaData());
            ctx.tellFailure(msg, new RuntimeException(errMsg));
            return;
        }

        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("serviceId", serviceIdStr);
        body.put("sessionId", sessionIdStr);
        body.put("requestId", requestIdStr);
        body.put("response", msg.getData());
        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(ctx.getTenantId(), edgeId, EdgeEventType.DEVICE,
                        EdgeEventActionType.RPC_CALL, deviceId, JacksonUtil.valueToTree(body));
        ListenableFuture<Void> future = ctx.getEdgeEventService().saveAsync(edgeEvent);
        Futures.addCallback(future, new FutureCallback<>() {
            /**
             * 功能：处理`on Success`。
             * 参数：
             * - `result`：`result` 参数。
             * 返回：无。
             */
            @Override
            public void onSuccess(Void result) {
                ctx.onEdgeEventUpdate(ctx.getTenantId(), edgeId);
                ctx.tellSuccess(msg);
            }

            /**
             * 功能：处理失败信息。
             * 参数：
             * - `t`：`t` 参数。
             * 返回：无。
             */
            @Override
            public void onFailure(Throwable t) {
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }
}

/*
 * 本类总结：
 * 本类负责发送设备 RPC 回复：普通路径调用 RpcService，Edge 路径把回复保存到 EdgeEvent 队列。
 * 普通路径本身不直接访问数据库或缓存；Edge 路径通过 EdgeEventService.saveAsync 直接进入异步持久化边界。
 * RpcService、Actor、MQTT/传输和 Edge 同步的具体实现/调用链可能间接涉及其它系统组件。
 */
