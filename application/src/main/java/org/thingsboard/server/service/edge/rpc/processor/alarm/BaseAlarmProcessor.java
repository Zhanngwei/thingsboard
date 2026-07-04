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
package org.thingsboard.server.service.edge.rpc.processor.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`BaseAlarmProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public abstract class BaseAlarmProcessor extends BaseEdgeProcessor {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `alarmCommentDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected AlarmCommentDao alarmCommentDao;

    /**
     * 方法说明：
     * 1. 职责：执行 `processAlarmMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processAlarmMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        log.trace("[{}] processAlarmMsg [{}]", tenantId, alarmUpdateMsg);
        AlarmId alarmId = new AlarmId(new UUID(alarmUpdateMsg.getIdMSB(), alarmUpdateMsg.getIdLSB()));
        EntityId originatorId = getAlarmOriginatorFromMsg(tenantId, alarmUpdateMsg);
        Alarm alarm = constructAlarmFromUpdateMsg(tenantId, alarmId, originatorId, alarmUpdateMsg);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (alarm == null) {
            throw new RuntimeException("[{" + tenantId + "}] alarmUpdateMsg {" + alarmUpdateMsg + "} cannot be converted to alarm");
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (alarm.getOriginator() == null) {
            log.warn("[{}] Originator not found for the alarm msg {}", tenantId, alarmUpdateMsg);
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            return Futures.immediateFuture(null);
        }
        try {
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (alarmUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                    alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm, null, alarmId));
                    break;
                case ENTITY_UPDATED_RPC_MESSAGE:
                    alarmService.updateAlarm(AlarmUpdateRequest.fromAlarm(alarm));
                    break;
                case ALARM_ACK_RPC_MESSAGE:
                    Alarm alarmToAck = alarmService.findAlarmById(tenantId, alarmId);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (alarmToAck != null) {
                        alarmService.acknowledgeAlarm(tenantId, alarmId, alarm.getAckTs());
                    }
                    break;
                case ALARM_CLEAR_RPC_MESSAGE:
                    Alarm alarmToClear = alarmService.findAlarmById(tenantId, alarmId);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (alarmToClear != null) {
                        alarmService.clearAlarm(tenantId, alarmId, alarm.getClearTs(), alarm.getDetails());
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    Alarm alarmToDelete = alarmService.findAlarmById(tenantId, alarmId);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (alarmToDelete != null) {
                        alarmService.delAlarm(tenantId, alarmId);
                    }
                    break;
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(alarmUpdateMsg.getMsgType());
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("[{}] Failed to process alarm update msg [{}]", tenantId, alarmUpdateMsg, e);
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            return Futures.immediateFailedFuture(e);
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return Futures.immediateFuture(null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processAlarmCommentMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processAlarmCommentMsg(TenantId tenantId, AlarmCommentUpdateMsg alarmCommentUpdateMsg) {
        log.trace("[{}] processAlarmCommentMsg [{}]", tenantId, alarmCommentUpdateMsg);
        AlarmComment alarmComment = JacksonUtil.fromString(alarmCommentUpdateMsg.getEntity(), AlarmComment.class, true);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (alarmComment == null) {
            throw new RuntimeException("[{" + tenantId + "}] alarmCommentUpdateMsg {" + alarmCommentUpdateMsg + "} cannot be converted to alarm comment");
        }
        try {
            Alarm alarm = alarmService.findAlarmById(tenantId, new AlarmId(alarmComment.getAlarmId().getId()));
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (alarm == null) {
                return Futures.immediateFuture(null);
            }
            switch (alarmCommentUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                    alarmCommentDao.save(tenantId, alarmComment);
                    break;
                case ENTITY_UPDATED_RPC_MESSAGE:
                    alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    AlarmComment alarmCommentToDelete = alarmCommentService.findAlarmCommentById(tenantId, alarmComment.getId());
                    if (alarmCommentToDelete != null) {
                        alarmCommentService.saveAlarmComment(tenantId, alarmCommentToDelete);
                    }
                    break;
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(alarmCommentUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process alarm comment update msg [{}]", tenantId, alarmCommentUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(null);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `getAlarmOriginatorFromMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected abstract EntityId getAlarmOriginatorFromMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg);

    /**
     * 方法说明：
     * 1. 职责：执行 `constructAlarmFromUpdateMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected abstract Alarm constructAlarmFromUpdateMsg(TenantId tenantId, AlarmId alarmId, EntityId originatorId, AlarmUpdateMsg alarmUpdateMsg);

    /**
     * 方法说明：
     * 1. 职责：执行 `convertAlarmEventToAlarmMsg` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected AlarmUpdateMsg convertAlarmEventToAlarmMsg(TenantId tenantId, UUID entityId, EdgeEventActionType actionType, JsonNode body, EdgeVersion edgeVersion) {
        AlarmId alarmId = new AlarmId(entityId);
        UpdateMsgType msgType = getUpdateMsgType(actionType);
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
                Alarm alarm = alarmService.findAlarmById(tenantId, alarmId);
                if (alarm != null) {
                    return ((AlarmMsgConstructor) alarmMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                            .constructAlarmUpdatedMsg(msgType, alarm, findOriginatorEntityName(tenantId, alarm));
                }
                break;
            case ALARM_DELETE:
            case DELETED:
                Alarm deletedAlarm = JacksonUtil.convertValue(body, Alarm.class);
                if (deletedAlarm != null) {
                    return ((AlarmMsgConstructor) alarmMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                            .constructAlarmUpdatedMsg(msgType, deletedAlarm, findOriginatorEntityName(tenantId, deletedAlarm));
                }
        }
        return null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `findOriginatorEntityName` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String findOriginatorEntityName(TenantId tenantId, Alarm alarm) {
        String entityName = null;
        switch (alarm.getOriginator().getEntityType()) {
            case DEVICE:
                Device deviceById = deviceService.findDeviceById(tenantId, new DeviceId(alarm.getOriginator().getId()));
                if (deviceById != null) {
                    entityName = deviceById.getName();
                }
                break;
            case ASSET:
                Asset assetById = assetService.findAssetById(tenantId, new AssetId(alarm.getOriginator().getId()));
                if (assetById != null) {
                    entityName = assetById.getName();
                }
                break;
            case ENTITY_VIEW:
                EntityView entityViewById = entityViewService.findEntityViewById(tenantId, new EntityViewId(alarm.getOriginator().getId()));
                if (entityViewById != null) {
                    entityName = entityViewById.getName();
                }
                break;
        }
        return entityName;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseAlarmProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
