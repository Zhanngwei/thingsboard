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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.entity.relation.TbEntityRelationService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RELATION_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RELATION_TYPE_GROUP_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RELATION_TYPE_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`EntityRelationController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class EntityRelationController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `tbEntityRelationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbEntityRelationService tbEntityRelationService;

    /**
     * 字段说明：
     * 1. 保存 `TO_TYPE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_TYPE = "toType";
    public static final String FROM_ID = "fromId";
    /**
     * 字段说明：
     * 1. 保存 `FROM_TYPE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String FROM_TYPE = "fromType";
    public static final String RELATION_TYPE = "relationType";
    /**
     * 字段说明：
     * 1. 保存 `TO_ID` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TO_ID = "toId";

    private static final String SECURITY_CHECKS_ENTITIES_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that 'from' and 'to' entities are owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that 'from' and 'to' entities are owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the 'from' and 'to' entities are assigned to the same customer.";

    private static final String SECURITY_CHECKS_ENTITY_DESCRIPTION = "\n\nIf the user has the authority of 'System Administrator', the server checks that the entity is owned by the sysadmin. " +
            "If the user has the authority of 'Tenant Administrator', the server checks that the entity is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the entity is assigned to the same customer.";

    @ApiOperation(value = "Create Relation (saveRelation)",
            notes = "Creates or updates a relation between two entities in the platform. " +
                    "Relations unique key is a combination of from/to entity id and relation type group and relation type. " +
                    SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `saveRelation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void saveRelation(@ApiParam(value = "A JSON value representing the relation.", required = true)
                             @RequestBody EntityRelation relation) throws ThingsboardException {
        checkNotNull(relation);
        checkCanCreateRelation(relation.getFrom());
        checkCanCreateRelation(relation.getTo());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (relation.getTypeGroup() == null) {
            relation.setTypeGroup(RelationTypeGroup.COMMON);
        }

        tbEntityRelationService.save(getTenantId(), getCurrentUser().getCustomerId(), relation, getCurrentUser());
    }

    @ApiOperation(value = "Delete Relation (deleteRelation)",
            notes = "Deletes a relation between two entities in the platform. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.DELETE, params = {FROM_ID, FROM_TYPE, RELATION_TYPE, TO_ID, TO_TYPE})
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteRelation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteRelation(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                               @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                               @ApiParam(value = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                               @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                               @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                               @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(RELATION_TYPE, strRelationType);
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkCanCreateRelation(fromId);
        checkCanCreateRelation(toId);

        RelationTypeGroup relationTypeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        EntityRelation relation = new EntityRelation(fromId, toId, strRelationType, relationTypeGroup);
        tbEntityRelationService.delete(getTenantId(), getCurrentUser().getCustomerId(), relation, getCurrentUser());
    }

    @ApiOperation(value = "Delete common relations (deleteCommonRelations)",
            notes = "Deletes all the relations ('from' and 'to' direction) for the specified entity and relation type group: 'COMMON'. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.DELETE, params = {"entityId", "entityType"})
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteRelations` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteRelations(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam("entityId") String strId,
                                @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam("entityType") String strType) throws ThingsboardException {
        checkParameter("entityId", strId);
        checkParameter("entityType", strType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strType, strId);
        checkEntityId(entityId, Operation.WRITE);
        tbEntityRelationService.deleteCommonRelations(getTenantId(), getCurrentUser().getCustomerId(), entityId, getCurrentUser());
    }

    @ApiOperation(value = "Get Relation (getRelation)",
            notes = "Returns relation object between two specified entities if present. Otherwise throws exception. " + SECURITY_CHECKS_ENTITIES_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE, RELATION_TYPE, TO_ID, TO_TYPE})
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getRelation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityRelation getRelation(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                      @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                      @ApiParam(value = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                      @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION) @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                                      @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                      @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(RELATION_TYPE, strRelationType);
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(fromId, Operation.READ);
        checkEntityId(toId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(relationService.getRelation(getTenantId(), fromId, toId, strRelationType, typeGroup));
    }

    @ApiOperation(value = "Get List of Relations (findByFrom)",
            notes = "Returns list of relation objects for the specified entity by the 'from' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE})
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findByFrom` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelation> findByFrom(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                           @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                           @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByFrom(getTenantId(), entityId, typeGroup)));
    }

    @ApiOperation(value = "Get List of Relation Infos (findInfoByFrom)",
            notes = "Returns list of relation info objects for the specified entity by the 'from' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE})
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findInfoByFrom` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelationInfo> findInfoByFrom(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                                   @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                                   @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                   @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException, ExecutionException, InterruptedException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByFrom(getTenantId(), entityId, typeGroup).get()));
    }

    @ApiOperation(value = "Get List of Relations (findByFrom)",
            notes = "Returns list of relation objects for the specified entity by the 'from' direction and relation type. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {FROM_ID, FROM_TYPE, RELATION_TYPE})
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findByFrom` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelation> findByFrom(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_ID) String strFromId,
                                           @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(FROM_TYPE) String strFromType,
                                           @ApiParam(value = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                           @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(FROM_ID, strFromId);
        checkParameter(FROM_TYPE, strFromType);
        checkParameter(RELATION_TYPE, strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByFromAndType(getTenantId(), entityId, strRelationType, typeGroup)));
    }

    @ApiOperation(value = "Get List of Relations (findByTo)",
            notes = "Returns list of relation objects for the specified entity by the 'to' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {TO_ID, TO_TYPE})
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findByTo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelation> findByTo(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                         @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                         @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByTo(getTenantId(), entityId, typeGroup)));
    }

    @ApiOperation(value = "Get List of Relation Infos (findInfoByTo)",
            notes = "Returns list of relation info objects for the specified entity by the 'to' direction. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION + " " + RELATION_INFO_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {TO_ID, TO_TYPE})
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findInfoByTo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelationInfo> findInfoByTo(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                                 @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                                 @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                                 @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException, ExecutionException, InterruptedException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByTo(getTenantId(), entityId, typeGroup).get()));
    }

    @ApiOperation(value = "Get List of Relations (findByTo)",
            notes = "Returns list of relation objects for the specified entity by the 'to' direction and relation type. " +
                    SECURITY_CHECKS_ENTITY_DESCRIPTION,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {TO_ID, TO_TYPE, RELATION_TYPE})
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findByTo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelation> findByTo(@ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @RequestParam(TO_ID) String strToId,
                                         @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(TO_TYPE) String strToType,
                                         @ApiParam(value = RELATION_TYPE_PARAM_DESCRIPTION, required = true) @RequestParam(RELATION_TYPE) String strRelationType,
                                         @ApiParam(value = RELATION_TYPE_GROUP_PARAM_DESCRIPTION)
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter(TO_ID, strToId);
        checkParameter(TO_TYPE, strToType);
        checkParameter(RELATION_TYPE, strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId, Operation.READ);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByToAndType(getTenantId(), entityId, strRelationType, typeGroup)));
    }

    @ApiOperation(value = "Find related entities (findByQuery)",
            notes = "Returns all entities that are related to the specific entity. " +
                    "The entity id, relation type, entity types, depth of the search, and other query parameters defined using complex 'EntityRelationsQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findByQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelation> findByQuery(@ApiParam(value = "A JSON value representing the entity relations query object.", required = true)
                                            @RequestBody EntityRelationsQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        return checkNotNull(filterRelationsByReadPermission(relationService.findByQuery(getTenantId(), query).get()));
    }

    @ApiOperation(value = "Find related entity infos (findInfoByQuery)",
            notes = "Returns all entity infos that are related to the specific entity. " +
                    "The entity id, relation type, entity types, depth of the search, and other query parameters defined using complex 'EntityRelationsQuery' object. " +
                    "See 'Model' tab of the Parameters for more info. " + RELATION_INFO_DESCRIPTION, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findInfoByQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityRelationInfo> findInfoByQuery(@ApiParam(value = "A JSON value representing the entity relations query object.", required = true)
                                                    @RequestBody EntityRelationsQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        return checkNotNull(filterRelationsByReadPermission(relationService.findInfoByQuery(getTenantId(), query).get()));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkCanCreateRelation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void checkCanCreateRelation(EntityId entityId) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        var isTenantAdminAndRelateToSelf = currentUser.isTenantAdmin() && currentUser.getTenantId().equals(entityId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!isTenantAdminAndRelateToSelf) {
            checkEntityId(entityId, Operation.WRITE);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `filterRelationsByReadPermission` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T extends EntityRelation> List<T> filterRelationsByReadPermission(List<T> relationsByQuery) {
        return relationsByQuery.stream().filter(relationByQuery -> {
            try {
                checkEntityId(relationByQuery.getTo(), Operation.READ);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (ThingsboardException e) {
                return false;
            }
            try {
                checkEntityId(relationByQuery.getFrom(), Operation.READ);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (ThingsboardException e) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `parseRelationTypeGroup` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private RelationTypeGroup parseRelationTypeGroup(String strRelationTypeGroup, RelationTypeGroup defaultValue) {
        RelationTypeGroup result = defaultValue;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (strRelationTypeGroup != null && strRelationTypeGroup.trim().length() > 0) {
            try {
                result = RelationTypeGroup.valueOf(strRelationTypeGroup);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (IllegalArgumentException e) {
            }
        }
        return result;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EntityRelationController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
