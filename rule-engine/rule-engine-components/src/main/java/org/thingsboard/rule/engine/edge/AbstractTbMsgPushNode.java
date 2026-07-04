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
package org.thingsboard.rule.engine.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.msg.TbMsgType.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.CONNECT_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.DISCONNECT_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.TIMESERIES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.TO_SERVER_RPC_REQUEST;

@Slf4j
/**
 * Edge/Cloud 推送节点的抽象基类，负责把支持的 Rule Engine 消息转换为事件语义。
 * 本类本身不直接访问数据库、缓存或外部网络；具体保存、通知和远端同步由子类及其调用链实现。
 */
public abstract class AbstractTbMsgPushNode<T extends BaseTbMsgPushNodeConfiguration, S, U> implements TbNode {

    /**
     * 推送节点配置，主要提供属性 scope 默认值。
     */
    protected T config;

    /**
     * 属性 scope 元数据键名。
     */
    private static final String SCOPE = "scope";

    /**
     * 初始化推送节点配置。
     * 本方法不直接访问数据库或缓存，也不执行 Edge/Cloud 事件持久化。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, getConfigClazz());
    }

    /**
     * 过滤来自反向同步源的消息，并校验消息类型后交给子类处理。
     * 本方法本身不直接保存事件；不支持的消息会走 Failure，忽略来源的消息会直接 ack。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (getIgnoredMessageSource().equalsIgnoreCase(msg.getMetaData().getValue(DataConstants.MSG_SOURCE_KEY))) {
            log.debug("Ignoring msg from the {}, msg [{}]", getIgnoredMessageSource(), msg);
            ctx.ack(msg);
            return;
        }
        if (isSupportedMsgType(msg)) {
            processMsg(ctx, msg);
        } else {
            String errMsg = String.format("Unsupported msg type %s", msg.getType());
            log.debug(errMsg);
            ctx.tellFailure(msg, new RuntimeException(errMsg));
        }
    }

    /**
     * 根据消息类型构建子类事件对象。
     * 本方法只做本地数据转换，不直接写数据库或发送到 Edge/Cloud；实际持久化由 processMsg 子类实现。
     */
    protected S buildEvent(TbMsg msg, TbContext ctx) {
        if (msg.isTypeOf(ALARM)) {
            EdgeEventActionType actionType = getAlarmActionType(msg);
            return buildEvent(ctx.getTenantId(), actionType, getUUIDFromMsgData(msg), getAlarmEventType(), null);
        } else {
            Map<String, String> metadata = msg.getMetaData().getData();
            EdgeEventActionType actionType = getEdgeEventActionTypeByMsgType(msg);
            Map<String, Object> entityBody = new HashMap<>();
            JsonNode dataJson = JacksonUtil.toJsonNode(msg.getData());
            switch (actionType) {
                case ATTRIBUTES_UPDATED:
                case POST_ATTRIBUTES:
                    // 属性更新事件保留 kv 和 scope，POST_ATTRIBUTES 额外标记来源语义。
                    entityBody.put("kv", dataJson);
                    entityBody.put(SCOPE, getScope(metadata));
                    if (EdgeEventActionType.POST_ATTRIBUTES.equals(actionType)) {
                        entityBody.put("isPostAttributes", true);
                    }
                    break;
                case ATTRIBUTES_DELETED:
                    // 删除属性事件只需要属性键集合和 scope。
                    List<String> keys = JacksonUtil.convertValue(dataJson.get("attributes"), new TypeReference<>() {
                    });
                    entityBody.put("keys", keys);
                    entityBody.put(SCOPE, getScope(metadata));
                    break;
                case TIMESERIES_UPDATED:
                    // 时序事件保留数据体和消息元数据时间戳。
                    entityBody.put("data", dataJson);
                    entityBody.put("ts", msg.getMetaDataTs());
                    break;
            }
            return buildEvent(ctx.getTenantId(),
                    actionType,
                    msg.getOriginator().getId(),
                    getEventTypeByEntityType(msg.getOriginator().getEntityType()),
                    JacksonUtil.valueToTree(entityBody));
        }
    }

    /**
     * 根据告警元数据判断 EdgeEventActionType。
     * 本方法只读取消息元数据，不直接访问数据库、缓存或外部系统。
     */
    private static EdgeEventActionType getAlarmActionType(TbMsg msg) {
        boolean isNewAlarm = Boolean.parseBoolean(msg.getMetaData().getValue(DataConstants.IS_NEW_ALARM));
        boolean isClearedAlarm = Boolean.parseBoolean(msg.getMetaData().getValue(DataConstants.IS_CLEARED_ALARM));
        EdgeEventActionType eventAction;
        if (isNewAlarm) {
            eventAction = EdgeEventActionType.ADDED;
        } else if (isClearedAlarm) {
            eventAction = EdgeEventActionType.ALARM_CLEAR;
        } else {
            eventAction = EdgeEventActionType.UPDATED;
        }
        return eventAction;
    }

