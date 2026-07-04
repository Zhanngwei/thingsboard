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

@Slf4j
/**
 * 中文说明：`TbAbstractNodeWithFetchTo` 是抽象节点With获取到规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public abstract class TbAbstractNodeWithFetchTo<C extends TbAbstractFetchToNodeConfiguration> implements TbNode {

    /**
     * 常量字段：定义 `FETCH_TO_PROPERTY_NAME`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    protected final static String FETCH_TO_PROPERTY_NAME = "fetchTo";

    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    protected C config;
    /**
     * 字段说明：保存 `fetchTo`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    protected TbMsgSource fetchTo;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = loadNodeConfiguration(configuration);
        if (config.getFetchTo() == null) {
            throw new TbNodeException("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        fetchTo = config.getFetchTo();
    }

    /**
     * 方法说明：加载或解析本类处理所需的配置、实体或辅助数据，供 `TbAbstractNodeWithFetchTo` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract C loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 方法说明：检查状态、关系、配置或数据合法性，供 `TbAbstractNodeWithFetchTo` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAbstractNodeWithFetchTo` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected ObjectNode getMsgDataAsObjectNode(TbMsg msg) {
        var msgDataNode = JacksonUtil.toJsonNode(msg.getData());
        if (msgDataNode == null || !msgDataNode.isObject()) {
            throw new IllegalArgumentException("Message body is not an object!");
        }
        return (ObjectNode) msgDataNode;
    }

    /**
     * 方法说明：执行 `enrichMessage` 对应的辅助逻辑，供 `TbAbstractNodeWithFetchTo` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected void enrichMessage(ObjectNode msgData, TbMsgMetaData metaData, KvEntry kvEntry, String targetKey) {
        if (TbMsgSource.DATA.equals(fetchTo)) {
            JacksonUtil.addKvEntry(msgData, kvEntry, targetKey);
        } else if (TbMsgSource.METADATA.equals(fetchTo)) {
            metaData.putValue(targetKey, kvEntry.getValueAsString());
        }
    }

    /**
     * 方法说明：转换规则链消息或转换异步处理结果，供 `TbAbstractNodeWithFetchTo` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：迁移旧版本规则节点 JSON 配置结构，供 `TbAbstractNodeWithFetchTo` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：迁移旧版本规则节点 JSON 配置结构，供 `TbAbstractNodeWithFetchTo` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
