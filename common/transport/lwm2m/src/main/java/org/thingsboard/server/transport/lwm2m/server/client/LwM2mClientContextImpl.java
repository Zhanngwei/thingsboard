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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfigService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.session.LwM2MSessionManager;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MClientStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`LwM2mClientContextImpl` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2mClientContextImpl implements LwM2mClientContext {

    /**
     * 字段说明：
     * 1. 保存 `context` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    /**
     * 字段说明：
     * 1. 保存 `securityStore` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbMainSecurityStore securityStore;
    private final TbLwM2MClientStore clientStore;
    /**
     * 字段说明：
     * 1. 保存 `sessionManager` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2MSessionManager sessionManager;
    private final TransportDeviceProfileCache deviceProfileCache;
    /**
     * 字段说明：
     * 1. 保存 `modelConfigService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2MModelConfigService modelConfigService;

    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `defaultLwM2MUplinkMsgHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2mUplinkMsgHandler defaultLwM2MUplinkMsgHandler;
    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `otaUpdateService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2MOtaUpdateService otaUpdateService;

    private final Map<String, LwM2mClient> lwM2mClientsByEndpoint = new ConcurrentHashMap<>();
    private final Map<String, LwM2mClient> lwM2mClientsByRegistrationId = new ConcurrentHashMap<>();
    private final Map<UUID, Lwm2mDeviceProfileTransportConfiguration> profiles = new ConcurrentHashMap<>();

    @AfterStartUp(order = AfterStartUp.BEFORE_TRANSPORT_SERVICE)
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
    public void init() {
        String nodeId = context.getNodeId();
        Set<LwM2mClient> fetchedClients = clientStore.getAll();
        log.debug("Fetched clients from store: {}", fetchedClients);
        fetchedClients.forEach(client -> {
            lwM2mClientsByEndpoint.put(client.getEndpoint(), client);
            try {
                client.lock();
                updateFetchedClient(nodeId, client);
            } finally {
                client.unlock();
            }
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getClientByEndpoint` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2mClient getClientByEndpoint(String endpoint) {
        return lwM2mClientsByEndpoint.computeIfAbsent(endpoint, ep -> {
            LwM2mClient client = clientStore.get(ep);
            String nodeId = context.getNodeId();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (client == null) {
                log.info("[{}] initialized new client.", endpoint);
                client = new LwM2mClient(nodeId, ep);
            } else {
                log.debug("[{}] fetched client from store: {}", endpoint, client);
                updateFetchedClient(nodeId, client);
            }
            return client;
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateFetchedClient` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void updateFetchedClient(String nodeId, LwM2mClient client) {
        boolean updated = false;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getRegistration() != null) {
            lwM2mClientsByRegistrationId.put(client.getRegistration().getId(), client);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (client.getSession() != null) {
            client.refreshSessionId(nodeId);
            sessionManager.register(client.getSession());
            updated = true;
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (updated) {
            clientStore.put(client);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `register` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Optional<TransportProtos.SessionInfoProto> register(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportProtos.SessionInfoProto oldSession;
        client.lock();
        try {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (LwM2MClientState.UNREGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            oldSession = client.getSession();
            TbLwM2MSecurityInfo securityInfo = securityStore.getTbLwM2MSecurityInfoByEndpoint(client.getEndpoint());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (securityInfo.getSecurityMode() != null) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (SecurityMode.X509.equals(securityInfo.getSecurityMode())) {
                    securityStore.registerX509(registration.getEndpoint(), registration.getId());
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (securityInfo.getDeviceProfile() != null) {
                    profileUpdate(securityInfo.getDeviceProfile());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (securityInfo.getSecurityInfo() != null) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    } else if (NO_SEC.equals(securityInfo.getSecurityMode())) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    } else {
                        throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                    }
                } else {
                    throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                }
            } else {
                throw new RuntimeException(String.format("Registration failed: FORBIDDEN, endpointId: %s", client.getEndpoint()));
            }
            client.setRegistration(registration);
            this.lwM2mClientsByRegistrationId.put(registration.getId(), client);
            client.setState(LwM2MClientState.REGISTERED);
            onUplink(client);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!compareAndSetSleepFlag(client, false)) {
                clientStore.put(client);
            }
        } finally {
            client.unlock();
        }
        return Optional.ofNullable(oldSession);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `asleep` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean asleep(LwM2mClient client) {
        boolean changed = compareAndSetSleepFlag(client, true);
        if (changed) {
            log.debug("[{}] client is sleeping", client.getEndpoint());
            context.getTransportService().log(client.getSession(), "Info : Client is sleeping!");
        }
        return changed;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `awake` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean awake(LwM2mClient client) {
        onUplink(client);
        boolean changed = compareAndSetSleepFlag(client, false);
        if (changed) {
            log.debug("[{}] client is awake", client.getEndpoint());
            context.getTransportService().log(client.getSession(), "Info : Client is awake!");
            sendMsgsAfterSleeping(client);
        }
        return changed;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `compareAndSetSleepFlag` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean compareAndSetSleepFlag(LwM2mClient client, boolean sleeping) {
        if (sleeping == client.isAsleep()) {
            log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getEndpoint(), client.isAsleep(), sleeping);
            return false;
        }
        client.lock();
        try {
            if (sleeping == client.isAsleep()) {
                log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getEndpoint(), client.isAsleep(), sleeping);
                return false;
            } else {
                PowerMode powerMode = getPowerMode(client);
                if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                    log.trace("[{}] Switch sleeping from: {} to: {}", client.getEndpoint(), client.isAsleep(), sleeping);
                    client.setAsleep(sleeping);
                    update(client);
                    return true;
                } else {
                    return false;
                }
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateRegistration` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void updateRegistration(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            client.setRegistration(registration);
            if (!awake(client)) {
                clientStore.put(client);
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `unregister` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void unregister(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            lwM2mClientsByRegistrationId.remove(registration.getId());
            Registration currentRegistration = client.getRegistration();
            if (currentRegistration.getId().equals(registration.getId())) {
                client.setState(LwM2MClientState.UNREGISTERED);
                lwM2mClientsByEndpoint.remove(client.getEndpoint());
//                TODO: change tests to use new certificate.
//                this.securityStore.remove(client.getEndpoint(), registration.getId());
                clientStore.remove(client.getEndpoint());
                modelConfigService.removeUpdates(client.getEndpoint());
                UUID profileId = client.getProfileId();
                if (profileId != null) {
                    Optional<LwM2mClient> otherClients = lwM2mClientsByRegistrationId.values().stream().filter(e -> e.getProfileId().equals(profileId)).findFirst();
                    if (otherClients.isEmpty()) {
                        profiles.remove(profileId);
                    }
                }
            } else {
                throw new LwM2MClientStateException(client.getState(), "Client has different registration.");
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getClientBySessionInfo` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2mClient = null;
        UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
        Predicate<LwM2mClient> isClientFilter =
                c -> c.getSession() != null && sessionId.equals((new UUID(c.getSession().getSessionIdMSB(), c.getSession().getSessionIdLSB())));
        if (this.lwM2mClientsByEndpoint.size() > 0) {
            lwM2mClient = this.lwM2mClientsByEndpoint.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null && this.lwM2mClientsByRegistrationId.size() > 0) {
            lwM2mClient = this.lwM2mClientsByRegistrationId.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null) {
            log.error("[{}] Failed to lookup client by session id.", sessionId);
        }
        return lwM2mClient;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getObjectIdByKeyNameFromProfile` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public String getObjectIdByKeyNameFromProfile(LwM2mClient client, String keyName) {
        Lwm2mDeviceProfileTransportConfiguration profile = getProfile(client.getProfileId());
        for (Map.Entry<String, String> entry : profile.getObserveAttr().getKeyName().entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v.equals(keyName) && client.isValidObjectVersion(k).isEmpty()) {
                return k;
            }
        }
        throw new IllegalArgumentException(keyName + " is not configured in the device profile!");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getRegistration` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Registration getRegistration(String registrationId) {
        return this.lwM2mClientsByRegistrationId.get(registrationId).getRegistration();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `registerClient` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials) {
        LwM2mClient client = getClientByEndpoint(registration.getEndpoint());
        client.init(credentials, UUID.randomUUID());
        lwM2mClientsByRegistrationId.put(registration.getId(), client);
        profileUpdate(credentials.getDeviceProfile());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `update` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void update(LwM2mClient client) {
        client.lock();
        try {
            if (client.getState().equals(LwM2MClientState.REGISTERED)) {
                clientStore.put(client);
            } else {
                log.error("[{}] Client is in invalid state: {}!", client.getEndpoint(), client.getState());
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendMsgsAfterSleeping` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendMsgsAfterSleeping(LwM2mClient lwM2MClient) {
        if (LwM2MClientState.REGISTERED.equals(lwM2MClient.getState())) {
            PowerMode powerMode = getPowerMode(lwM2MClient);
            if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                modelConfigService.sendUpdates(lwM2MClient);
                defaultLwM2MUplinkMsgHandler.initAttributes(lwM2MClient, false);
                TransportProtos.TransportToDeviceActorMsg persistentRpcRequestMsg = TransportProtos.TransportToDeviceActorMsg
                        .newBuilder()
                        .setSessionInfo(lwM2MClient.getSession())
                        .setSendPendingRPC(TransportProtos.SendPendingRPCMsg.newBuilder().build())
                        .build();
                context.getTransportService().process(persistentRpcRequestMsg, TransportServiceCallback.EMPTY);
                otaUpdateService.init(lwM2MClient);
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getPowerMode` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private PowerMode getPowerMode(LwM2mClient lwM2MClient) {
        PowerMode powerMode = lwM2MClient.getPowerMode();
        if (powerMode == null) {
            Lwm2mDeviceProfileTransportConfiguration deviceProfile = getProfile(lwM2MClient.getProfileId());
            powerMode = deviceProfile.getClientLwM2mSettings().getPowerMode();
        }
        return powerMode;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getLwM2mClients` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Collection<LwM2mClient> getLwM2mClients() {
        return lwM2mClientsByEndpoint.values();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getProfile` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Lwm2mDeviceProfileTransportConfiguration getProfile(UUID profileId) {
        return doGetAndCache(profileId);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getProfile` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Lwm2mDeviceProfileTransportConfiguration getProfile(Registration registration) {
        UUID profileId = getClientByEndpoint(registration.getEndpoint()).getProfileId();
        return doGetAndCache(profileId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `doGetAndCache` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Lwm2mDeviceProfileTransportConfiguration doGetAndCache(UUID profileId) {

        Lwm2mDeviceProfileTransportConfiguration result = profiles.get(profileId);
        if (result == null) {
            log.debug("Fetching profile [{}]", profileId);
            DeviceProfile deviceProfile = deviceProfileCache.get(new DeviceProfileId(profileId));
            if (deviceProfile != null) {
                result = profileUpdate(deviceProfile);
            } else {
                log.warn("Device profile was not found! Most probably device profile [{}] has been removed from the database.", profileId);
            }
        }
        return result;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `profileUpdate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Lwm2mDeviceProfileTransportConfiguration profileUpdate(DeviceProfile deviceProfile) {
        Lwm2mDeviceProfileTransportConfiguration clientProfile = LwM2MTransportUtil.toLwM2MClientProfile(deviceProfile);
        profiles.put(deviceProfile.getUuidId(), clientProfile);
        return clientProfile;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getSupportedIdVerInClient` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Set<String> getSupportedIdVerInClient(LwM2mClient client) {
        Set<String> clientObjects = ConcurrentHashMap.newKeySet();
        Arrays.stream(client.getRegistration().getObjectLinks()).forEach(link -> {
            LwM2mPath pathIds = new LwM2mPath(link.getUriReference());
            if (!pathIds.isRoot()) {
                clientObjects.add(convertObjectIdToVersionedId(link.getUriReference(), client.getRegistration()));
            }
        });
        return (clientObjects.size() > 0) ? clientObjects : null;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getClientByDeviceId` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2mClient getClientByDeviceId(UUID deviceId) {
        return lwM2mClientsByRegistrationId.values().stream().filter(e -> deviceId.equals(e.getDeviceId())).findFirst().orElse(null);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `isDownlinkAllowed` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean isDownlinkAllowed(LwM2mClient client) {
        PowerMode powerMode = client.getPowerMode();
        OtherConfiguration profileSettings = null;
        if (powerMode == null && client.getProfileId() != null) {
            var clientProfile = getProfile(client.getProfileId());
            profileSettings = clientProfile.getClientLwM2mSettings();
            powerMode = profileSettings.getPowerMode();
            if (powerMode == null) {
                powerMode = PowerMode.DRX;
            }
        }
        if (powerMode == null || PowerMode.DRX.equals(powerMode) || otaUpdateService.isOtaDownloading(client)) {
            return true;
        }
        client.lock();
        long timeSinceLastUplink = System.currentTimeMillis() - client.getLastUplinkTime();
        try {
            if (PowerMode.PSM.equals(powerMode)) {
                Long psmActivityTimer = client.getPsmActivityTimer();
                if (psmActivityTimer == null && profileSettings != null) {
                    psmActivityTimer = profileSettings.getPsmActivityTimer();

                }
                if (psmActivityTimer == null || psmActivityTimer == 0L) {
                    psmActivityTimer = config.getPsmActivityTimer();
                }
                return timeSinceLastUplink <= psmActivityTimer;
            } else {
                Long pagingTransmissionWindow = client.getPagingTransmissionWindow();
                if (pagingTransmissionWindow == null && profileSettings != null) {
                    pagingTransmissionWindow = profileSettings.getPagingTransmissionWindow();

                }
                if (pagingTransmissionWindow == null || pagingTransmissionWindow == 0L) {
                    pagingTransmissionWindow = config.getPagingTransmissionWindow();
                }
                boolean allowed = timeSinceLastUplink <= pagingTransmissionWindow;
                if (!allowed) {
                    return client.checkFirstDownlink();
                } else {
                    return true;
                }
            }
        } finally {
            client.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onUplink` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onUplink(LwM2mClient client) {
        PowerMode powerMode = client.getPowerMode();
        OtherConfiguration profileSettings = null;
        if (powerMode == null && client.getProfileId() != null) {
            var clientProfile = getProfile(client.getProfileId());
            profileSettings = clientProfile.getClientLwM2mSettings();
            powerMode = profileSettings.getPowerMode();
            if (powerMode == null) {
                powerMode = PowerMode.DRX;
            }
        }
        if (powerMode == null || PowerMode.DRX.equals(powerMode)) {
            client.updateLastUplinkTime();
            return;
        }
        client.lock();
        try {
            long uplinkTime = client.updateLastUplinkTime();
            long timeout;
            if (PowerMode.PSM.equals(powerMode)) {
                Long psmActivityTimer = client.getPsmActivityTimer();
                if (psmActivityTimer == null && profileSettings != null) {
                    psmActivityTimer = profileSettings.getPsmActivityTimer();

                }
                if (psmActivityTimer == null || psmActivityTimer == 0L) {
                    psmActivityTimer = config.getPsmActivityTimer();
                }

                timeout = psmActivityTimer;
            } else {
                Long pagingTransmissionWindow = client.getPagingTransmissionWindow();
                if (pagingTransmissionWindow == null && profileSettings != null) {
                    pagingTransmissionWindow = profileSettings.getPagingTransmissionWindow();

                }
                if (pagingTransmissionWindow == null || pagingTransmissionWindow == 0L) {
                    pagingTransmissionWindow = config.getPagingTransmissionWindow();
                }
                timeout = pagingTransmissionWindow;
            }
            Future<Void> sleepTask = client.getSleepTask();
            if (sleepTask != null) {
                sleepTask.cancel(false);
            }
            Future<Void> task = context.getScheduler().schedule(() -> {
                if (uplinkTime == client.getLastUplinkTime() && !otaUpdateService.isOtaDownloading(client)) {
                    asleep(client);
                }
                return null;
            }, timeout, TimeUnit.MILLISECONDS);
            client.setSleepTask(task);
        } finally {
            client.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getRequestTimeout` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Long getRequestTimeout(LwM2mClient client) {
        Long timeout = null;
        if (PowerMode.E_DRX.equals(client.getPowerMode()) && client.getEdrxCycle() != null) {
            timeout = client.getEdrxCycle();
        } else {
            var clientProfile = getProfile(client.getProfileId());
            OtherConfiguration clientLwM2mSettings = clientProfile.getClientLwM2mSettings();
            if (PowerMode.E_DRX.equals(clientLwM2mSettings.getPowerMode())) {
                timeout = clientLwM2mSettings.getEdrxCycle();
            }
        }
        if (timeout == null || timeout == 0L) {
            timeout = this.config.getTimeout();
        }
        return timeout;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mClientContextImpl` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
