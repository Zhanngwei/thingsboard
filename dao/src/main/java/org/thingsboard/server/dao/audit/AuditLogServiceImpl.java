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
package org.thingsboard.server.dao.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.audit.sink.AuditLogSink;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. 类目的：`AuditLogServiceImpl` 是 ThingsBoard DAO 模块 中的审计与事件持久化类型，用于记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 生命周期：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Observer / Event Sourcing / Repository。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
public class AuditLogServiceImpl implements AuditLogService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    /**
     * `auditLogLevelFilter` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private AuditLogLevelFilter auditLogLevelFilter;

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private AuditLogDao auditLogDao;

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityService entityService;

    /**
     * `auditLogSink` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private AuditLogSink auditLogSink;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    private JpaExecutorService executor;

    /**
     * 校验器，封装可复用的处理规则。
     */
    @Autowired
    private DataValidator<AuditLog> auditLogValidator;

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `actionTypes`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndCustomerId [{}], [{}], [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        return auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId.getId(), customerId, actionTypes, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `actionTypes`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndUserId(TenantId tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndUserId [{}], [{}], [{}]", tenantId, userId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userId, "Incorrect userId" + userId);
        return auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId.getId(), userId, actionTypes, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `actionTypes`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndEntityId [{}], [{}], [{}]", tenantId, entityId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityId(entityId, INCORRECT_TENANT_ID + entityId);
        return auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId.getId(), entityId, actionTypes, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `actionTypes`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AuditLog> findAuditLogsByTenantId(TenantId tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogs [{}]", pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return auditLogDao.findAuditLogsByTenantId(tenantId.getId(), actionTypes, pageLink);
    }

    @Override
    public <E extends HasName, I extends EntityId> ListenableFuture<Void>
    logEntityAction(TenantId tenantId, CustomerId customerId, UserId userId, String userName, I entityId, E entity,
                    ActionType actionType, Exception e, Object... additionalInfo) {
        if (canLog(entityId.getEntityType(), actionType)) {
            JsonNode actionData = constructActionData(entityId, entity, actionType, additionalInfo);
            ActionStatus actionStatus = ActionStatus.SUCCESS;
            String failureDetails = "";
            String entityName = "N/A";
            if (entity != null) {
                entityName = entity.getName();
            } else {
                try {
                    entityName = entityService.fetchEntityName(tenantId, entityId).orElse(entityName);
                } catch (Exception ignored) {
                }
            }
            if (e != null) {
                actionStatus = ActionStatus.FAILURE;
                failureDetails = getFailureStack(e);
            }
            if (actionType == ActionType.RPC_CALL) {
                String rpcErrorString = extractParameter(String.class, additionalInfo);
                if (!StringUtils.isEmpty(rpcErrorString)) {
                    actionStatus = ActionStatus.FAILURE;
                    failureDetails = rpcErrorString;
                }
            }
            return logAction(tenantId,
                    entityId,
                    entityName,
                    customerId,
                    userId,
                    userName,
                    actionType,
                    actionData,
                    actionStatus,
                    failureDetails);
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `constructActionData` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entity`：实体对象。
     * - `actionType`：类型。
     * - `additionalInfo`：`additionalInfo` 参数。
     * 返回：处理结果。
     */
    private <E extends HasName, I extends EntityId> JsonNode constructActionData(I entityId, E entity,
                                                                                 ActionType actionType,
                                                                                 Object... additionalInfo) {
        ObjectNode actionData = JacksonUtil.newObjectNode();
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
            case ALARM_ASSIGNED:
            case ALARM_UNASSIGNED:
            case RELATIONS_DELETED:
            case ASSIGNED_TO_TENANT:
                if (entity != null) {
                    ObjectNode entityNode = (ObjectNode) JacksonUtil.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                    actionData.set("entity", entityNode);
                }
                if (entityId.getEntityType() == EntityType.RULE_CHAIN) {
                    RuleChainMetaData ruleChainMetaData = extractParameter(RuleChainMetaData.class, additionalInfo);
                    if (ruleChainMetaData != null) {
                        ObjectNode ruleChainMetaDataNode = (ObjectNode) JacksonUtil.valueToTree(ruleChainMetaData);
                        actionData.set("metadata", ruleChainMetaDataNode);
                    }
                }
                break;
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
            case DELETED_COMMENT:
                AlarmComment comment = extractParameter(AlarmComment.class, additionalInfo);
                actionData.set("comment", comment.getComment());
                break;
            case ALARM_DELETE:
            case DELETED:
            case ACTIVATED:
            case SUSPENDED:
            case CREDENTIALS_READ:
                String strEntityId = extractParameter(String.class, additionalInfo);
                actionData.put("entityId", strEntityId);
                break;
            case ATTRIBUTES_UPDATED:
                actionData.put("entityId", entityId.toString());
                String scope = extractParameter(String.class, 0, additionalInfo);
                @SuppressWarnings("unchecked")
                List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                actionData.put("scope", scope);
                ObjectNode attrsNode = JacksonUtil.newObjectNode();
                if (attributes != null) {
                    for (AttributeKvEntry attr : attributes) {
                        attrsNode.put(attr.getKey(), attr.getValueAsString());
                    }
                }
                actionData.set("attributes", attrsNode);
                break;
            case ATTRIBUTES_DELETED:
            case ATTRIBUTES_READ:
                actionData.put("entityId", entityId.toString());
                scope = extractParameter(String.class, 0, additionalInfo);
                actionData.put("scope", scope);
                @SuppressWarnings("unchecked")
                List<String> keys = extractParameter(List.class, 1, additionalInfo);
                ArrayNode attrsArrayNode = actionData.putArray("attributes");
                if (keys != null) {
                    keys.forEach(attrsArrayNode::add);
                }
                break;
            case RPC_CALL:
                actionData.put("entityId", entityId.toString());
                Boolean oneWay = extractParameter(Boolean.class, 1, additionalInfo);
                String method = extractParameter(String.class, 2, additionalInfo);
                String params = extractParameter(String.class, 3, additionalInfo);
                actionData.put("oneWay", oneWay);
                actionData.put("method", method);
                actionData.put("params", params);
                break;
            case CREDENTIALS_UPDATED:
                actionData.put("entityId", entityId.toString());
                DeviceCredentials deviceCredentials = extractParameter(DeviceCredentials.class, additionalInfo);
                actionData.set("credentials", JacksonUtil.valueToTree(deviceCredentials));
                break;
            case ASSIGNED_TO_CUSTOMER:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("assignedCustomerId", strCustomerId);
                actionData.put("assignedCustomerName", strCustomerName);
                break;
            case UNASSIGNED_FROM_CUSTOMER:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                strCustomerId = extractParameter(String.class, 1, additionalInfo);
                strCustomerName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("unassignedCustomerId", strCustomerId);
                actionData.put("unassignedCustomerName", strCustomerName);
                break;
            case RELATION_ADD_OR_UPDATE:
            case RELATION_DELETED:
                EntityRelation relation = extractParameter(EntityRelation.class, 0, additionalInfo);
                actionData.set("relation", JacksonUtil.valueToTree(relation));
                break;
            case LOGIN:
            case LOGOUT:
            case LOCKOUT:
                String clientAddress = extractParameter(String.class, 0, additionalInfo);
                String browser = extractParameter(String.class, 1, additionalInfo);
                String os = extractParameter(String.class, 2, additionalInfo);
                String device = extractParameter(String.class, 3, additionalInfo);
                String provider = extractParameter(String.class, 4, additionalInfo);
                actionData.put("clientAddress", clientAddress);
                actionData.put("browser", browser);
                actionData.put("os", os);
                actionData.put("device", device);
                if (StringUtils.hasText(provider)) {
                    actionData.put("provider", provider);
                }
                break;
            case PROVISION_SUCCESS:
            case PROVISION_FAILURE:
                ProvisionRequest request = extractParameter(ProvisionRequest.class, additionalInfo);
                if (request != null) {
                    actionData.set("provisionRequest", JacksonUtil.valueToTree(request));
                }
                break;
            case TIMESERIES_UPDATED:
                actionData.put("entityId", entityId.toString());
                @SuppressWarnings("unchecked")
                List<TsKvEntry> updatedTimeseries = extractParameter(List.class, 0, additionalInfo);
                if (updatedTimeseries != null) {
                    ArrayNode result = actionData.putArray("timeseries");
                    updatedTimeseries.stream()
                            .collect(Collectors.groupingBy(TsKvEntry::getTs))
                            .forEach((k, v) -> {
                                ObjectNode element = JacksonUtil.newObjectNode();
                                element.put("ts", k);
                                ObjectNode values = element.putObject("values");
                                v.forEach(kvEntry -> values.put(kvEntry.getKey(), kvEntry.getValueAsString()));
                                result.add(element);
                            });
                }
                break;
            case TIMESERIES_DELETED:
                actionData.put("entityId", entityId.toString());
                @SuppressWarnings("unchecked")
                List<String> timeseriesKeys = extractParameter(List.class, 0, additionalInfo);
                if (timeseriesKeys != null) {
                    ArrayNode timeseriesArrayNode = actionData.putArray("timeseries");
                    timeseriesKeys.forEach(timeseriesArrayNode::add);
                }
                actionData.put("startTs", extractParameter(Long.class, 1, additionalInfo));
                actionData.put("endTs", extractParameter(Long.class, 2, additionalInfo));
                break;
            case ASSIGNED_TO_EDGE:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                String strEdgeId = extractParameter(String.class, 1, additionalInfo);
                String strEdgeName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("assignedEdgeId", strEdgeId);
                actionData.put("assignedEdgeName", strEdgeName);
                break;
            case UNASSIGNED_FROM_EDGE:
                strEntityId = extractParameter(String.class, 0, additionalInfo);
                strEdgeId = extractParameter(String.class, 1, additionalInfo);
                strEdgeName = extractParameter(String.class, 2, additionalInfo);
                actionData.put("entityId", strEntityId);
                actionData.put("unassignedEdgeId", strEdgeId);
                actionData.put("unassignedEdgeName", strEdgeName);
                break;
            case SMS_SENT:
                String number = extractParameter(String.class, 0, additionalInfo);
                actionData.put("recipientNumber", number);
                break;
        }
        return actionData;
    }

    /**
     * 功能：执行 `extractParameter` 对应的处理。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * - `additionalInfo`：`additionalInfo` 参数。
     * 返回：处理结果。
     */
    private <T> T extractParameter(Class<T> clazz, Object... additionalInfo) {
        return extractParameter(clazz, 0, additionalInfo);
    }

    /**
     * 功能：执行 `extractParameter` 对应的处理。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * - `index`：`index` 参数。
     * - `additionalInfo`：`additionalInfo` 参数。
     * 返回：处理结果。
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
     * 功能：获取失败信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：文本结果。
     */
    private String getFailureStack(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * 功能：执行 `canLog` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * - `actionType`：类型。
     * 返回：判断结果。
     */
    private boolean canLog(EntityType entityType, ActionType actionType) {
        return auditLogLevelFilter.logEnabled(entityType, actionType);
    }

    /**
     * 功能：保存或创建`Audit Log Entry`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `entityName`：实体对象。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private AuditLog createAuditLogEntry(TenantId tenantId,
                                         EntityId entityId,
                                         String entityName,
                                         CustomerId customerId,
                                         UserId userId,
                                         String userName,
                                         ActionType actionType,
                                         JsonNode actionData,
                                         ActionStatus actionStatus,
                                         String actionFailureDetails) {
        AuditLog result = new AuditLog();
        result.setTenantId(tenantId);
        result.setEntityId(entityId);
        result.setEntityName(entityName);
        result.setCustomerId(customerId);
        result.setUserId(userId);
        result.setUserName(userName);
        result.setActionType(actionType);
        result.setActionData(actionData);
        result.setActionStatus(actionStatus);
        result.setActionFailureDetails(actionFailureDetails);
        return result;
    }

    /**
     * 功能：执行 `logAction` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `entityName`：实体对象。
     * - `customerId`：客户IDID。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> logAction(TenantId tenantId,
                                             EntityId entityId,
                                             String entityName,
                                             CustomerId customerId,
                                             UserId userId,
                                             String userName,
                                             ActionType actionType,
                                             JsonNode actionData,
                                             ActionStatus actionStatus,
                                             String actionFailureDetails) {
        AuditLog auditLogEntry = createAuditLogEntry(tenantId, entityId, entityName, customerId, userId, userName,
                actionType, actionData, actionStatus, actionFailureDetails);
        log.trace("Executing logAction [{}]", auditLogEntry);
        try {
            auditLogValidator.validate(auditLogEntry, AuditLog::getTenantId);
        } catch (Exception e) {
            if (StringUtils.contains(e.getMessage(), "is malformed")) {
                auditLogEntry.setEntityName("MALFORMED");
            } else {
                return Futures.immediateFailedFuture(e);
            }
        }

        return executor.submit(() -> {
            AuditLog auditLog = auditLogDao.save(tenantId, auditLogEntry);
            auditLogSink.logAction(auditLog);
            return null;
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AuditLogServiceImpl` 在 ThingsBoard DAO 模块 中承担审计与事件持久化类型职责，核心目的是记录用户操作、系统事件、实体事件和事件溯源数据，支持查询、清理和异步下沉。
 * 2. 核心流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
 * 3. 关键依赖：主要依赖或协作对象包括AuditLogService、EventService、HouseKeeper、DAO、队列和外部审计 Sink。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
