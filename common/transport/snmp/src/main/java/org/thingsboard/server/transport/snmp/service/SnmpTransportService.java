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
package org.thingsboard.server.transport.snmp.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.transport.snmp.SnmpCommunicationSpec;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.SnmpMethod;
import org.thingsboard.server.common.data.transport.snmp.config.RepeatingQueryingSnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.SnmpTransportContext;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;
import org.thingsboard.server.transport.snmp.session.ScheduledTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@TbSnmpTransportComponent
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
/**
 * 中文说明：
 * 1. 类目的：`SnmpTransportService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SnmpTransportService implements TbTransportService, CommandResponder {
    /**
     * 字段说明：
     * 1. 保存 `transportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TransportService transportService;
    private final PduService pduService;
    @Autowired @Lazy
    /**
     * 字段说明：
     * 1. 保存 `transportContext` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private SnmpTransportContext transportContext;

    @Getter
    /**
     * 字段说明：
     * 1. 保存 `snmp` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Snmp snmp;
    private ListeningScheduledExecutorService scheduler;
    /**
     * 字段说明：
     * 1. 保存 `executor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ExecutorService executor;

    private final Map<SnmpCommunicationSpec, ResponseDataMapper> responseDataMappers = new EnumMap<>(SnmpCommunicationSpec.class);
    private final Map<SnmpCommunicationSpec, ResponseProcessor> responseProcessors = new EnumMap<>(SnmpCommunicationSpec.class);

    @Value("${transport.snmp.bind_port:1620}")
    /**
     * 字段说明：
     * 1. 保存 `snmpBindPort` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Integer snmpBindPort;
    @Value("${transport.snmp.response_processing.parallelism_level:4}")
    /**
     * 字段说明：
     * 1. 保存 `responseProcessingThreadPoolSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int responseProcessingThreadPoolSize;
    @Value("${transport.snmp.scheduler_thread_pool_size:4}")
    /**
     * 字段说明：
     * 1. 保存 `schedulerThreadPoolSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int schedulerThreadPoolSize;
    @Value("${transport.snmp.underlying_protocol}")
    /**
     * 字段说明：
     * 1. 保存 `snmpUnderlyingProtocol` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String snmpUnderlyingProtocol;
    @Value("${transport.snmp.request_chunk_delay_ms:100}")
    /**
     * 字段说明：
     * 1. 保存 `requestChunkDelayMs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int requestChunkDelayMs;

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void init() throws IOException {
        scheduler = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(schedulerThreadPoolSize, ThingsBoardThreadFactory.forName("snmp-querying")));
        executor = ThingsBoardExecutors.newWorkStealingPool(responseProcessingThreadPoolSize, "snmp-response-processing");

        initializeSnmp();
        configureResponseDataMappers();
        configureResponseProcessors();

        log.info("SNMP transport service initialized");
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `stop` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stop() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initializeSnmp` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void initializeSnmp() throws IOException {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportMapping<?> transportMapping;
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (snmpUnderlyingProtocol) {
            case "udp":
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                transportMapping = new DefaultUdpTransportMapping(new UdpAddress(snmpBindPort));
                break;
            case "tcp":
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                transportMapping = new DefaultTcpTransportMapping(new TcpAddress(snmpBindPort));
                break;
            default:
                throw new IllegalArgumentException("Underlying protocol " + snmpUnderlyingProtocol + " for SNMP is not supported");
        }
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        snmp = new Snmp(transportMapping);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        snmp.addNotificationListener(transportMapping, transportMapping.getListenAddress(), this);
        snmp.listen();

        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createQueryingTasks` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void createQueryingTasks(DeviceSessionContext sessionContext) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(communicationConfig -> communicationConfig instanceof RepeatingQueryingSnmpCommunicationConfig)
                .forEach(config -> {
                    RepeatingQueryingSnmpCommunicationConfig repeatingCommunicationConfig = (RepeatingQueryingSnmpCommunicationConfig) config;
                    Long queryingFrequency = repeatingCommunicationConfig.getQueryingFrequencyMs();

                    ScheduledTask scheduledTask = new ScheduledTask();
                    scheduledTask.init(() -> {
                        try {
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            if (sessionContext.isActive()) {
                                return sendRequest(sessionContext, repeatingCommunicationConfig);
                            }
                        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                        } catch (Exception e) {
                            log.error("Failed to send SNMP request for device {}: {}", sessionContext.getDeviceId(), e.toString());
                            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), config.getSpec().getLabel(), e);
                        }
                        return Futures.immediateVoidFuture();
                    }, queryingFrequency, scheduler);
                    sessionContext.getQueryingTasks().add(scheduledTask);
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `cancelQueryingTasks` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void cancelQueryingTasks(DeviceSessionContext sessionContext) {
        sessionContext.getQueryingTasks().forEach(ScheduledTask::cancel);
        sessionContext.getQueryingTasks().clear();
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `sendRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig) {
        return sendRequest(sessionContext, communicationConfig, Collections.emptyMap());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> sendRequest(DeviceSessionContext sessionContext, SnmpCommunicationConfig communicationConfig, Map<String, String> values) {
        List<PDU> request = pduService.createPdus(sessionContext, communicationConfig, values);
        RequestContext requestContext = RequestContext.builder()
                .communicationSpec(communicationConfig.getSpec())
                .method(communicationConfig.getMethod())
                .responseMappings(communicationConfig.getAllMappings())
                .requestSize(request.size())
                .build();
        return sendRequest(sessionContext, request, requestContext);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> sendRequest(DeviceSessionContext sessionContext, List<PDU> request, RequestContext requestContext) {
        if (request.size() <= 1 || requestChunkDelayMs == 0) {
            for (PDU pdu : request) {
                sendPdu(pdu, requestContext, sessionContext);
            }
            return Futures.immediateVoidFuture();
        }

        List<ListenableFuture<?>> futures = new ArrayList<>();
        for (int i = 0, delay = 0; i < request.size(); i++, delay += requestChunkDelayMs) {
            PDU pdu = request.get(i);
            if (delay == 0) {
                sendPdu(pdu, requestContext, sessionContext);
            } else {
                ListenableScheduledFuture<?> future = scheduler.schedule(() -> {
                    sendPdu(pdu, requestContext, sessionContext);
                }, delay, TimeUnit.MILLISECONDS);
                futures.add(future);
            }
        }
        return Futures.whenAllComplete(futures).call(() -> null, MoreExecutors.directExecutor());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendPdu` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void sendPdu(PDU pdu, RequestContext requestContext, DeviceSessionContext sessionContext) {
        log.debug("[{}] Sending SNMP request with {} variable bindings to {}", sessionContext.getDeviceId(), pdu.size(), sessionContext.getTarget().getAddress());
        try {
            snmp.send(pdu, sessionContext.getTarget(), requestContext, sessionContext);
        } catch (Exception e) {
            log.error("[{}] Failed to send SNMP request", sessionContext.getDeviceId(), e);
            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributeUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAttributeUpdate(DeviceSessionContext sessionContext, TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotification) {
        sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.SHARED_ATTRIBUTES_SETTING)
                .findFirst()
                .ifPresent(communicationConfig -> {
                    Map<String, String> sharedAttributes = JsonConverter.toJson(attributeUpdateNotification).entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString()
                            ));
                    sendRequest(sessionContext, communicationConfig, sharedAttributes);
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onToDeviceRpcRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onToDeviceRpcRequest(DeviceSessionContext sessionContext, TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg) {
        SnmpMethod snmpMethod = SnmpMethod.valueOf(toDeviceRpcRequestMsg.getMethodName());
        JsonObject params = JsonConverter.parse(toDeviceRpcRequestMsg.getParams()).getAsJsonObject();

        String key = Optional.ofNullable(params.get("key")).map(JsonElement::getAsString).orElse(null);
        String value = Optional.ofNullable(params.get("value")).map(JsonElement::getAsString).orElse(null);

        if (value == null && snmpMethod == SnmpMethod.SET) {
            throw new IllegalArgumentException("Value must be specified for SNMP method 'SET'");
        }

        SnmpCommunicationConfig communicationConfig = sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No communication config found with RPC spec"));
        SnmpMapping snmpMapping = communicationConfig.getAllMappings().stream()
                .filter(mapping -> mapping.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No SNMP mapping found in the config for specified key"));

        String oid = snmpMapping.getOid();
        DataType dataType = snmpMapping.getDataType();

        PDU request = pduService.createSingleVariablePdu(sessionContext, snmpMethod, oid, value, dataType);
        RequestContext requestContext = RequestContext.builder()
                .requestId(toDeviceRpcRequestMsg.getRequestId())
                .communicationSpec(communicationConfig.getSpec())
                .method(snmpMethod)
                .responseMappings(communicationConfig.getAllMappings())
                .requestSize(1)
                .build();
        sendRequest(sessionContext, List.of(request), requestContext);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `processResponseEvent` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void processResponseEvent(DeviceSessionContext sessionContext, ResponseEvent event) {
        ((Snmp) event.getSource()).cancel(event.getRequest(), sessionContext);
        RequestContext requestContext = (RequestContext) event.getUserObject();
        if (event.getError() != null) {
            log.warn("[{}] SNMP response error: {}", sessionContext.getDeviceId(), event.getError().toString());
            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), new RuntimeException(event.getError()));
            return;
        }

        PDU responsePdu = event.getResponse();
        log.trace("[{}] Received PDU: {}", sessionContext.getDeviceId(), responsePdu);

        List<PDU> response;
        if (requestContext.getRequestSize() == 1) {
            if (responsePdu == null) {
                if (requestContext.getMethod() == SnmpMethod.GET) {
                    log.debug("[{}][{}] Empty response from device", sessionContext.getDeviceId(), event.getRequest().getRequestID());
                    transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), new RuntimeException("No response from device"));
                }
                return;
            }
            response = List.of(responsePdu);
        } else {
            List<PDU> responseParts = requestContext.getResponseParts();
            responseParts.add(responsePdu);
            if (responseParts.size() == requestContext.getRequestSize()) {
                response = new ArrayList<>();
                for (PDU responsePart : responseParts) {
                    if (responsePart != null) {
                        response.add(responsePart);
                    }
                }
                log.debug("[{}] All {} response parts are collected for request", sessionContext.getDeviceId(), responseParts.size());
            } else {
                log.trace("[{}] Awaiting other response parts for request", sessionContext.getDeviceId());
                return;
            }
        }

        executor.execute(() -> {
            try {
                processResponse(sessionContext, response, requestContext);
            } catch (Exception e) {
                transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), requestContext.getCommunicationSpec().getLabel(), e);
            }
        });
    }

    /*
     * SNMP notifications handler
     *
     * TODO: add check for host uniqueness when saving device (for backward compatibility - only for the ones using from-device RPC requests)
     *
     * NOTE: SNMP TRAPs support won't work properly when there is more than one SNMP transport,
     *  due to load-balancing of requests from devices: session might not be on this instance
     * */
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processPdu` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void processPdu(CommandResponderEvent event) {
        IpAddress sourceAddress = (IpAddress) event.getPeerAddress();
        List<DeviceSessionContext> sessions = transportContext.getSessions().stream()
                .filter(session -> ((IpAddress) session.getTarget().getAddress()).getInetAddress().equals(sourceAddress.getInetAddress()))
                .collect(Collectors.toList());
        if (sessions.isEmpty()) {
            log.warn("Couldn't find device session for SNMP TRAP for address {}", sourceAddress);
            return;
        } else if (sessions.size() > 1) {
            for (DeviceSessionContext sessionContext : sessions) {
                transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(), SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST.getLabel(),
                        new IllegalStateException("Found multiple devices for host " + sourceAddress.getInetAddress().getHostAddress()));
            }
            return;
        }

        DeviceSessionContext sessionContext = sessions.get(0);
        try {
            processIncomingTrap(sessionContext, event);
        } catch (Throwable e) {
            transportService.errorEvent(sessionContext.getTenantId(), sessionContext.getDeviceId(),
                    SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST.getLabel(), e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processIncomingTrap` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processIncomingTrap(DeviceSessionContext sessionContext, CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        if (pdu == null) {
            log.warn("[{}] Received empty SNMP trap", sessionContext.getDeviceId());
            throw new IllegalArgumentException("Received TRAP with no data");
        }

        log.debug("[{}] Processing SNMP trap: {}", sessionContext.getDeviceId(), pdu);
        SnmpCommunicationConfig communicationConfig = sessionContext.getProfileTransportConfiguration().getCommunicationConfigs().stream()
                .filter(config -> config.getSpec() == SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No config found for to-server RPC requests"));
        RequestContext requestContext = RequestContext.builder()
                .communicationSpec(communicationConfig.getSpec())
                .responseMappings(communicationConfig.getAllMappings())
                .method(SnmpMethod.TRAP)
                .build();

        executor.execute(() -> {
            processResponse(sessionContext, List.of(pdu), requestContext);
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processResponse` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processResponse(DeviceSessionContext sessionContext, List<PDU> response, RequestContext requestContext) {
        ResponseProcessor responseProcessor = responseProcessors.get(requestContext.getCommunicationSpec());
        if (responseProcessor == null) return;

        JsonObject responseData = responseDataMappers.get(requestContext.getCommunicationSpec()).map(response, requestContext);
        if (responseData.size() == 0) {
            log.warn("[{}] No values in the response", sessionContext.getDeviceId());
            throw new IllegalArgumentException("No values in the response");
        }

        responseProcessor.process(responseData, requestContext, sessionContext);
        reportActivity(sessionContext.getSessionInfo());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `configureResponseDataMappers` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void configureResponseDataMappers() {
        responseDataMappers.put(SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST, (pdus, requestContext) -> {
            JsonObject responseData = new JsonObject();
            pduService.processPdus(pdus).forEach((oid, value) -> {
                requestContext.getResponseMappings().stream()
                        .filter(snmpMapping -> snmpMapping.getOid().equals(oid.toDottedString()))
                        .findFirst()
                        .ifPresent(snmpMapping -> {
                            pduService.processValue(snmpMapping.getKey(), snmpMapping.getDataType(), value, responseData);
                        });
            });
            return responseData;
        });

        ResponseDataMapper defaultResponseDataMapper = (pdus, requestContext) -> {
            return pduService.processPdus(pdus, requestContext.getResponseMappings());
        };
        Arrays.stream(SnmpCommunicationSpec.values())
                .forEach(communicationSpec -> {
                    responseDataMappers.putIfAbsent(communicationSpec, defaultResponseDataMapper);
                });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `configureResponseProcessors` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void configureResponseProcessors() {
        responseProcessors.put(SnmpCommunicationSpec.TELEMETRY_QUERYING, (responseData, requestContext, sessionContext) -> {
            TransportProtos.PostTelemetryMsg postTelemetryMsg = JsonConverter.convertToTelemetryProto(responseData);
            transportService.process(sessionContext.getSessionInfo(), postTelemetryMsg, null);
            log.debug("Posted telemetry for SNMP device {}: {}", sessionContext.getDeviceId(), responseData);
        });

        responseProcessors.put(SnmpCommunicationSpec.CLIENT_ATTRIBUTES_QUERYING, (responseData, requestContext, sessionContext) -> {
            TransportProtos.PostAttributeMsg postAttributesMsg = JsonConverter.convertToAttributesProto(responseData);
            transportService.process(sessionContext.getSessionInfo(), postAttributesMsg, null);
            log.debug("Posted attributes for SNMP device {}: {}", sessionContext.getDeviceId(), responseData);
        });

        responseProcessors.put(SnmpCommunicationSpec.TO_DEVICE_RPC_REQUEST, (responseData, requestContext, sessionContext) -> {
            TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                    .setRequestId(requestContext.getRequestId())
                    .setPayload(JsonConverter.toJson(responseData))
                    .build();
            transportService.process(sessionContext.getSessionInfo(), rpcResponseMsg, null);
            log.debug("Posted RPC response {} for device {}", responseData, sessionContext.getDeviceId());
        });

        responseProcessors.put(SnmpCommunicationSpec.TO_SERVER_RPC_REQUEST, (responseData, requestContext, sessionContext) -> {
            TransportProtos.ToServerRpcRequestMsg toServerRpcRequestMsg = TransportProtos.ToServerRpcRequestMsg.newBuilder()
                    .setRequestId(0)
                    .setMethodName(requestContext.getMethod().name())
                    .setParams(JsonConverter.toJson(responseData))
                    .build();
            transportService.process(sessionContext.getSessionInfo(), toServerRpcRequestMsg, null);
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reportActivity` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        transportService.recordActivity(sessionInfo);
    }


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getName` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getName() {
        return DataConstants.SNMP_TRANSPORT_NAME;
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `shutdown` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void shutdown() {
        log.info("Stopping SNMP transport!");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        if (snmp != null) {
            try {
                snmp.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.info("SNMP transport stopped!");
    }

    @Data
    /**
     * 中文说明：
     * 1. 类目的：`RequestContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private static class RequestContext {
        /**
         * 字段说明：
         * 1. 保存 `requestId` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final Integer requestId;
        private final SnmpCommunicationSpec communicationSpec;
        /**
         * 字段说明：
         * 1. 保存 `method` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final SnmpMethod method;
        private final List<SnmpMapping> responseMappings;

        /**
         * 字段说明：
         * 1. 保存 `requestSize` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final int requestSize;
        private List<PDU> responseParts;

        @Builder
        /**
         * 方法说明：
         * 1. 职责：执行 `RequestContext` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public RequestContext(Integer requestId, SnmpCommunicationSpec communicationSpec, SnmpMethod method, List<SnmpMapping> responseMappings, int requestSize) {
            this.requestId = requestId;
            this.communicationSpec = communicationSpec;
            this.method = method;
            this.responseMappings = responseMappings;
            this.requestSize = requestSize;
            if (requestSize > 1) {
                this.responseParts = Collections.synchronizedList(new ArrayList<>());
            }
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`ResponseDataMapper` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private interface ResponseDataMapper {
        /**
         * 方法说明：
         * 1. 职责：执行 `map` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        JsonObject map(List<PDU> pdus, RequestContext requestContext);
    }

    /**
     * 中文说明：
     * 1. 类目的：`ResponseProcessor` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private interface ResponseProcessor {
        /**
         * 方法说明：
         * 1. 职责：执行 `process` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        void process(JsonObject responseData, RequestContext requestContext, DeviceSessionContext sessionContext);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SnmpTransportService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
