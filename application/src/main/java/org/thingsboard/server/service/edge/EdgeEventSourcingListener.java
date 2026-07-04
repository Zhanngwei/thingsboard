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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.user.UserServiceImpl;

import javax.annotation.PostConstruct;

/**
 * This event listener does not support async event processing because relay on ThreadLocal
 * Another possible approach is to implement a special annotation and a bunch of classes similar to TransactionalApplicationListener
 * This class is the simplest approach to maintain edge synchronization within the single class.
 * <p>
 * For async event publishers, you have to decide whether publish event on creating async task in the same thread where dao method called
 * @Autowired
 * EdgeEventSynchronizationManager edgeSynchronizationManager
 * ...
 *   //some async write action make future
 *   if (!edgeSynchronizationManager.isSync()) {
 *     future.addCallback(eventPublisher.publishEvent(...))
 *   }
 * */
@Component
@RequiredArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`EdgeEventSourcingListener` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public class EdgeEventSourcingListener {

    /**
     * 字段说明：
     * 1. 保存 `tbClusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbClusterService tbClusterService;
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init() {
        log.info("EdgeEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `handleEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleEvent(SaveEntityEvent<?> event) {
        try {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!isValidSaveEntityEventForEdgeProcessing(event.getEntity(), event.getOldEntity())) {
                return;
            }
            log.trace("[{}] SaveEntityEvent called: {}", event.getTenantId(), event);
            boolean isCreated = Boolean.TRUE.equals(event.getCreated());
            String body = getBodyMsgForEntityEvent(event.getEntity());
            EdgeEventType type = getEdgeEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType action = getActionForEntityEvent(event.getEntity(), isCreated);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                    body, type, action, edgeSynchronizationManager.getEdgeId().get());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("[{}] failed to process SaveEntityEvent: {}", event.getTenantId(), event, e);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `handleEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleEvent(DeleteEntityEvent<?> event) {
        try {
            log.trace("[{}] DeleteEntityEvent called: {}", event.getTenantId(), event);
            EdgeEventType type = getEdgeEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType actionType = getEdgeEventActionTypeForEntityEvent(event.getEntity());
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                    JacksonUtil.toString(event.getEntity()), type, actionType,
                    edgeSynchronizationManager.getEdgeId().get());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("[{}] failed to process DeleteEntityEvent: {}", event.getTenantId(), event, e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeEventActionTypeForEntityEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EdgeEventActionType getEdgeEventActionTypeForEntityEvent(Object entity) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (entity instanceof AlarmComment) {
            return EdgeEventActionType.DELETED_COMMENT;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (entity instanceof Alarm) {
            return EdgeEventActionType.ALARM_DELETE;
        }
        return EdgeEventActionType.DELETED;
    }

    @TransactionalEventListener(fallbackExecution = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `handleEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleEvent(ActionEntityEvent event) {
        try {
            log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                    event.getBody(), null, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()),
                    edgeSynchronizationManager.getEdgeId().get());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    /**
     * 方法说明：
     * 1. 职责：执行 `handleEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleEvent(RelationActionEvent event) {
        try {
            EntityRelation relation = event.getRelation();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (relation == null) {
                log.trace("[{}] skipping RelationActionEvent event in case relation is null: {}", event.getTenantId(), event);
                return;
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!RelationTypeGroup.COMMON.equals(relation.getTypeGroup())) {
                log.trace("[{}] skipping RelationActionEvent event in case NOT COMMON relation type group: {}", event.getTenantId(), event);
                return;
            }
            log.trace("[{}] RelationActionEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, null,
                    JacksonUtil.toString(relation), EdgeEventType.RELATION, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()),
                    edgeSynchronizationManager.getEdgeId().get());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("[{}] failed to process RelationActionEvent: {}", event.getTenantId(), event, e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isValidSaveEntityEventForEdgeProcessing` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean isValidSaveEntityEventForEdgeProcessing(Object entity, Object oldEntity) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (entity instanceof OtaPackageInfo) {
            OtaPackageInfo otaPackageInfo = (OtaPackageInfo) entity;
            return otaPackageInfo.hasUrl() || otaPackageInfo.isHasData();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (entity instanceof RuleChain) {
            RuleChain ruleChain = (RuleChain) entity;
            return RuleChainType.EDGE.equals(ruleChain.getType());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (entity instanceof User) {
            User user = (User) entity;
            if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
                return false;
            }
            if (oldEntity != null) {
                User oldUser = (User) oldEntity;
                cleanUpUserAdditionalInfo(oldUser);
                cleanUpUserAdditionalInfo(user);
                return !user.equals(oldUser);
            }
        } else if (entity instanceof AlarmApiCallResult || entity instanceof Alarm) {
            return false;
        }
        // Default: If the entity doesn't match any of the conditions, consider it as valid.
        return true;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `cleanUpUserAdditionalInfo` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void cleanUpUserAdditionalInfo(User user) {
        // reset FAILED_LOGIN_ATTEMPTS and LAST_LOGIN_TS - edge is not interested in this information
        if (user.getAdditionalInfo() instanceof NullNode) {
            user.setAdditionalInfo(null);
        }
        if (user.getAdditionalInfo() instanceof ObjectNode) {
            ObjectNode additionalInfo = ((ObjectNode) user.getAdditionalInfo());
            additionalInfo.remove(UserServiceImpl.FAILED_LOGIN_ATTEMPTS);
            additionalInfo.remove(UserServiceImpl.LAST_LOGIN_TS);
            if (additionalInfo.isEmpty()) {
                user.setAdditionalInfo(null);
            } else {
                user.setAdditionalInfo(additionalInfo);
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeEventTypeForEntityEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EdgeEventType getEdgeEventTypeForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return EdgeEventType.ALARM_COMMENT;
        }
        return null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getBodyMsgForEntityEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getBodyMsgForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return JacksonUtil.toString(entity);
        }
        return null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getActionForEntityEvent` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EdgeEventActionType getActionForEntityEvent(Object entity, boolean isCreated) {
        if (entity instanceof AlarmComment) {
            return isCreated ? EdgeEventActionType.ADDED_COMMENT : EdgeEventActionType.UPDATED_COMMENT;
        }
        return isCreated ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeEventSourcingListener` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