    /**
     * 由子类构造具体事件对象。
     * 本方法声明本身不直接涉及数据库或缓存；子类实现可能只是对象构造或进一步交给服务保存。
     */
    abstract S buildEvent(TenantId tenantId, EdgeEventActionType eventAction, UUID entityId, U eventType, JsonNode entityBody);

    /**
     * 由子类把实体类型映射为目标事件类型。
     */
    abstract U getEventTypeByEntityType(EntityType entityType);

    /**
     * 返回告警消息对应的事件类型。
     */
    abstract U getAlarmEventType();

    /**
     * 返回应忽略的消息来源标识，防止 Edge/Cloud 双向同步形成回环。
     */
    abstract String getIgnoredMessageSource();

    /**
     * 返回当前节点配置类，用于通用 init 转换。
     */
    abstract protected Class<T> getConfigClazz();

    /**
     * 子类处理已校验消息，通常负责保存事件并安排成功/失败路由。
     */
    abstract void processMsg(TbContext ctx, TbMsg msg);

    /**
     * 从告警消息体中解析告警 UUID。
     * 本方法只解析本地 JSON，不直接访问数据库或缓存。
     */
    protected UUID getUUIDFromMsgData(TbMsg msg) {
        Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
        return alarm != null ? alarm.getUuidId() : null;
    }

    /**
     * 获取属性 scope，优先使用消息元数据，缺省时使用节点配置。
     * 本方法不直接访问数据库、缓存或外部系统。
     */
    protected String getScope(Map<String, String> metadata) {
        String scope = metadata.get(SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = config.getScope();
        }
        return scope;
    }

    /**
     * 将 Rule Engine 消息类型映射为 EdgeEventActionType。
     * 连接/活动事件根据 scope 判断写入时序还是属性；不支持类型会抛出异常。
     */
    protected EdgeEventActionType getEdgeEventActionTypeByMsgType(TbMsg msg) {
        EdgeEventActionType actionType;
        if (msg.isTypeOneOf(POST_TELEMETRY_REQUEST, TIMESERIES_UPDATED)) {
            actionType = EdgeEventActionType.TIMESERIES_UPDATED;
        } else if (msg.isTypeOf(ATTRIBUTES_UPDATED)) {
            actionType = EdgeEventActionType.ATTRIBUTES_UPDATED;
        } else if (msg.isTypeOf(POST_ATTRIBUTES_REQUEST)) {
            actionType = EdgeEventActionType.POST_ATTRIBUTES;
        } else if (msg.isTypeOf(ATTRIBUTES_DELETED)) {
            actionType = EdgeEventActionType.ATTRIBUTES_DELETED;
        } else if (msg.isTypeOneOf(CONNECT_EVENT, DISCONNECT_EVENT, ACTIVITY_EVENT, INACTIVITY_EVENT)) {
            String scope = msg.getMetaData().getValue(SCOPE);
            actionType = StringUtils.isEmpty(scope) ?
                    EdgeEventActionType.TIMESERIES_UPDATED : EdgeEventActionType.ATTRIBUTES_UPDATED;
        } else {
            String type = msg.getType();
            log.warn("Unsupported msg type [{}]", type);
            throw new IllegalArgumentException("Unsupported msg type: " + type);
        }
        return actionType;
    }

    /**
     * 判断消息类型是否可被推送节点处理。
     * 本方法只做本地类型判断，不直接涉及数据库、缓存或外部调用。
     */
    protected boolean isSupportedMsgType(TbMsg msg) {
        return msg.isTypeOneOf(POST_TELEMETRY_REQUEST, POST_ATTRIBUTES_REQUEST, ATTRIBUTES_UPDATED, ATTRIBUTES_DELETED, TIMESERIES_UPDATED,
                ALARM, CONNECT_EVENT, DISCONNECT_EVENT, ACTIVITY_EVENT, INACTIVITY_EVENT, TO_SERVER_RPC_REQUEST);
    }
}

/*
 * 本类总结：
 * 本类提供 Edge/Cloud 推送节点的通用消息过滤、事件构造和类型映射逻辑。
 * 它本身不直接进行数据库持久化、缓存访问或远端推送；这些边界由具体子类或服务调用链实现。
 */
