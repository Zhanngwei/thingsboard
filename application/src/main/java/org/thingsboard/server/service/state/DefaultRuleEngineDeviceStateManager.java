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
package org.thingsboard.server.service.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@TbRuleEngineComponent
/**
 * 中文说明：
 * 1. 类目的：`DefaultRuleEngineDeviceStateManager` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultRuleEngineDeviceStateManager implements RuleEngineDeviceStateManager {

    /**
     * 字段说明：
     * 1. 保存 `serviceInfoProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    /**
     * 字段说明：
     * 1. 保存 `deviceStateService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final Optional<DeviceStateService> deviceStateService;
    private final TbClusterService clusterService;

    /**
     * 方法说明：
     * 1. 职责：执行 `DefaultRuleEngineDeviceStateManager` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DefaultRuleEngineDeviceStateManager(
            TbServiceInfoProvider serviceInfoProvider, PartitionService partitionService,
            Optional<DeviceStateService> deviceStateServiceOptional, TbClusterService clusterService
    ) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        this.deviceStateService = deviceStateServiceOptional;
        this.clusterService = clusterService;
    }

    @Getter
    /**
     * 中文说明：
     * 1. 类目的：`ConnectivityEventInfo` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private abstract static class ConnectivityEventInfo {

        /**
         * 字段说明：
         * 1. 保存 `tenantId` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final TenantId tenantId;
        private final DeviceId deviceId;
        /**
         * 字段说明：
         * 1. 保存 `eventTime` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final long eventTime;

        /**
         * 方法说明：
         * 1. 职责：执行 `ConnectivityEventInfo` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        private ConnectivityEventInfo(TenantId tenantId, DeviceId deviceId, long eventTime) {
            this.tenantId = tenantId;
            this.deviceId = deviceId;
            this.eventTime = eventTime;
        }

        /**
         * 方法说明：
         * 1. 职责：执行 `forwardToLocalService` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        abstract void forwardToLocalService();

        /**
         * 方法说明：
         * 1. 职责：执行 `toQueueMsg` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        abstract TransportProtos.ToCoreMsg toQueueMsg();

    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceConnect` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, connectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceConnect(tenantId, deviceId, connectTime));
            }

            @Override
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.ToCoreMsg toQueueMsg() {
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastConnectTime(connectTime)
                        .build();
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceConnectMsg(deviceConnectMsg)
                        .build();
            }
        }, callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceActivity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, activityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceActivity(tenantId, deviceId, activityTime));
            }

            @Override
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.ToCoreMsg toQueueMsg() {
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastActivityTime(activityTime)
                        .build();
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceActivityMsg(deviceActivityMsg)
                        .build();
            }
        }, callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceDisconnect` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, disconnectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceDisconnect(tenantId, deviceId, disconnectTime));
            }

            @Override
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.ToCoreMsg toQueueMsg() {
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastDisconnectTime(disconnectTime)
                        .build();
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceDisconnectMsg(deviceDisconnectMsg)
                        .build();
            }
        }, callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceInactivity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, inactivityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceInactivity(tenantId, deviceId, inactivityTime));
            }

            @Override
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.ToCoreMsg toQueueMsg() {
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastInactivityTime(inactivityTime)
                        .build();
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceInactivityMsg(deviceInactivityMsg)
                        .build();
            }
        }, callback);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `routeEvent` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void routeEvent(ConnectivityEventInfo eventInfo, TbCallback callback) {
        var tenantId = eventInfo.getTenantId();
        var deviceId = eventInfo.getDeviceId();
        long eventTime = eventInfo.getEventTime();

        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) && tpi.isMyPartition() && deviceStateService.isPresent()) {
            log.debug("[{}][{}] Forwarding device connectivity event to local service. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            try {
                eventInfo.forwardToLocalService();
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process device connectivity event. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime, e);
                callback.onFailure(e);
                return;
            }
            callback.onSuccess();
        } else {
            TransportProtos.ToCoreMsg msg = eventInfo.toQueueMsg();
            log.debug("[{}][{}] Sending device connectivity message to core. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            clusterService.pushMsgToCore(tpi, UUID.randomUUID(), msg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultRuleEngineDeviceStateManager` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
