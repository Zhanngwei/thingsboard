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
 * 中文说明：`TbAbstractNodeWithFetchTo` 是抽象节点With获取到规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
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

    /*
     * 本类总结：`TbAbstractNodeWithFetchTo` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
