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
package org.thingsboard.server.transport.snmp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.service.ProtoTransportEntityService;
import org.thingsboard.server.transport.snmp.service.SnmpAuthService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportBalancingService;
import org.thingsboard.server.transport.snmp.service.SnmpTransportService;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@TbSnmpTransportComponent
@Component
@Slf4j
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`SnmpTransportContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SnmpTransportContext extends TransportContext {
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `snmpTransportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final SnmpTransportService snmpTransportService;
    private final TransportDeviceProfileCache deviceProfileCache;
    /**
     * 字段说明：
     * 1. 保存 `transportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TransportService transportService;
    private final ProtoTransportEntityService protoEntityService;
    /**
     * 字段说明：
     * 1. 保存 `balancingService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final SnmpTransportBalancingService balancingService;
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `snmpAuthService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final SnmpAuthService snmpAuthService;

    private final Map<DeviceId, DeviceSessionContext> sessions = new ConcurrentHashMap<>();
    private final Collection<DeviceId> allSnmpDevicesIds = new ConcurrentLinkedDeque<>();

    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    /**
     * 方法说明：
     * 1. 职责：执行 `fetchDevicesAndEstablishSessions` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void fetchDevicesAndEstablishSessions() {
        log.info("Initializing SNMP devices sessions");

        int batchIndex = 0;
        int batchSize = 512;
        boolean nextBatchExists = true;

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        while (nextBatchExists) {
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.GetSnmpDevicesResponseMsg snmpDevicesResponse = protoEntityService.getSnmpDevicesIds(batchIndex, batchSize);
            snmpDevicesResponse.getIdsList().stream()
                    .map(id -> new DeviceId(UUID.fromString(id)))
                    .peek(allSnmpDevicesIds::add)
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    .filter(deviceId -> balancingService.isManagedByCurrentTransport(deviceId.getId()))
                    .map(protoEntityService::getDeviceById)
                    .forEach(device -> getExecutor().execute(() -> establishDeviceSession(device)));

            nextBatchExists = snmpDevicesResponse.getHasNextPage();
            batchIndex++;
        }

        log.debug("Found all SNMP devices ids: {}", allSnmpDevicesIds);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `establishDeviceSession` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void establishDeviceSession(Device device) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device == null) return;
        log.info("Establishing SNMP session for device {}", device.getId());

        DeviceProfileId deviceProfileId = device.getDeviceProfileId();
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            return;
        }

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        SnmpDeviceProfileTransportConfiguration profileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        DeviceSessionContext sessionContext;
        try {
            sessionContext = DeviceSessionContext.builder()
                    .tenantId(deviceProfile.getTenantId())
                    .device(device)
                    .deviceProfile(deviceProfile)
                    .token(credentials.getCredentialsId())
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    .profileTransportConfiguration(profileTransportConfiguration)
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    .deviceTransportConfiguration(deviceTransportConfiguration)
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    .snmpTransportContext(this)
                    .build();
            registerSessionMsgListener(sessionContext);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.error("Failed to establish session for SNMP device {}: {}", device.getId(), e.toString());
            transportService.errorEvent(device.getTenantId(), device.getId(), "sessionEstablishing", e);
            return;
        }
        sessions.put(device.getId(), sessionContext);
        snmpTransportService.createQueryingTasks(sessionContext);
        log.info("Established SNMP device session for device {}", device.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateDeviceSession` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void updateDeviceSession(DeviceSessionContext sessionContext, Device device, DeviceProfile deviceProfile) {
        log.info("Updating SNMP session for device {}", device.getId());

        DeviceCredentials credentials = protoEntityService.getDeviceCredentialsByDeviceId(device.getId());
        if (credentials.getCredentialsType() != DeviceCredentialsType.ACCESS_TOKEN) {
            log.warn("[{}] Expected credentials type is {} but found {}", device.getId(), DeviceCredentialsType.ACCESS_TOKEN, credentials.getCredentialsType());
            destroyDeviceSession(sessionContext);
            return;
        }

        SnmpDeviceProfileTransportConfiguration newProfileTransportConfiguration = (SnmpDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
        SnmpDeviceTransportConfiguration newDeviceTransportConfiguration = (SnmpDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();

        try {
            if (!newProfileTransportConfiguration.equals(sessionContext.getProfileTransportConfiguration())) {
                sessionContext.setProfileTransportConfiguration(newProfileTransportConfiguration);
                sessionContext.setDevice(device);
                sessionContext.initializeTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
                snmpTransportService.cancelQueryingTasks(sessionContext);
                snmpTransportService.createQueryingTasks(sessionContext);
                transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.UPDATED, true, null);
            } else if (!newDeviceTransportConfiguration.equals(sessionContext.getDeviceTransportConfiguration())) {
                sessionContext.setDeviceTransportConfiguration(newDeviceTransportConfiguration);
                sessionContext.setDevice(device);
                sessionContext.initializeTarget(newProfileTransportConfiguration, newDeviceTransportConfiguration);
                transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.UPDATED, true, null);
            } else {
                log.trace("Configuration of the device {} was not updated", device);
            }
        } catch (Exception e) {
            log.error("Failed to update session for SNMP device {}: {}", sessionContext.getDeviceId(), e.getMessage());
            transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.UPDATED, false, e);
            destroyDeviceSession(sessionContext);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `destroyDeviceSession` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void destroyDeviceSession(DeviceSessionContext sessionContext) {
        if (sessionContext == null) return;
        log.info("Destroying SNMP device session for device {}", sessionContext.getDevice().getId());
        sessionContext.close();
        snmpAuthService.cleanUpSnmpAuthInfo(sessionContext);
        transportService.deregisterSession(sessionContext.getSessionInfo());
        snmpTransportService.cancelQueryingTasks(sessionContext);
        sessions.remove(sessionContext.getDeviceId());
        transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.STOPPED, true, null);
        log.trace("Unregistered and removed session");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `registerSessionMsgListener` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void registerSessionMsgListener(DeviceSessionContext sessionContext) {
        transportService.process(DeviceTransportType.SNMP,
                TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(sessionContext.getToken()).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        if (msg.hasDeviceInfo()) {
                            registerTransportSession(sessionContext, msg);
                            sessionContext.setSessionTimeoutHandler(() -> {
                                registerTransportSession(sessionContext, msg);
                            });
                            transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.STARTED, true, null);
                        } else {
                            log.warn("[{}] Failed to process device auth", sessionContext.getDeviceId());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.warn("[{}] Failed to process device auth: {}", sessionContext.getDeviceId(), e);
                        transportService.lifecycleEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), ComponentLifecycleEvent.STARTED, false, e);
                    }
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `registerTransportSession` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void registerTransportSession(DeviceSessionContext deviceSessionContext, ValidateDeviceCredentialsResponse msg) {
        SessionInfoProto sessionInfo = SessionInfoCreator.create(
                msg, SnmpTransportContext.this, UUID.randomUUID()
        );
        log.debug("Registering transport session: {}", sessionInfo);

        transportService.registerAsyncSession(sessionInfo, deviceSessionContext);
        transportService.process(sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .build(), TransportServiceCallback.EMPTY);
        transportService.process(sessionInfo, TransportProtos.SubscribeToRPCMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .build(), TransportServiceCallback.EMPTY);

        deviceSessionContext.setSessionInfo(sessionInfo);
        deviceSessionContext.setDeviceInfo(msg.getDeviceInfo());
        deviceSessionContext.setConnected(true);
    }

    @EventListener(DeviceUpdatedEvent.class)
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceUpdatedOrCreated` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceUpdatedOrCreated(DeviceUpdatedEvent deviceUpdatedEvent) {
        Device device = deviceUpdatedEvent.getDevice();
        log.debug("Got creating or updating device event for device {}", device);
        DeviceTransportType transportType = Optional.ofNullable(device.getDeviceData().getTransportConfiguration())
                .map(DeviceTransportConfiguration::getType)
                .orElse(null);
        if (!allSnmpDevicesIds.contains(device.getId())) {
            if (transportType != DeviceTransportType.SNMP) {
                return;
            }
            allSnmpDevicesIds.add(device.getId());
            if (balancingService.isManagedByCurrentTransport(device.getId().getId())) {
                establishDeviceSession(device);
            }
        } else {
            if (balancingService.isManagedByCurrentTransport(device.getId().getId())) {
                DeviceSessionContext sessionContext = sessions.get(device.getId());
                if (transportType == DeviceTransportType.SNMP) {
                    if (sessionContext != null) {
                        updateDeviceSession(sessionContext, device, deviceProfileCache.get(device.getDeviceProfileId()));
                    } else {
                        establishDeviceSession(device);
                    }
                } else {
                    log.trace("Transport type was changed to {}", transportType);
                    destroyDeviceSession(sessionContext);
                }
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceDeleted` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceDeleted(DeviceSessionContext sessionContext) {
        destroyDeviceSession(sessionContext);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceProfileUpdated` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceProfileUpdated(DeviceProfile deviceProfile, DeviceSessionContext sessionContext) {
        log.debug("Handling device profile {} update event for device {}", deviceProfile.getId(), sessionContext.getDeviceId());
        updateDeviceSession(sessionContext, sessionContext.getDevice(), deviceProfile);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onSnmpTransportListChanged` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onSnmpTransportListChanged() {
        log.trace("SNMP transport list changed. Updating sessions");
        List<DeviceId> deleted = new LinkedList<>();
        for (DeviceId deviceId : allSnmpDevicesIds) {
            if (balancingService.isManagedByCurrentTransport(deviceId.getId())) {
                if (!sessions.containsKey(deviceId)) {
                    Device device = protoEntityService.getDeviceById(deviceId);
                    if (device != null) {
                        log.info("SNMP device {} is now managed by current transport node", deviceId);
                        establishDeviceSession(device);
                    } else {
                        deleted.add(deviceId);
                    }
                }
            } else {
                Optional.ofNullable(sessions.get(deviceId))
                        .ifPresent(sessionContext -> {
                            log.info("SNMP session for device {} is not managed by current transport node anymore", deviceId);
                            destroyDeviceSession(sessionContext);
                        });
            }
        }
        log.trace("Removing deleted SNMP devices: {}", deleted);
        allSnmpDevicesIds.removeAll(deleted);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `getSessions` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Collection<DeviceSessionContext> getSessions() {
        return sessions.values();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SnmpTransportContext` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
