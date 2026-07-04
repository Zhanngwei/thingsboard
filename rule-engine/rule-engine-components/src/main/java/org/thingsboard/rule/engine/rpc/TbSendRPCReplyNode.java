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
/**
 * 设备到服务端 RPC 回复节点，根据元数据定位 RPC 会话并发送回复。
 * 普通路径直接委托 RpcService；Edge 路径会异步保存 EdgeEvent，本类本身不直接管理底层传输连接。
 */
public class TbSendRPCReplyNode implements TbNode {

    /**
     * RPC 回复节点配置，定义 serviceId、sessionId 和 requestId 的元数据字段名。
     */
    private TbSendRpcReplyNodeConfiguration config;

    /**
     * 初始化 RPC 回复节点配置。
     * 本方法不直接发送 RPC、不访问数据库或缓存，也不处理消息确认。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendRpcReplyNodeConfiguration.class);
    }

    /**
     * 校验 RPC 回复所需元数据并发送到设备会话。
     * 非 Edge 场景直接调用 RpcService 并成功路由；Edge 场景保存 EdgeEvent 后由数据库回调决定成功或失败。
     * RpcService 的 Actor/MQTT/传输细节不在本类直接实现，具体调用链可能间接涉及。
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
     * 将 RPC 回复保存为 EdgeEvent，等待 Edge 同步链路处理。
     * 本方法直接调用 EdgeEventService.saveAsync，属于数据库/持久化边界；保存结果通过 dbCallbackExecutor 回调路由。
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
             * EdgeEvent 保存成功后通知 Edge 更新并走 Success。
             * 本回调运行在 dbCallbackExecutor 上。
             */
            @Override
            public void onSuccess(Void result) {
                ctx.onEdgeEventUpdate(ctx.getTenantId(), edgeId);
                ctx.tellSuccess(msg);
            }

            /**
             * EdgeEvent 保存失败后走 Failure。
             * 失败通常来自数据库/持久化或 EdgeEventService 调用链。
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
