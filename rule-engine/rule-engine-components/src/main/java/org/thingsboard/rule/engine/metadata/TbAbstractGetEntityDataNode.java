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

@Slf4j
/**
 * 中文说明：`TbAbstractGetEntityDataNode` 是抽象获取实体数据节点规则节点，用于读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`泛型或父类定义的配置对象`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public abstract class TbAbstractGetEntityDataNode<T extends EntityId> extends TbAbstractGetMappedDataNode<T, TbGetEntityDataNodeConfiguration> {

    /**
     * 常量字段：定义 `DATA_TO_FETCH_PROPERTY_NAME`，用于消息体数据，本身不触发外部系统调用。
     */
    private final static String DATA_TO_FETCH_PROPERTY_NAME = "dataToFetch";
    /**
     * 常量字段：定义 `OLD_DATA_TO_FETCH_PROPERTY_NAME`，用于消息体数据，本身不触发外部系统调用。
     */
    private static final String OLD_DATA_TO_FETCH_PROPERTY_NAME = "telemetry";
    /**
     * 字段说明：保存 `DATA_MAPPING_PROPERTY_NAME` 本地缓存、队列或并发状态，用于协调本类处理流程。
     */
    private final static String DATA_MAPPING_PROPERTY_NAME = "dataMapping";
    /**
     * 字段说明：保存 `OLD_DATA_MAPPING_PROPERTY_NAME` 本地缓存、队列或并发状态，用于协调本类处理流程。
     */
    private static final String OLD_DATA_MAPPING_PROPERTY_NAME = "attrMapping";

    /**
     * 常量字段：定义 `DATA_TO_FETCH_VALIDATION_MSG`，用于消息体数据，本身不触发外部系统调用。
     */
    private static final String DATA_TO_FETCH_VALIDATION_MSG = "DataToFetch property has invalid value: %s." +
            " Only ATTRIBUTES and LATEST_TELEMETRY values supported!";

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) {
        var msgDataAsObjectNode = TbMsgSource.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
        withCallback(findEntityAsync(ctx, msg.getOriginator()),
                entityId -> processDataAndTell(ctx, msg, entityId, msgDataAsObjectNode),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    /**
     * 方法说明：按租户、实体或关系条件查询数据，供 `TbAbstractGetEntityDataNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected abstract ListenableFuture<T> findEntityAsync(TbContext ctx, EntityId originator);

    /**
     * 方法说明：从消息或服务层获取需要补充的数据，供 `TbAbstractGetEntityDataNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected void checkDataToFetchSupportedOrElseThrow(DataToFetch dataToFetch) throws TbNodeException {
        if (dataToFetch == null || dataToFetch.equals(DataToFetch.FIELDS)) {
            throw new TbNodeException(String.format(DATA_TO_FETCH_VALIDATION_MSG, dataToFetch));
        }
    }

    /**
     * 方法说明：执行本类核心处理流程，供 `TbAbstractGetEntityDataNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
     * 方法说明：迁移旧版本规则节点 JSON 配置结构，供 `TbAbstractGetEntityDataNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
