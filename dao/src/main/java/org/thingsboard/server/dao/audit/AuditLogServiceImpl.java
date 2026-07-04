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

@Slf4j
@Service
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
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
public class AuditLogServiceImpl implements AuditLogService {

    /**
     * 字段说明：
     * 1. 保存 `INCORRECT_TENANT_ID` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `auditLogLevelFilter` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private AuditLogLevelFilter auditLogLevelFilter;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `auditLogDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private AuditLogDao auditLogDao;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `entityService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private EntityService entityService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `auditLogSink` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private AuditLogSink auditLogSink;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `executor` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private JpaExecutorService executor;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `auditLogValidator` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DataValidator<AuditLog> auditLogValidator;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findAuditLogsByTenantIdAndCustomerId` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public PageData<AuditLog> findAuditLogsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndCustomerId [{}], [{}], [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return auditLogDao.findAuditLogsByTenantIdAndCustomerId(tenantId.getId(), customerId, actionTypes, pageLink);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findAuditLogsByTenantIdAndUserId` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public PageData<AuditLog> findAuditLogsByTenantIdAndUserId(TenantId tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndUserId [{}], [{}], [{}]", tenantId, userId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userId, "Incorrect userId" + userId);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return auditLogDao.findAuditLogsByTenantIdAndUserId(tenantId.getId(), userId, actionTypes, pageLink);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findAuditLogsByTenantIdAndEntityId` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public PageData<AuditLog> findAuditLogsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogsByTenantIdAndEntityId [{}], [{}], [{}]", tenantId, entityId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityId(entityId, INCORRECT_TENANT_ID + entityId);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return auditLogDao.findAuditLogsByTenantIdAndEntityId(tenantId.getId(), entityId, actionTypes, pageLink);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findAuditLogsByTenantId` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public PageData<AuditLog> findAuditLogsByTenantId(TenantId tenantId, List<ActionType> actionTypes, TimePageLink pageLink) {
        log.trace("Executing findAuditLogs [{}]", pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        return auditLogDao.findAuditLogsByTenantId(tenantId.getId(), actionTypes, pageLink);
    }

    @Override
    public <E extends HasName, I extends EntityId> ListenableFuture<Void>
    logEntityAction(TenantId tenantId, CustomerId customerId, UserId userId, String userName, I entityId, E entity,
                    ActionType actionType, Exception e, Object... additionalInfo) {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (canLog(entityId.getEntityType(), actionType)) {
            JsonNode actionData = constructActionData(entityId, entity, actionType, additionalInfo);
            ActionStatus actionStatus = ActionStatus.SUCCESS;
            String failureDetails = "";
            String entityName = "N/A";
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (entity != null) {
                entityName = entity.getName();
            } else {
                try {
                    entityName = entityService.fetchEntityName(tenantId, entityId).orElse(entityName);
                // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
                } catch (Exception ignored) {
                }
            }
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (e != null) {
                actionStatus = ActionStatus.FAILURE;
                failureDetails = getFailureStack(e);
            }
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (actionType == ActionType.RPC_CALL) {
                String rpcErrorString = extractParameter(String.class, additionalInfo);
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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
     * 方法说明：
     * 1. 职责：执行 `constructActionData` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private <E extends HasName, I extends EntityId> JsonNode constructActionData(I entityId, E entity,
                                                                                 ActionType actionType,
                                                                                 Object... additionalInfo) {
        ObjectNode actionData = JacksonUtil.newObjectNode();
        // 根据实体类型、查询类型或数据库方言分支，保持不同持久化路径的语义隔离。
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
            case ALARM_ASSIGNED:
            case ALARM_UNASSIGNED:
            case RELATIONS_DELETED:
            case ASSIGNED_TO_TENANT:
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                if (entity != null) {
                    ObjectNode entityNode = (ObjectNode) JacksonUtil.valueToTree(entity);
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                    actionData.set("entity", entityNode);
                }
                // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
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
     * 方法说明：
     * 1. 职责：执行 `extractParameter` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private <T> T extractParameter(Class<T> clazz, Object... additionalInfo) {
        return extractParameter(clazz, 0, additionalInfo);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractParameter` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 1. 职责：执行 `getFailureStack` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private String getFailureStack(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `canLog` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private boolean canLog(EntityType entityType, ActionType actionType) {
        return auditLogLevelFilter.logEnabled(entityType, actionType);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createAuditLogEntry` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 方法说明：
     * 1. 职责：执行 `logAction` 对应的审计与事件持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由业务服务在关键操作后创建事件或审计记录，并由数据库写入、清理任务或测试流程消费时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收业务事件上下文后写入数据库或下沉目标，并按租户、实体和时间范围支持查询。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
