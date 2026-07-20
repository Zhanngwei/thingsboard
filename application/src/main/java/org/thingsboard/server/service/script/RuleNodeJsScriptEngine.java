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
package org.thingsboard.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 中文说明：
 * 1. `RuleNodeJsScriptEngine` 是 ThingsBoard Application 中围绕规则节点提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `RuleNodeScriptEngine`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class RuleNodeJsScriptEngine extends RuleNodeScriptEngine<JsInvokeService, JsonNode> {

    /**
     * 功能：创建 `RuleNodeJsScriptEngine` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scriptInvokeService`：服务对象。
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：新创建的对象实例。
     */
    public RuleNodeJsScriptEngine(TenantId tenantId, JsInvokeService scriptInvokeService, String script, String... argNames) {
        super(tenantId, scriptInvokeService, script, argNames);
    }

    /**
     * 功能：执行JSON。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) {
        return executeScriptAsync(msg);
    }

    /**
     * 功能：执行`Update Transform`。
     * 参数：
     * - `msg`：待处理消息。
     * - `json`：`json` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, JsonNode json) {
        if (json.isObject()) {
            return Futures.immediateFuture(Collections.singletonList(unbindMsg(json, msg)));
        } else if (json.isArray()) {
            List<TbMsg> res = new ArrayList<>(json.size());
            json.forEach(jsonObject -> res.add(unbindMsg(jsonObject, msg)));
            return Futures.immediateFuture(res);
        }
        log.warn("Wrong result type: {}", json.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
    }

    /**
     * 功能：执行`Generate Transform`。
     * 参数：
     * - `prevMsg`：待处理消息。
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, JsonNode result) {
        if (!result.isObject()) {
            log.warn("Wrong result type: {}", result.getNodeType());
            Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
        }
        return Futures.immediateFuture(unbindMsg(result, prevMsg));
    }

    /**
     * 功能：转换`Result`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    @Override
    protected JsonNode convertResult(Object result) {
        return JacksonUtil.toJsonNode(result != null ? result.toString() : null);
    }

    /**
     * 功能：执行`To String Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<String> executeToStringTransform(JsonNode result) {
        if (result.isTextual()) {
            return Futures.immediateFuture(result.asText());
        }
        log.warn("Wrong result type: {}", result.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
    }

    /**
     * 功能：执行`Filter Transform`。
     * 参数：
     * - `json`：`json` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<Boolean> executeFilterTransform(JsonNode json) {
        if (json.isBoolean()) {
            return Futures.immediateFuture(json.asBoolean());
        }
        log.warn("Wrong result type: {}", json.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + json.getNodeType()));
    }

    /**
     * 功能：执行`Switch Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<Set<String>> executeSwitchTransform(JsonNode result) {
        if (result.isTextual()) {
            return Futures.immediateFuture(Collections.singleton(result.asText()));
        }
        if (result.isArray()) {
            Set<String> nextStates = new HashSet<>();
            for (JsonNode val : result) {
                if (!val.isTextual()) {
                    log.warn("Wrong result type: {}", val.getNodeType());
                    return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + val.getNodeType()));
                } else {
                    nextStates.add(val.asText());
                }
            }
            return Futures.immediateFuture(nextStates);
        }
        log.warn("Wrong result type: {}", result.getNodeType());
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + result.getNodeType()));
    }

    /**
     * 功能：执行 `prepareArgs` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected Object[] prepareArgs(TbMsg msg) {
        String[] args = new String[3];
        if (msg.getData() != null) {
            args[0] = msg.getData();
        } else {
            args[0] = "";
        }
        args[1] = JacksonUtil.toString(msg.getMetaData().getData());
        args[2] = msg.getType();
        return args;
    }

    /**
     * 功能：执行 `unbindMsg` 对应的处理。
     * 参数：
     * - `msgData`：待处理消息。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private static TbMsg unbindMsg(JsonNode msgData, TbMsg msg) {
        String data = null;
        Map<String, String> metadata = null;
        String messageType = null;
        if (msgData.has(RuleNodeScriptFactory.MSG)) {
            JsonNode msgPayload = msgData.get(RuleNodeScriptFactory.MSG);
            data = JacksonUtil.toString(msgPayload);
        }
        if (msgData.has(RuleNodeScriptFactory.METADATA)) {
            JsonNode msgMetadata = msgData.get(RuleNodeScriptFactory.METADATA);
            metadata = JacksonUtil.convertValue(msgMetadata, new TypeReference<>() {
            });
        }
        if (msgData.has(RuleNodeScriptFactory.MSG_TYPE)) {
            messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).asText();
        }
        String newData = data != null ? data : msg.getData();
        TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
        String newMessageType = !StringUtils.isEmpty(messageType) ? messageType : msg.getType();
        return TbMsg.transformMsg(msg, newMessageType, msg.getOriginator(), newMetadata, newData);
    }
}
