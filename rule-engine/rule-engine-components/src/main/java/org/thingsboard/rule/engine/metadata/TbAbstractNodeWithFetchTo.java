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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * 中文说明：
 * 1. `TbAbstractNodeWithFetchTo` 是 ThingsBoard Rule Engine Components 中处理 `Tb Node With` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractFetchToNodeConfiguration`、`TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
public abstract class TbAbstractNodeWithFetchTo<C extends TbAbstractFetchToNodeConfiguration> implements TbNode {

    /**
     * 名称常量，用于统一引用固定值。
     */
    protected final static String FETCH_TO_PROPERTY_NAME = "fetchTo";

    /**
     * 配置，保存当前对象的配置选项。
     */
    protected C config;
    /**
     * `fetchTo` 字段，保存当前对象的对应属性。
     */
    protected TbMsgSource fetchTo;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = loadNodeConfiguration(configuration);
        if (config.getFetchTo() == null) {
            throw new TbNodeException("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        fetchTo = config.getFetchTo();
    }

    /**
     * 功能：获取节点实例。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    protected abstract C loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 功能：校验实体。
     * 参数：
     * - `message`：待处理消息。
     * 返回：判断结果。
     */
    protected <I extends EntityId> AsyncFunction<I, I> checkIfEntityIsPresentOrThrow(String message) {
        return id -> {
            if (id == null || id.isNullUid()) {
                return Futures.immediateFailedFuture(new NoSuchElementException(message));
            }
            return Futures.immediateFuture(id);
        };
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    protected ObjectNode getMsgDataAsObjectNode(TbMsg msg) {
        var msgDataNode = JacksonUtil.toJsonNode(msg.getData());
        if (msgDataNode == null || !msgDataNode.isObject()) {
            throw new IllegalArgumentException("Message body is not an object!");
        }
        return (ObjectNode) msgDataNode;
    }

    /**
     * 功能：执行 `enrichMessage` 对应的处理。
     * 参数：
     * - `msgData`：待处理消息。
     * - `metaData`：待处理数据。
     * - `kvEntry`：`kvEntry` 参数。
     * - `targetKey`：键。
     * 返回：无。
     */
    protected void enrichMessage(ObjectNode msgData, TbMsgMetaData metaData, KvEntry kvEntry, String targetKey) {
        if (TbMsgSource.DATA.equals(fetchTo)) {
            JacksonUtil.addKvEntry(msgData, kvEntry, targetKey);
        } else if (TbMsgSource.METADATA.equals(fetchTo)) {
            metaData.putValue(targetKey, kvEntry.getValueAsString());
        }
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `msg`：待处理消息。
     * - `msgDataNode`：待处理消息。
     * - `msgMetaData`：待处理消息。
     * 返回：处理结果。
     */
    protected TbMsg transformMessage(TbMsg msg, ObjectNode msgDataNode, TbMsgMetaData msgMetaData) {
        switch (fetchTo) {
            case DATA:
                return TbMsg.transformMsgData(msg, JacksonUtil.toString(msgDataNode));
            case METADATA:
                return TbMsg.transformMsgMetadata(msg, msgMetaData);
            default:
                log.debug("Unexpected FetchTo value: {}. Allowed values: {}", fetchTo, TbMsgSource.values());
                return msg;
        }
    }

    /**
     * 功能：执行 `upgradeRuleNodesWithOldPropertyToUseFetchTo` 对应的处理。
     * 参数：
     * - `oldConfiguration`：配置对象。
     * - `oldProperty`：`oldProperty` 参数。
     * - `ifTrue`：`ifTrue` 参数。
     * - `ifFalse`：`ifFalse` 参数。
     * 返回：处理结果。
     */
    protected TbPair<Boolean, JsonNode> upgradeRuleNodesWithOldPropertyToUseFetchTo(
            JsonNode oldConfiguration,
            String oldProperty,
            String ifTrue,
            String ifFalse
    ) throws TbNodeException {
        var newConfig = (ObjectNode) oldConfiguration;
        if (!newConfig.has(oldProperty)) {
            throw new TbNodeException("property to update: '" + oldProperty + "' doesn't exists in configuration!");
        }
        return upgradeConfigurationToUseFetchTo(oldProperty, ifTrue, ifFalse, newConfig);
    }

    /**
     * 功能：执行 `upgradeConfigurationToUseFetchTo` 对应的处理。
     * 参数：
     * - `oldProperty`：`oldProperty` 参数。
     * - `ifTrue`：`ifTrue` 参数。
     * - `ifFalse`：`ifFalse` 参数。
     * - `newConfig`：配置对象。
     * 返回：处理结果。
     */
    protected TbPair<Boolean, JsonNode> upgradeConfigurationToUseFetchTo(
            String oldProperty, String ifTrue,
            String ifFalse, ObjectNode newConfig
    ) throws TbNodeException {
        var value = newConfig.get(oldProperty).asText();
        if ("true".equals(value)) {
            newConfig.remove(oldProperty);
            newConfig.put(FETCH_TO_PROPERTY_NAME, ifTrue);
            return new TbPair<>(true, newConfig);
        } else if ("false".equals(value)) {
            newConfig.remove(oldProperty);
            newConfig.put(FETCH_TO_PROPERTY_NAME, ifFalse);
            return new TbPair<>(true, newConfig);
        } else {
            throw new TbNodeException("property to update: '" + oldProperty + "' has unexpected value: "
                    + value + ". Allowed values: true or false!");
        }
    }
}
