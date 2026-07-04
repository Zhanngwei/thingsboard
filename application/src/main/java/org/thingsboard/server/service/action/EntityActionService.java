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
package org.thingsboard.server.service.action;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmAssignmentTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmCommentTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.EntitiesLimitTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionTrigger;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.audit.AuditLogService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`EntityActionService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class EntityActionService {
    /**
     * 字段说明：
     * 1. 保存 `tbClusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbClusterService tbClusterService;
    private final AuditLogService auditLogService;
    /**
     * 字段说明：
     * 1. 保存 `notificationRuleProcessor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationRuleProcessor notificationRuleProcessor;

    /**
     * 方法说明：
     * 1. 职责：执行 `pushEntityActionToRuleEngine` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void pushEntityActionToRuleEngine(EntityId entityId, HasName entity, TenantId tenantId, CustomerId customerId,
                                             ActionType actionType, User user, Object... additionalInfo) {
        Optional<TbMsgType> msgType = actionType.getRuleEngineMsgType();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (msgType.isPresent()) {
            try {
                TbMsgMetaData metaData = new TbMsgMetaData();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (user != null) {
                    metaData.putValue("userId", user.getId().toString());
                    metaData.putValue("userName", user.getName());
                    metaData.putValue("userEmail", user.getEmail());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (user.getFirstName() != null) {
                        metaData.putValue("userFirstName", user.getFirstName());
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (user.getLastName() != null) {
                        metaData.putValue("userLastName", user.getLastName());
                    }
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (customerId != null && !customerId.isNullUid()) {
                    metaData.putValue("customerId", customerId.toString());
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (actionType == ActionType.ASSIGNED_TO_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("assignedCustomerId", strCustomerId);
                    metaData.putValue("assignedCustomerName", strCustomerName);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (actionType == ActionType.UNASSIGNED_FROM_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("unassignedCustomerId", strCustomerId);
                    metaData.putValue("unassignedCustomerName", strCustomerName);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (actionType == ActionType.ASSIGNED_FROM_TENANT) {
                    String strTenantId = extractParameter(String.class, 0, additionalInfo);
                    String strTenantName = extractParameter(String.class, 1, additionalInfo);
                    metaData.putValue("assignedFromTenantId", strTenantId);
                    metaData.putValue("assignedFromTenantName", strTenantName);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (actionType == ActionType.ASSIGNED_TO_TENANT) {
                    String strTenantId = extractParameter(String.class, 0, additionalInfo);
                    String strTenantName = extractParameter(String.class, 1, additionalInfo);
                    metaData.putValue("assignedToTenantId", strTenantId);
                    metaData.putValue("assignedToTenantName", strTenantName);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (actionType == ActionType.ASSIGNED_TO_EDGE) {
                    String strEdgeId = extractParameter(String.class, 1, additionalInfo);
                    String strEdgeName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("assignedEdgeId", strEdgeId);
                    metaData.putValue("assignedEdgeName", strEdgeName);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (actionType == ActionType.UNASSIGNED_FROM_EDGE) {
                    String strEdgeId = extractParameter(String.class, 1, additionalInfo);
                    String strEdgeName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("unassignedEdgeId", strEdgeId);
                    metaData.putValue("unassignedEdgeName", strEdgeName);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (actionType == ActionType.ADDED_COMMENT || actionType == ActionType.UPDATED_COMMENT) {
                    AlarmComment comment = extractParameter(AlarmComment.class, 0, additionalInfo);
                    metaData.putValue("comment", JacksonUtil.toString(comment));
                }
                ObjectNode entityNode;
                if (entity != null) {
                    entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                    metaData.putValue("entityName", entity.getName());
                    metaData.putValue("entityType", entityId.getEntityType().toString());
                } else {
                    entityNode = JacksonUtil.newObjectNode();
                    if (actionType == ActionType.ATTRIBUTES_UPDATED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        @SuppressWarnings("unchecked")
                        List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue(DataConstants.SCOPE, scope);
                        if (attributes != null) {
                            for (AttributeKvEntry attr : attributes) {
                                JacksonUtil.addKvEntry(entityNode, attr);
                            }
                        }
                    } else if (actionType == ActionType.ATTRIBUTES_DELETED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        @SuppressWarnings("unchecked")
                        List<String> keys = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue(DataConstants.SCOPE, scope);
                        ArrayNode attrsArrayNode = entityNode.putArray("attributes");
                        if (keys != null) {
                            keys.forEach(attrsArrayNode::add);
                        }
                    } else if (actionType == ActionType.TIMESERIES_UPDATED) {
                        @SuppressWarnings("unchecked")
                        List<TsKvEntry> timeseries = extractParameter(List.class, 0, additionalInfo);
                        addTimeseries(entityNode, timeseries);
                    } else if (actionType == ActionType.TIMESERIES_DELETED) {
                        @SuppressWarnings("unchecked")
                        List<String> keys = extractParameter(List.class, 0, additionalInfo);
                        if (keys != null) {
                            ArrayNode timeseriesArrayNode = entityNode.putArray("timeseries");
                            keys.forEach(timeseriesArrayNode::add);
                        }
                        entityNode.put("startTs", extractParameter(Long.class, 1, additionalInfo));
                        entityNode.put("endTs", extractParameter(Long.class, 2, additionalInfo));
                    } else if (ActionType.RELATION_ADD_OR_UPDATE.equals(actionType) || ActionType.RELATION_DELETED.equals(actionType)) {
                        entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(extractParameter(EntityRelation.class, 0, additionalInfo));
                    }
                }

                if (tenantId == null || tenantId.isNullUid()) {
                    if (entity instanceof HasTenantId) {
                        tenantId = ((HasTenantId) entity).getTenantId();
                    }
                }
                if (tenantId != null && !tenantId.isSysTenantId()) {
                    processNotificationRules(tenantId, entityId, entity, actionType, user, additionalInfo);
                }
                TbMsg tbMsg = TbMsg.newMsg(msgType.get(), entityId, customerId, metaData, TbMsgDataType.JSON, JacksonUtil.toString(entityNode));
                tbClusterService.pushMsgToRuleEngine(tenantId, entityId, tbMsg, null);
            } catch (Exception e) {
                log.warn("[{}] Failed to push entity action to rule engine: {}", entityId, actionType, e);
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processNotificationRules` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processNotificationRules(TenantId tenantId, EntityId originatorId, HasName entity, ActionType actionType, User user, Object... additionalInfo) {
        EntityId entityId = entity instanceof HasId ? ((HasId<? extends EntityId>) entity).getId() : originatorId;
        switch (actionType) {
            case ADDED:
                notificationRuleProcessor.process(EntitiesLimitTrigger.builder()
                        .tenantId(tenantId)
                        .entityType(entityId.getEntityType())
                        .build());
            case UPDATED:
            case DELETED:
                notificationRuleProcessor.process(EntityActionTrigger.builder()
                        .tenantId(tenantId)
                        .entityId(entityId)
                        .entity(entity)
                        .actionType(actionType)
                        .user(user)
                        .build());
                break;
            case ALARM_ASSIGNED:
            case ALARM_UNASSIGNED:
                if (!(entity instanceof AlarmInfo)) { // should not normally happen
                    log.warn("Invalid alarm assignment event: entity is not instance of AlarmInfo");
                    break;
                }
                notificationRuleProcessor.process(AlarmAssignmentTrigger.builder()
                        .tenantId(tenantId)
                        .alarmInfo((AlarmInfo) entity)
                        .actionType(actionType)
                        .user(user)
                        .build());
                break;
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
                if (!(entity instanceof Alarm)) { // should not normally happen
                    log.warn("Invalid alarm comment event: entity is not instance of Alarm");
                    break;
                }
                notificationRuleProcessor.process(AlarmCommentTrigger.builder()
                        .tenantId(tenantId)
                        .comment(extractParameter(AlarmComment.class, 0, additionalInfo))
                        .alarm((Alarm) entity)
                        .actionType(actionType)
                        .user(user)
                        .build());
                break;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logEntityAction` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public <E extends HasName, I extends EntityId> void logEntityAction(User user, I entityId, E entity, CustomerId customerId,
                                                                        ActionType actionType, Exception e, Object... additionalInfo) {
        if (customerId == null || customerId.isNullUid()) {
            customerId = user.getCustomerId();
        }
        if (e == null) {
            pushEntityActionToRuleEngine(entityId, entity, user.getTenantId(), customerId, actionType, user, additionalInfo);
        }
        auditLogService.logEntityAction(user.getTenantId(), customerId, user.getId(), user.getName(), entityId, entity, actionType, e, additionalInfo);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractParameter` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T> T extractParameter(Class<T> clazz, int index, Object... additionalInfo) {
        T result = null;
        if (additionalInfo != null && additionalInfo.length > index) {
            Object paramObject = additionalInfo[index];
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addTimeseries` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addTimeseries(ObjectNode entityNode, List<TsKvEntry> timeseries) throws Exception {
        if (timeseries != null && !timeseries.isEmpty()) {
            ArrayNode result = entityNode.putArray("timeseries");
            Map<Long, List<TsKvEntry>> groupedTelemetry = timeseries.stream()
                    .collect(Collectors.groupingBy(TsKvEntry::getTs));
            for (Map.Entry<Long, List<TsKvEntry>> entry : groupedTelemetry.entrySet()) {
                ObjectNode element = JacksonUtil.newObjectNode();
                element.put("ts", entry.getKey());
                ObjectNode values = element.putObject("values");
                for (TsKvEntry tsKvEntry : entry.getValue()) {
                    JacksonUtil.addKvEntry(values, tsKvEntry);
                }
                result.add(element);
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EntityActionService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
