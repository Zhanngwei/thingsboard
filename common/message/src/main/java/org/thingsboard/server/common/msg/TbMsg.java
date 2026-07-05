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
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by ashvayka on 13.01.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`TbMsg` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Data
@Slf4j
public final class TbMsg implements Serializable {

    /**
     * JSON常量，用于统一引用固定值。
     */
    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final String EMPTY_JSON_ARRAY = "[]";
    /**
     * `EMPTY_STRING`常量，用于统一引用固定值。
     */
    public static final String EMPTY_STRING = "";

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    private final String queueName;
    private final UUID id;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final long ts;
    private final String type;
    /**
     * 类型，用于区分不同处理分支。
     */
    private final TbMsgType internalType;
    private final EntityId originator;
    /**
     * 客户ID，用于定位对应业务对象。
     */
    private final CustomerId customerId;
    private final TbMsgMetaData metaData;
    /**
     * 数据，用于区分不同处理分支。
     */
    private final TbMsgDataType dataType;
    private final String data;
    /**
     * 规则链ID，用于定位对应业务对象。
     */
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;
    @Getter(value = AccessLevel.NONE)
    @JsonIgnore
    //This field is not serialized because we use queues and there is no need to do it
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final TbMsgProcessingCtx ctx;

    //This field is not serialized because we use queues and there is no need to do it
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    @JsonIgnore
    transient private final TbMsgCallback callback;

