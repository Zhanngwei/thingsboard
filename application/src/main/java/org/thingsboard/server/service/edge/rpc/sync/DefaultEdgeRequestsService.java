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
package org.thingsboard.server.service.edge.rpc.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.edge.v1.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewsRequestMsg;
import org.thingsboard.server.gen.edge.v1.RelationRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.WidgetBundleTypesRequestMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@TbCoreComponent
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultEdgeRequestsService` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class DefaultEdgeRequestsService implements EdgeRequestsService {

    /**
     * 字段说明：
     * 1. 保存 `DEFAULT_PAGE_SIZE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int DEFAULT_PAGE_SIZE = 1000;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `edgeEventService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EdgeEventService edgeEventService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `attributesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AttributesService attributesService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `timeseriesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TimeseriesService timeseriesService;
    
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `relationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RelationService relationService;

    @Lazy
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `entityViewService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbEntityViewService entityViewService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `widgetsBundleService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `widgetTypeService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private WidgetTypeService widgetTypeService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `dbCallbackExecutorService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tbClusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbClusterService tbClusterService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processRuleChainMetadataRequestMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        log.trace("[{}] processRuleChainMetadataRequestMsg [{}][{}]", tenantId, edge.getName(), ruleChainMetadataRequestMsg);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() == 0 || ruleChainMetadataRequestMsg.getRuleChainIdLSB() == 0) {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            return Futures.immediateFuture(null);
        }
        RuleChainId ruleChainId =
                new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(),
                EdgeEventType.RULE_CHAIN_METADATA, EdgeEventActionType.ADDED, ruleChainId, null);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processAttributesRequestMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg) {
        log.trace("[{}] processAttributesRequestMsg [{}][{}]", tenantId, edge.getName(), attributesRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        final EdgeEventType entityType = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (entityType == null) {
            log.warn("[{}] Type doesn't supported {}", tenantId, entityId.getEntityType());
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            return Futures.immediateFuture(null);
        }
        String scope = attributesRequestMsg.getScope();
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        ListenableFuture<List<AttributeKvEntry>> findAttrFuture = attributesService.findAll(tenantId, entityId, scope);
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return Futures.transformAsync(findAttrFuture, ssAttributes
                        -> processEntityAttributesAndAddToEdgeQueue(tenantId, entityId, edge, entityType, scope, ssAttributes, attributesRequestMsg),
                dbCallbackExecutorService);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processEntityAttributesAndAddToEdgeQueue` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> processEntityAttributesAndAddToEdgeQueue(TenantId tenantId, EntityId entityId, Edge edge,
                                                                            EdgeEventType entityType, String scope, List<AttributeKvEntry> ssAttributes,
                                                                            AttributesRequestMsg attributesRequestMsg) {
        try {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            ListenableFuture<Void> future;
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (ssAttributes == null || ssAttributes.isEmpty()) {
                log.trace("[{}][{}] No attributes found for entity {} [{}]", tenantId,
                        edge.getName(),
                        entityId.getEntityType(),
                        entityId.getId());
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                future = Futures.immediateFuture(null);
            } else {
                Map<String, Object> entityData = new HashMap<>();
                ObjectNode attributes = JacksonUtil.newObjectNode();
                // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                for (AttributeKvEntry attr : ssAttributes) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (DefaultDeviceStateService.PERSISTENT_ATTRIBUTES.contains(attr.getKey())
                            && !DefaultDeviceStateService.INACTIVITY_TIMEOUT.equals(attr.getKey())) {
                        continue;
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                        attributes.put(attr.getKey(), attr.getBooleanValue().get());
                    } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                        attributes.put(attr.getKey(), attr.getDoubleValue().get());
                    } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                        attributes.put(attr.getKey(), attr.getLongValue().get());
                    } else if (attr.getDataType() == DataType.JSON && attr.getJsonValue().isPresent()) {
                        attributes.set(attr.getKey(), JacksonUtil.toJsonNode(attr.getJsonValue().get()));
                    } else {
                        attributes.put(attr.getKey(), attr.getValueAsString());
                    }
                }
                if (attributes.size() > 0) {
                    entityData.put("kv", attributes);
                    entityData.put("scope", scope);
                    JsonNode body = JacksonUtil.valueToTree(entityData);
                    log.debug("[{}] Sending attributes data msg, entityId [{}], attributes [{}]", tenantId, entityId, body);
                    future = saveEdgeEvent(tenantId, edge.getId(), entityType, EdgeEventActionType.ATTRIBUTES_UPDATED, entityId, body);
                } else {
                    future = Futures.immediateFuture(null);
                }
            }
            return Futures.transformAsync(future, v -> processLatestTimeseriesAndAddToEdgeQueue(tenantId, entityId, edge, entityType), dbCallbackExecutorService);
        } catch (Exception e) {
            String errMsg = String.format("[%s][%s] Failed to save attribute updates to the edge [%s]", tenantId, edge.getId(), attributesRequestMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
    }
    
    /**
     * 方法说明：
     * 1. 职责：执行 `processLatestTimeseriesAndAddToEdgeQueue` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> processLatestTimeseriesAndAddToEdgeQueue(TenantId tenantId, EntityId entityId, Edge edge,
                                                                            EdgeEventType entityType) {
        ListenableFuture<List<TsKvEntry>> getAllLatestFuture = timeseriesService.findAllLatest(tenantId, entityId);
        return Futures.transformAsync(getAllLatestFuture, tsKvEntries -> {
            if (tsKvEntries == null || tsKvEntries.isEmpty()) {
                log.trace("[{}][{}] No timeseries found for entity {} [{}]", tenantId,
                        edge.getName(),
                        entityId.getEntityType(),
                        entityId.getId());
                return Futures.immediateFuture(null);
            }
            Map<Long, Map<String, Object>> tsData = new HashMap<>();
            for (TsKvEntry tsKvEntry : tsKvEntries) {
                if (DefaultDeviceStateService.PERSISTENT_ATTRIBUTES.contains(tsKvEntry.getKey())) {
                    continue;
                }
                tsData.computeIfAbsent(tsKvEntry.getTs(), k -> new HashMap<>()).put(tsKvEntry.getKey(), tsKvEntry.getValue());
            }
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            for (Map.Entry<Long, Map<String, Object>> entry : tsData.entrySet()) {
                Map<String, Object> entityBody = new HashMap<>();
                entityBody.put("data", entry.getValue());
                entityBody.put("ts", entry.getKey());
                futures.add(saveEdgeEvent(tenantId, edge.getId(), entityType, EdgeEventActionType.TIMESERIES_UPDATED, entityId, JacksonUtil.valueToTree(entityBody)));
            }
            return Futures.transform(Futures.allAsList(futures), v -> null, dbCallbackExecutorService);
        }, dbCallbackExecutorService);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processRelationRequestMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processRelationRequestMsg(TenantId tenantId, Edge edge, RelationRequestMsg relationRequestMsg) {
        log.trace("[{}] processRelationRequestMsg [{}][{}]", tenantId, edge.getName(), relationRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(relationRequestMsg.getEntityType()),
                new UUID(relationRequestMsg.getEntityIdMSB(), relationRequestMsg.getEntityIdLSB()));

        List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.FROM));
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.TO));
        ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(relationsListFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<List<EntityRelation>> relationsList) {
                try {
                    if (relationsList != null && !relationsList.isEmpty()) {
                        List<ListenableFuture<Void>> futures = new ArrayList<>();
                        for (List<EntityRelation> entityRelations : relationsList) {
                            log.trace("[{}][{}][{}][{}] relation(s) are going to be pushed to edge.", tenantId, edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        futures.add(saveEdgeEvent(tenantId,
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                EdgeEventActionType.ADDED,
                                                null,
                                                JacksonUtil.valueToTree(relation)));
                                    }
                                } catch (Exception e) {
                                    String errMsg = String.format("[%s][%s] Exception during loading relation [%s] to edge on sync!", tenantId, edge.getId(), relation);
                                    log.error(errMsg, e);
                                    futureToSet.setException(new RuntimeException(errMsg, e));
                                    return;
                                }
                            }
                        }
                        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable List<Void> voids) {
                                futureToSet.set(null);
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                String errMsg = String.format("[%s][%s] Exception during saving edge events [%s]!", tenantId, edge.getId(), relationRequestMsg);
                                log.error(errMsg, throwable);
                                futureToSet.setException(new RuntimeException(errMsg, throwable));
                            }
                        }, dbCallbackExecutorService);
                    } else {
                        futureToSet.set(null);
                    }
                } catch (Exception e) {
                    log.error("[{}] Exception during loading relation(s) to edge on sync!", tenantId, e);
                    futureToSet.setException(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                String errMsg = String.format("[%s] Can't find relation by query. Entity id [%s]!", tenantId, entityId);
                log.error(errMsg, t);
                futureToSet.setException(new RuntimeException(errMsg, t));
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `findRelationByQuery` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<List<EntityRelation>> findRelationByQuery(TenantId tenantId, Edge edge,
                                                                       EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, 1, false));
        return relationService.findByQuery(tenantId, query);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processDeviceCredentialsRequestMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        log.trace("[{}] processDeviceCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), deviceCredentialsRequestMsg);
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() == 0 || deviceCredentialsRequestMsg.getDeviceIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE,
                EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processUserCredentialsRequestMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processUserCredentialsRequestMsg(TenantId tenantId, Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg) {
        log.trace("[{}] processUserCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), userCredentialsRequestMsg);
        if (userCredentialsRequestMsg.getUserIdMSB() == 0 || userCredentialsRequestMsg.getUserIdLSB() == 0) {
            return Futures.immediateFuture(null);
        }
        UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
        return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER,
                EdgeEventActionType.CREDENTIALS_UPDATED, userId, null);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processWidgetBundleTypesRequestMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processWidgetBundleTypesRequestMsg(TenantId tenantId, Edge edge,
                                                                     WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg) {
        log.trace("[{}] processWidgetBundleTypesRequestMsg [{}][{}]", tenantId, edge.getName(), widgetBundleTypesRequestMsg);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (widgetBundleTypesRequestMsg.getWidgetBundleIdMSB() != 0 && widgetBundleTypesRequestMsg.getWidgetBundleIdLSB() != 0) {
            WidgetsBundleId widgetsBundleId = new WidgetsBundleId(new UUID(widgetBundleTypesRequestMsg.getWidgetBundleIdMSB(), widgetBundleTypesRequestMsg.getWidgetBundleIdLSB()));
            WidgetsBundle widgetsBundleById = widgetsBundleService.findWidgetsBundleById(tenantId, widgetsBundleId);
            if (widgetsBundleById != null) {
                List<WidgetType> widgetTypesToPush =
                        widgetTypeService.findWidgetTypesByWidgetsBundleId(widgetsBundleById.getTenantId(), widgetsBundleId);
                for (WidgetType widgetType : widgetTypesToPush) {
                    futures.add(saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGET_TYPE, EdgeEventActionType.ADDED, widgetType.getId(), null));
                }
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processEntityViewsRequestMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processEntityViewsRequestMsg(TenantId tenantId, Edge edge, EntityViewsRequestMsg entityViewsRequestMsg) {
        log.trace("[{}] processEntityViewsRequestMsg [{}][{}]", tenantId, edge.getName(), entityViewsRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(entityViewsRequestMsg.getEntityType()),
                new UUID(entityViewsRequestMsg.getEntityIdMSB(), entityViewsRequestMsg.getEntityIdLSB()));
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<EntityView> entityViews) {
                if (entityViews == null || entityViews.isEmpty()) {
                    futureToSet.set(null);
                    return;
                }
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                for (EntityView entityView : entityViews) {
                    ListenableFuture<Boolean> future = relationService.checkRelationAsync(tenantId, edge.getId(), entityView.getId(),
                            EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
                    futures.add(Futures.transformAsync(future, result -> {
                        if (Boolean.TRUE.equals(result)) {
                            return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW,
                                    EdgeEventActionType.ADDED, entityView.getId(), null);
                        } else {
                            return Futures.immediateFuture(null);
                        }
                    }, dbCallbackExecutorService));
                }
                Futures.addCallback(Futures.allAsList(futures), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable List<Void> result) {
                        futureToSet.set(null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Exception during loading relation to edge on sync!", tenantId, t);
                        futureToSet.setException(t);
                    }
                }, dbCallbackExecutorService);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't find entity views by entity id [{}]", tenantId, entityId, t);
                futureToSet.setException(t);
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveEdgeEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                 EdgeId edgeId,
                                                 EdgeEventType type,
                                                 EdgeEventActionType action,
                                                 EntityId entityId,
                                                 JsonNode body) {
        log.trace("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);

        return Futures.transform(edgeEventService.saveAsync(edgeEvent), unused -> {
            tbClusterService.onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, dbCallbackExecutorService);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEdgeRequestsService` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
