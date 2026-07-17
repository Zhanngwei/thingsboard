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
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 中文说明：
 * 1. `RuleNodeTbelScriptEngine` 是 ThingsBoard Application 中围绕规则节点提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `RuleNodeScriptEngine`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class RuleNodeTbelScriptEngine extends RuleNodeScriptEngine<TbelInvokeService, Object> {

    /**
     * 功能：创建 `RuleNodeTbelScriptEngine` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scriptInvokeService`：服务对象。
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：新创建的对象实例。
     */
    public RuleNodeTbelScriptEngine(TenantId tenantId, TbelInvokeService scriptInvokeService, String script, String... argNames) {
        super(tenantId, scriptInvokeService, script, argNames);
    }

    /**
     * 功能：执行`Filter Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<Boolean> executeFilterTransform(Object result) {
        if (result instanceof Boolean) {
            return Futures.immediateFuture((Boolean) result);
        }
        return wrongResultType(result);
    }

    /**
     * 功能：执行`Update Transform`。
     * 参数：
     * - `msg`：待处理消息。
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, Object result) {
        if (result instanceof Map) {
            return Futures.immediateFuture(Collections.singletonList(unbindMsg((Map) result, msg)));
        } else if (result instanceof Collection) {
            List<TbMsg> res = new ArrayList<>();
            for (Object resObject : (Collection) result) {
                if (resObject instanceof Map) {
                    res.add(unbindMsg((Map) resObject, msg));
                } else {
                    return wrongResultType(resObject);
                }
            }
            return Futures.immediateFuture(res);
        }
        return wrongResultType(result);
    }

    /**
     * 功能：执行`Generate Transform`。
     * 参数：
     * - `prevMsg`：待处理消息。
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, Object result) {
        if (result instanceof Map) {
            return Futures.immediateFuture(unbindMsg((Map) result, prevMsg));
        }
        return wrongResultType(result);
    }

    /**
     * 功能：执行`To String Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<String> executeToStringTransform(Object result) {
        if (result instanceof String) {
            return Futures.immediateFuture((String) result);
        } else {
            return Futures.immediateFuture(JacksonUtil.toString(result));
        }
    }

    /**
     * 功能：执行`Switch Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<Set<String>> executeSwitchTransform(Object result) {
        if (result instanceof String) {
            return Futures.immediateFuture(Collections.singleton((String) result));
        } else if (result instanceof Collection) {
            Set<String> res = new HashSet<>();
            for (Object resObject : (Collection) result) {
                if (resObject instanceof String) {
                    res.add((String) resObject);
                } else {
                    return wrongResultType(resObject);
                }
            }
            return Futures.immediateFuture(res);
        }
        return wrongResultType(result);
    }

    /**
     * 功能：执行JSON。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), JacksonUtil::valueToTree, MoreExecutors.directExecutor());

    }

    /**
     * 功能：转换`Result`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    @Override
    protected Object convertResult(Object result) {
        return result;
    }

    /**
     * 功能：执行 `prepareArgs` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected Object[] prepareArgs(TbMsg msg) {
        Object[] args = new Object[3];
        if (msg.getData() != null) {
            args[0] = JacksonUtil.fromString(msg.getData(), Object.class);
        } else {
            args[0] = new HashMap<>();
        }
        args[1] = new HashMap<>(msg.getMetaData().getData());
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
    private static TbMsg unbindMsg(Map msgData, TbMsg msg) {
        String data = null;
        Map<String, String> metadata = null;
        String messageType = null;
        if (msgData.containsKey(RuleNodeScriptFactory.MSG)) {
            data = JacksonUtil.toString(msgData.get(RuleNodeScriptFactory.MSG));
        }
        if (msgData.containsKey(RuleNodeScriptFactory.METADATA)) {
            Object msgMetadataObj = msgData.get(RuleNodeScriptFactory.METADATA);
            if (msgMetadataObj instanceof Map) {
                metadata = ((Map<?, ?>) msgMetadataObj).entrySet().stream().filter(e -> e.getValue() != null)
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            } else {
                metadata = JacksonUtil.convertValue(msgMetadataObj, new TypeReference<>() {
                });
            }
        }
        if (msgData.containsKey(RuleNodeScriptFactory.MSG_TYPE)) {
            messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).toString();
        }
        String newData = data != null ? data : msg.getData();
        TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
        String newMessageType = !StringUtils.isEmpty(messageType) ? messageType : msg.getType();
        return TbMsg.transformMsg(msg, newMessageType, msg.getOriginator(), newMetadata, newData);
    }

    /**
     * 功能：执行 `wrongResultType` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    private static <T> ListenableFuture<T> wrongResultType(Object result) {
        String className = toClassName(result);
        log.warn("Wrong result type: {}", className);
        return Futures.immediateFailedFuture(new ScriptException("Wrong result type: " + className));
    }

    /**
     * 功能：执行 `toClassName` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：文本结果。
     */
    private static String toClassName(Object result) {
        return result != null ? result.getClass().getSimpleName() : "null";
    }
}