    /**
     * 功能：获取规则节点。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getAndIncrementRuleNodeCounter() {
        return ctx.getAndIncrementRuleNodeCounter();
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return newMsg(queueName, type, originator, null, metaData, data, ruleChainId, ruleNodeId);
    }

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, CustomerId, TbMsgMetaData, String, RuleChainId, RuleNodeId)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param customerId  the ID of the customer associated with the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @param ruleChainId the ID of the rule chain associated with the message
     * @param ruleNodeId  the ID of the rule node associated with the message
     * @return new TbMsg instance
     */
    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(type, originator, null, metaData, data);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return newMsg(queueName, type, originator, null, metaData, data, ruleChainId, ruleNodeId);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(type, originator, null, metaData, data);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `data`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data, long ts) {
        return new TbMsg(null, UUID.randomUUID(), ts, type, originator, null,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    // REALLY NEW MSG

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     */
    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, CustomerId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param customerId  the ID of the customer associated with the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     */
    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), dataType, data, null, null, null, TbMsgCallback.EMPTY);
    }

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(TbMsgType, EntityId, TbMsgMetaData, TbMsgDataType, String)}
     * method instead.</p>
     *
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param metaData    the metadata of the message
     * @param dataType    the dataType of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     */
    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `dataType`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return newMsg(type, originator, null, metaData, dataType, data);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `customerId`：客户IDID。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), dataType, data, null, null, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `dataType`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return newMsg(type, originator, null, metaData, dataType, data);
    }

    // For Tests only

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `dataType`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, null,
                metaData.copy(), dataType, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `data`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data, TbMsgCallback callback) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, null,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, callback);
    }

    /**
     * Transforms an existing TbMsg instance by changing its message type, originator, metadata, and data.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #transformMsg(TbMsg, TbMsgType, EntityId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     *
     * @param tbMsg      the TbMsg instance to transform
     * @param type       the new message type
     * @param originator the new originator
     * @param metaData   the new metadata
     * @param data       the new data
     * @return the transformed TbMsg instance
     */
    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg transformMsg(TbMsg tbMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, null, type, originator, tbMsg.customerId, metaData.copy(), tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.callback);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `dataType`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, null,
                metaData.copy(), dataType, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - `data`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data, TbMsgCallback callback) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, null,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, callback);
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `type`：类型。
     * - `originator`：`originator` 参数。
     * - `metaData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static TbMsg transformMsg(TbMsg tbMsg, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, type, originator, tbMsg.customerId, metaData.copy(), tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.callback);
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `originatorId`：`originatorId`ID。
     * 返回：处理结果。
     */
    public static TbMsg transformMsgOriginator(TbMsg tbMsg, EntityId originatorId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, originatorId, tbMsg.getCustomerId(), tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    public static TbMsg transformMsgData(TbMsg tbMsg, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `metadata`：待处理数据。
     * 返回：处理结果。
     */
    public static TbMsg transformMsgMetadata(TbMsg tbMsg, TbMsgMetaData metadata) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, metadata.copy(), tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `metadata`：待处理数据。
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    public static TbMsg transformMsg(TbMsg tbMsg, TbMsgMetaData metadata, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, metadata, tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    /**
     * 功能：转换客户ID。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    public static TbMsg transformMsgCustomerId(TbMsg tbMsg, CustomerId customerId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    /**
     * 功能：转换规则链。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    public static TbMsg transformMsgRuleChainId(TbMsg tbMsg, RuleChainId ruleChainId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, ruleChainId, null, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    /**
     * 功能：转换队列名称。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * 返回：处理结果。
     */
    public static TbMsg transformMsgQueueName(TbMsg tbMsg, String queueName) {
        return new TbMsg(queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.getRuleChainId(), null, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `ruleChainId`：规则链ID。
     * - `queueName`：队列名称或队列对象。
     * 返回：处理结果。
     */
    public static TbMsg transformMsg(TbMsg tbMsg, RuleChainId ruleChainId, String queueName) {
        return new TbMsg(queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, ruleChainId, null, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    //used for enqueueForTellNext
    /**
     * 功能：执行 `newMsg` 对应的处理。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `queueName`：队列名称或队列对象。
     * - `ruleChainId`：规则链ID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    public static TbMsg newMsg(TbMsg tbMsg, String queueName, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), tbMsg.getTs(), tbMsg.getInternalType(), tbMsg.getType(), tbMsg.getOriginator(), tbMsg.customerId, tbMsg.getMetaData().copy(),
                tbMsg.getDataType(), tbMsg.getData(), ruleChainId, ruleNodeId, tbMsg.ctx.copy(), TbMsgCallback.EMPTY);
    }

    /**
     * 功能：创建 `TbMsg` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `id`：`id`ID。
     * - `ts`：时间戳。
     * - `internalType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    private TbMsg(String queueName, UUID id, long ts, TbMsgType internalType, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, TbMsgProcessingCtx ctx, TbMsgCallback callback) {
        this(queueName, id, ts, internalType, internalType.name(), originator, customerId, metaData, dataType, data, ruleChainId, ruleNodeId, ctx, callback);
    }

    /**
     * 功能：创建 `TbMsg` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `id`：`id`ID。
     * - `ts`：时间戳。
     * - `internalType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    private TbMsg(String queueName, UUID id, long ts, TbMsgType internalType, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, TbMsgProcessingCtx ctx, TbMsgCallback callback) {
        this.id = id;
        this.queueName = queueName;
        if (ts > 0) {
            this.ts = ts;
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.type = type;
        this.internalType = internalType != null ? internalType : getInternalType(type);
        this.originator = originator;
        if (customerId == null || customerId.isNullUid()) {
            if (originator != null && originator.getEntityType() == EntityType.CUSTOMER) {
                this.customerId = new CustomerId(originator.getId());
            } else {
                this.customerId = null;
            }
        } else {
            this.customerId = customerId;
        }
        this.metaData = metaData;
        this.dataType = dataType;
        this.data = data;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
        this.ctx = ctx != null ? ctx : new TbMsgProcessingCtx();
        this.callback = Objects.requireNonNullElse(callback, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：执行 `toByteString` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public static ByteString toByteString(TbMsg msg) {
        return ByteString.copyFrom(toByteArray(msg));
    }

    /**
     * 功能：执行 `toByteArray` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    public static byte[] toByteArray(TbMsg msg) {
        MsgProtos.TbMsgProto.Builder builder = MsgProtos.TbMsgProto.newBuilder();
        builder.setId(msg.getId().toString());
        builder.setTs(msg.getTs());
        builder.setType(msg.getType());
        builder.setEntityType(msg.getOriginator().getEntityType().name());
        builder.setEntityIdMSB(msg.getOriginator().getId().getMostSignificantBits());
        builder.setEntityIdLSB(msg.getOriginator().getId().getLeastSignificantBits());

        if (msg.getCustomerId() != null) {
            builder.setCustomerIdMSB(msg.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(msg.getCustomerId().getId().getLeastSignificantBits());
        }

        if (msg.getRuleChainId() != null) {
            builder.setRuleChainIdMSB(msg.getRuleChainId().getId().getMostSignificantBits());
            builder.setRuleChainIdLSB(msg.getRuleChainId().getId().getLeastSignificantBits());
        }

        if (msg.getRuleNodeId() != null) {
            builder.setRuleNodeIdMSB(msg.getRuleNodeId().getId().getMostSignificantBits());
            builder.setRuleNodeIdLSB(msg.getRuleNodeId().getId().getLeastSignificantBits());
        }

        if (msg.getMetaData() != null) {
            builder.setMetaData(MsgProtos.TbMsgMetaDataProto.newBuilder().putAllData(msg.getMetaData().getData()).build());
        }

        builder.setDataType(msg.getDataType().ordinal());
        builder.setData(msg.getData());

        builder.setCtx(msg.ctx.toProto());
        return builder.build().toByteArray();
    }

    /**
     * 功能：执行 `fromBytes` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `data`：待处理数据。
     * - `callback`：处理完成后的回调。
     * 返回：处理结果。
     */
    public static TbMsg fromBytes(String queueName, byte[] data, TbMsgCallback callback) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(data);
            TbMsgMetaData metaData = new TbMsgMetaData(proto.getMetaData().getDataMap());
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            CustomerId customerId = null;
            RuleChainId ruleChainId = null;
            RuleNodeId ruleNodeId = null;
            if (proto.getCustomerIdMSB() != 0L && proto.getCustomerIdLSB() != 0L) {
                customerId = new CustomerId(new UUID(proto.getCustomerIdMSB(), proto.getCustomerIdLSB()));
            }
            if (proto.getRuleChainIdMSB() != 0L && proto.getRuleChainIdLSB() != 0L) {
                ruleChainId = new RuleChainId(new UUID(proto.getRuleChainIdMSB(), proto.getRuleChainIdLSB()));
            }
            if (proto.getRuleNodeIdMSB() != 0L && proto.getRuleNodeIdLSB() != 0L) {
                ruleNodeId = new RuleNodeId(new UUID(proto.getRuleNodeIdMSB(), proto.getRuleNodeIdLSB()));
            }

            TbMsgProcessingCtx ctx;
            if (proto.hasCtx()) {
                ctx = TbMsgProcessingCtx.fromProto(proto.getCtx());
            } else {
                // Backward compatibility with unprocessed messages fetched from queue after update.
                ctx = new TbMsgProcessingCtx(proto.getRuleNodeExecCounter());
            }

            TbMsgDataType dataType = TbMsgDataType.values()[proto.getDataType()];
            return new TbMsg(queueName, UUID.fromString(proto.getId()), proto.getTs(), null, proto.getType(), entityId, customerId,
                    metaData, dataType, proto.getData(), ruleChainId, ruleNodeId, ctx, callback);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Could not parse protobuf for TbMsg", e);
        }
    }

    /**
     * 功能：执行 `copyWithRuleChainId` 对应的处理。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    public TbMsg copyWithRuleChainId(RuleChainId ruleChainId) {
        return copyWithRuleChainId(ruleChainId, this.id);
    }

    /**
     * 功能：执行 `copyWithRuleChainId` 对应的处理。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `msgId`：消息ID。
     * 返回：处理结果。
     */
    public TbMsg copyWithRuleChainId(RuleChainId ruleChainId, UUID msgId) {
        return new TbMsg(this.queueName, msgId, this.ts, this.internalType, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, null, this.ctx, callback);
    }

    /**
     * 功能：执行 `copyWithRuleNodeId` 对应的处理。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `ruleNodeId`：规则节点ID。
     * - `msgId`：消息ID。
     * 返回：处理结果。
     */
    public TbMsg copyWithRuleNodeId(RuleChainId ruleChainId, RuleNodeId ruleNodeId, UUID msgId) {
        return new TbMsg(this.queueName, msgId, this.ts, this.internalType, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, ruleNodeId, this.ctx, callback);
    }

    /**
     * 功能：执行 `copyWithNewCtx` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbMsg copyWithNewCtx() {
        return new TbMsg(this.queueName, this.id, this.ts, this.internalType, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, ruleNodeId, this.ctx.copy(), TbMsgCallback.EMPTY);
    }

    /**
     * 功能：获取回调。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbMsgCallback getCallback() {
        // May be null in case of deserialization;
        return Objects.requireNonNullElse(callback, TbMsgCallback.EMPTY);
    }

    /**
     * 功能：发送或提交`To Stack`。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    public void pushToStack(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        ctx.push(ruleChainId, ruleNodeId);
    }

    /**
     * 功能：执行 `popFormStack` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbMsgProcessingStackItem popFormStack() {
        return ctx.pop();
    }

    /**
     * Checks if the message is still valid for processing. May be invalid if the message pack is timed-out or canceled.
     * @return 'true' if message is valid for processing, 'false' otherwise.
     */
    /**
     * 功能：判断`Valid`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isValid() {
        return getCallback().isMsgValid();
    }

    /**
     * 功能：获取时间戳。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getMetaDataTs() {
        String tsStr = metaData.getValue("ts");
        if (!StringUtils.isEmpty(tsStr)) {
            try {
                return Long.parseLong(tsStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return ts;
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    private TbMsgType getInternalType(String type) {
        try {
            return TbMsgType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return TbMsgType.NA;
        }
    }

    /**
     * 功能：判断类型。
     * 参数：
     * - `tbMsgType`：待处理消息。
     * 返回：判断结果。
     */
    public boolean isTypeOf(TbMsgType tbMsgType) {
        return internalType.equals(tbMsgType);
    }

    /**
     * 功能：判断类型。
     * 参数：
     * - `types`：类型。
     * 返回：判断结果。
     */
    public boolean isTypeOneOf(TbMsgType... types) {
        for (TbMsgType type : types) {
            if (isTypeOf(type)) {
                return true;
            }
        }
        return false;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsg` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
