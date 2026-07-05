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
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * 中文说明：`TbAbstractGetEntityDataNode` 是抽象获取实体数据节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
public abstract class TbAbstractGetEntityDataNode<T extends EntityId> extends TbAbstractGetMappedDataNode<T, TbGetEntityDataNodeConfiguration> {

    /**
     * 数据常量，用于统一引用固定值。
     */
    private final static String DATA_TO_FETCH_PROPERTY_NAME = "dataToFetch";
    /**
     * 数据常量，用于统一引用固定值。
     */
    private static final String OLD_DATA_TO_FETCH_PROPERTY_NAME = "telemetry";
    /**
     * 数据常量，用于统一引用固定值。
     */
    private final static String DATA_MAPPING_PROPERTY_NAME = "dataMapping";
    /**
     * 数据常量，用于统一引用固定值。
     */
    private static final String OLD_DATA_MAPPING_PROPERTY_NAME = "attrMapping";

    /**
     * 消息常量，用于统一引用固定值。
     */
    private static final String DATA_TO_FETCH_VALIDATION_MSG = "DataToFetch property has invalid value: %s." +
            " Only ATTRIBUTES and LATEST_TELEMETRY values supported!";

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        withCallback(findEntityAsync(ctx, msg.getOriginator()),
                entityId -> processDataAndTell(ctx, msg, entityId, msgDataAsObjectNode),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `originator`：`originator` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<T> findEntityAsync(TbContext ctx, EntityId originator);

    /**
     * 功能：校验数据。
     * 参数：
     * - `dataToFetch`：待处理数据。
     * 返回：无。
     */
    protected void checkDataToFetchSupportedOrElseThrow(DataToFetch dataToFetch) throws TbNodeException {
        if (dataToFetch == null || dataToFetch.equals(DataToFetch.FIELDS)) {
            throw new TbNodeException(String.format(DATA_TO_FETCH_VALIDATION_MSG, dataToFetch));
        }
    }

    /**
     * 功能：处理数据。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `entityId`：实体IDID。
     * - `msgDataAsJsonNode`：待处理消息。
     * 返回：无。
     */
    protected void processDataAndTell(TbContext ctx, TbMsg msg, T entityId, ObjectNode msgDataAsJsonNode) {
        DataToFetch dataToFetch = config.getDataToFetch();
        switch (dataToFetch) {
            case ATTRIBUTES:
                processAttributesKvEntryData(ctx, msg, entityId, msgDataAsJsonNode);
                break;
            case LATEST_TELEMETRY:
                processTsKvEntryData(ctx, msg, entityId, msgDataAsJsonNode);
                break;
            case FIELDS:
                processFieldsData(ctx, msg, entityId, msgDataAsJsonNode, true);
                break;
        }
    }

    /**
     * 功能：执行 `upgradeToUseFetchToAndDataToFetch` 对应的处理。
     * 参数：
     * - `oldConfiguration`：配置对象。
     * 返回：处理结果。
     */
    protected TbPair<Boolean, JsonNode> upgradeToUseFetchToAndDataToFetch(JsonNode oldConfiguration) throws TbNodeException {
        var newConfigObjectNode = (ObjectNode) oldConfiguration;
        if (!newConfigObjectNode.has(OLD_DATA_TO_FETCH_PROPERTY_NAME)) {
            throw new TbNodeException("property to update: '" + OLD_DATA_TO_FETCH_PROPERTY_NAME + "' doesn't exists in configuration!");
        }
        if (!newConfigObjectNode.has(OLD_DATA_MAPPING_PROPERTY_NAME)) {
            throw new TbNodeException("property to update: '" + OLD_DATA_MAPPING_PROPERTY_NAME + "' doesn't exists in configuration!");
        }
        newConfigObjectNode.set(DATA_MAPPING_PROPERTY_NAME, newConfigObjectNode.get(OLD_DATA_MAPPING_PROPERTY_NAME));
        newConfigObjectNode.remove(OLD_DATA_MAPPING_PROPERTY_NAME);
        var value = newConfigObjectNode.get(OLD_DATA_TO_FETCH_PROPERTY_NAME).asText();
        if ("true".equals(value)) {
            newConfigObjectNode.remove(OLD_DATA_TO_FETCH_PROPERTY_NAME);
            newConfigObjectNode.put(DATA_TO_FETCH_PROPERTY_NAME, DataToFetch.LATEST_TELEMETRY.name());
        } else if ("false".equals(value)) {
            newConfigObjectNode.remove(OLD_DATA_TO_FETCH_PROPERTY_NAME);
            newConfigObjectNode.put(DATA_TO_FETCH_PROPERTY_NAME, DataToFetch.ATTRIBUTES.name());
        } else {
            throw new TbNodeException("property to update: '" + OLD_DATA_TO_FETCH_PROPERTY_NAME + "' has unexpected value: " + value + ". Allowed values: true or false!");
        }
        newConfigObjectNode.put(FETCH_TO_PROPERTY_NAME, TbMsgSource.METADATA.name());
        return new TbPair<>(true, newConfigObjectNode);
    }

    /*
     * 本类总结：`TbAbstractGetEntityDataNode` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
