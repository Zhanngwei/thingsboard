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
package org.thingsboard.server.dao.sql.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AlarmDataAdapter` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class AlarmDataAdapter {

    /**
     * 方法说明：
     * 1. 职责：执行 `createAlarmData` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public static PageData<AlarmData> createAlarmData(EntityDataPageLink pageLink,
                                                      List<Map<String, Object>> rows,
                                                      int totalElements, Collection<EntityId> orderedEntityIds) {
        Map<UUID, EntityId> entityIdMap = orderedEntityIds.stream().collect(Collectors.toMap(EntityId::getId, Function.identity()));
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
        List<AlarmData> entitiesData = convertListToAlarmData(rows, entityIdMap);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertListToAlarmData` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private static List<AlarmData> convertListToAlarmData(List<Map<String, Object>> result, Map<UUID, EntityId> entityIdMap) {
        return result.stream().map(tmp -> toEntityData(tmp, entityIdMap)).collect(Collectors.toList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toEntityData` 对应的SQL/JPA 持久化实现类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private static AlarmData toEntityData(Map<String, Object> row, Map<UUID, EntityId> entityIdMap) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId((UUID) row.get(ModelConstants.ID_PROPERTY)));
        alarm.setCreatedTime((long) row.get(ModelConstants.CREATED_TIME_PROPERTY));
        // 时序数据读写量大，单独处理时间窗口、分区和聚合可以避免污染普通实体 DAO 流程。
        alarm.setAckTs((long) row.get(ModelConstants.ALARM_ACK_TS_PROPERTY));
        // 时序数据读写量大，单独处理时间窗口、分区和聚合可以避免污染普通实体 DAO 流程。
        alarm.setClearTs((long) row.get(ModelConstants.ALARM_CLEAR_TS_PROPERTY));
        // 时序数据读写量大，单独处理时间窗口、分区和聚合可以避免污染普通实体 DAO 流程。
        alarm.setAssignTs((long) row.get(ModelConstants.ALARM_ASSIGN_TS_PROPERTY));
        // 时序数据读写量大，单独处理时间窗口、分区和聚合可以避免污染普通实体 DAO 流程。
        alarm.setStartTs((long) row.get(ModelConstants.ALARM_START_TS_PROPERTY));
        // 时序数据读写量大，单独处理时间窗口、分区和聚合可以避免污染普通实体 DAO 流程。
        alarm.setEndTs((long) row.get(ModelConstants.ALARM_END_TS_PROPERTY));
        Object additionalInfo = row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (additionalInfo != null) {
            try {
                alarm.setDetails(JacksonUtil.toJsonNode(additionalInfo.toString()));
            // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
            } catch (IllegalArgumentException e) {
                log.warn("Failed to parse json: {}", row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY), e);
            }
        }
        EntityType originatorType = EntityType.values()[(int) row.get(ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY)];
        UUID originatorId = (UUID) row.get(ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY);
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        Object assigneeIdObj = row.get(ModelConstants.ASSIGNEE_ID_PROPERTY);
        String assigneeFirstName = null;
        String assigneeLastName = null;
        String assigneeEmail = null;
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (assigneeIdObj != null) {
            alarm.setAssigneeId(new UserId((UUID) row.get(ModelConstants.ALARM_ASSIGNEE_ID_PROPERTY)));
            assigneeFirstName = (String) row.get(ModelConstants.ALARM_ASSIGNEE_FIRST_NAME_PROPERTY);
            assigneeLastName = (String) row.get(ModelConstants.ALARM_ASSIGNEE_LAST_NAME_PROPERTY);
            assigneeEmail = (String) row.get(ModelConstants.ALARM_ASSIGNEE_EMAIL_PROPERTY);
        }
        alarm.setPropagate((boolean) row.get(ModelConstants.ALARM_PROPAGATE_PROPERTY));
        alarm.setPropagateToOwner((boolean) row.get(ModelConstants.ALARM_PROPAGATE_TO_OWNER_PROPERTY));
        alarm.setPropagateToTenant((boolean) row.get(ModelConstants.ALARM_PROPAGATE_TO_TENANT_PROPERTY));
        alarm.setType(row.get(ModelConstants.ALARM_TYPE_PROPERTY).toString());
        alarm.setSeverity(AlarmSeverity.valueOf(row.get(ModelConstants.ALARM_SEVERITY_PROPERTY).toString()));
        alarm.setAcknowledged((boolean) row.get(ModelConstants.ALARM_ACKNOWLEDGED_PROPERTY));
        alarm.setCleared((boolean) row.get(ModelConstants.ALARM_CLEARED_PROPERTY));
        alarm.setTenantId(TenantId.fromUUID((UUID) row.get(ModelConstants.TENANT_ID_PROPERTY)));
        Object customerIdObj = row.get(ModelConstants.CUSTOMER_ID_PROPERTY);
        CustomerId customerId = customerIdObj != null ? new CustomerId((UUID) customerIdObj) : null;
        alarm.setCustomerId(customerId);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (row.get(ModelConstants.ALARM_PROPAGATE_RELATION_TYPES) != null) {
            String propagateRelationTypes = row.get(ModelConstants.ALARM_PROPAGATE_RELATION_TYPES).toString();
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (!StringUtils.isEmpty(propagateRelationTypes)) {
                alarm.setPropagateRelationTypes(Arrays.asList(propagateRelationTypes.split(",")));
            } else {
                alarm.setPropagateRelationTypes(Collections.emptyList());
            }
        } else {
            alarm.setPropagateRelationTypes(Collections.emptyList());
        }
        UUID entityUuid = (UUID) row.get(ModelConstants.ENTITY_ID_COLUMN);
        EntityId entityId = entityIdMap.get(entityUuid);
        Object originatorNameObj = row.get(ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY);
        String originatorName = originatorNameObj != null ? originatorNameObj.toString() : null;
        Object originatorLabelObj = row.get(ModelConstants.ALARM_ORIGINATOR_LABEL_PROPERTY);
        String originatorLabel = originatorLabelObj != null ? originatorLabelObj.toString() : null;

        AlarmData alarmData = new AlarmData(alarm, entityId);
        alarmData.setOriginatorName(originatorName);
        alarmData.setOriginatorLabel(originatorLabel);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (alarm.getAssigneeId() != null) {
            alarmData.setAssignee(new AlarmAssignee(alarm.getAssigneeId(), assigneeFirstName, assigneeLastName, assigneeEmail));
        }

        return alarmData;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmDataAdapter` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
