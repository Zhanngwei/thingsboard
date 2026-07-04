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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rpc call request",
        configClazz = TbSendRpcRequestNodeConfiguration.class,
        nodeDescription = "Sends RPC call to device",
        nodeDetails = "Expects messages with \"method\" and \"params\". Will forward response from device to next nodes." +
                "If the RPC call request is originated by REST API call from user, will forward the response to user immediately.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRpcRequestConfig",
        icon = "call_made"
)
/**
 * 服务端到设备 RPC 请求节点，基于消息体构造 RuleEngineDeviceRpcRequest 并交给 RpcService。
 * 本类不直接访问数据库或缓存；RPC 服务的具体传输、Actor、MQTT 或持久化调用链可能间接涉及。
 */
public class TbSendRPCRequestNode implements TbNode {

    /**
     * 未显式提供 requestId 时使用的随机数生成器。
     */
    private Random random = new Random();
    /**
     * JSON 包装和参数序列化使用的 Gson 实例。
     */
    private Gson gson = new Gson();
    /**
     * RPC 请求节点配置，当前主要提供默认超时时间。
     */
    private TbSendRpcRequestNodeConfiguration config;

    /**
     * 初始化 RPC 请求节点配置。
     * 本方法不直接发送 RPC、不访问数据库或缓存，也不处理消息确认。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendRpcRequestNodeConfiguration.class);
    }

    /**
     * 校验消息体并发送 RPC 请求到设备。
     * 外部调用边界是 ctx.getRpcService().sendRpcRequestToDevice；响应回调中根据设备响应入队 Success 或 Failure。
     * 本方法显式在请求提交后 ctx.ack(msg)，因此消息确认发生在 RPC 响应到达前。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        JsonObject json = JsonParser.parseString(msg.getData()).getAsJsonObject();
        String tmp;
        if (msg.getOriginator().getEntityType() != EntityType.DEVICE) {
            ctx.tellFailure(msg, new RuntimeException("Message originator is not a device entity!"));
        } else if (!json.has("method")) {
            ctx.tellFailure(msg, new RuntimeException("Method is not present in the message!"));
        } else if (!json.has("params")) {
            ctx.tellFailure(msg, new RuntimeException("Params are not present in the message!"));
        } else {
            int requestId = json.has("requestId") ? json.get("requestId").getAsInt() : random.nextInt();
            boolean restApiCall = msg.isTypeOf(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE);

            tmp = msg.getMetaData().getValue("oneway");
            boolean oneway = !StringUtils.isEmpty(tmp) && Boolean.parseBoolean(tmp);

            tmp = msg.getMetaData().getValue(DataConstants.PERSISTENT);
            boolean persisted = !StringUtils.isEmpty(tmp) && Boolean.parseBoolean(tmp);

            tmp = msg.getMetaData().getValue("requestUUID");
            UUID requestUUID = !StringUtils.isEmpty(tmp) ? UUID.fromString(tmp) : Uuids.timeBased();
            tmp = msg.getMetaData().getValue("originServiceId");
            String originServiceId = !StringUtils.isEmpty(tmp) ? tmp : null;

            tmp = msg.getMetaData().getValue(DataConstants.EXPIRATION_TIME);
            long expirationTime = !StringUtils.isEmpty(tmp) ? Long.parseLong(tmp) : (System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(config.getTimeoutInSeconds()));

            tmp = msg.getMetaData().getValue(DataConstants.RETRIES);
            Integer retries = !StringUtils.isEmpty(tmp) ? Integer.parseInt(tmp) : null;

            // params/additionalInfo 既允许 JSON 字符串，也允许对象/数组，发送前统一转为字符串负载。
            String params = parseJsonData(json.get("params"));
            String additionalInfo = parseJsonData(json.get(DataConstants.ADDITIONAL_INFO));

            RuleEngineDeviceRpcRequest request = RuleEngineDeviceRpcRequest.builder()
                    .oneway(oneway)
                    .method(json.get("method").getAsString())
                    .body(params)
                    .tenantId(ctx.getTenantId())
                    .deviceId(new DeviceId(msg.getOriginator().getId()))
                    .requestId(requestId)
                    .requestUUID(requestUUID)
                    .originServiceId(originServiceId)
                    .expirationTime(expirationTime)
                    .retries(retries)
                    .restApiCall(restApiCall)
                    .persisted(persisted)
                    .additionalInfo(additionalInfo)
                    .build();

            ctx.getRpcService().sendRpcRequestToDevice(request, ruleEngineDeviceRpcResponse -> {
                // RPC 响应回调可能来自 RPC/Actor 调用链，这里只负责把结果重新入队到规则链。
                if (ruleEngineDeviceRpcResponse.getError().isEmpty()) {
                    TbMsg next = ctx.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getCustomerId(), msg.getMetaData(), ruleEngineDeviceRpcResponse.getResponse().orElse(TbMsg.EMPTY_JSON_OBJECT));
                    ctx.enqueueForTellNext(next, TbNodeConnectionType.SUCCESS);
                } else {
                    TbMsg next = ctx.newMsg(msg.getQueueName(), msg.getType(), msg.getOriginator(), msg.getCustomerId(), msg.getMetaData(), wrap("error", ruleEngineDeviceRpcResponse.getError().get().name()));
                    ctx.enqueueForTellFailure(next, ruleEngineDeviceRpcResponse.getError().get().name());
                }
            });
            ctx.ack(msg);
        }
    }

    /**
     * 将错误内容包装为 JSON 对象字符串。
     * 本方法只处理本地 JSON，不直接涉及 RPC、数据库或缓存。
     */
    private String wrap(String name, String body) {
        JsonObject json = new JsonObject();
        json.addProperty(name, body);
        return gson.toJson(json);
    }

    /**
     * 将 JSON 参数元素转换为 RPC 请求字符串。
     * primitive 直接取字符串值，复杂 JSON 保持 JSON 序列化形式；本方法不直接访问外部系统。
     */
    private String parseJsonData(JsonElement paramsEl) {
        if (paramsEl != null) {
            return paramsEl.isJsonPrimitive() ? paramsEl.getAsString() : gson.toJson(paramsEl);
        } else {
            return null;
        }
    }

}

/*
 * 本类总结：
 * 本类根据 TbMsg 构造服务端到设备的 RPC 请求，并通过 RpcService 异步等待响应回调。
 * 节点提交 RPC 后立即 ack 原消息，之后由 RPC 回调把响应重新入队到 Success 或 Failure。
 * 本类本身不直接涉及数据库或缓存；RPC 服务、Actor、传输层、MQTT 或持久化的具体实现/调用链可能间接涉及。
 */
