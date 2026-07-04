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
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.BOOTSTRAP;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

@Slf4j
@Service("LwM2MBootstrapSecurityStore")
@TbLwM2mBootstrapTransportComponent
/**
 * 中文说明：
 * 1. 类目的：`LwM2MBootstrapSecurityStore` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2MBootstrapSecurityStore implements BootstrapSecurityStore {

    /**
     * 字段说明：
     * 1. 保存 `bootstrapConfigStore` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final EditableBootstrapConfigStore bootstrapConfigStore;

    /**
     * 字段说明：
     * 1. 保存 `lwM2MCredentialsSecurityInfoValidator` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator;

    /**
     * 字段说明：
     * 1. 保存 `context` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2mTransportContext context;
    private final LwM2mTransportServerHelper helper;
    private final Map<String /* endpoint */, TransportProtos.SessionInfoProto> bsSessions = new ConcurrentHashMap<>();

    /**
     * 方法说明：
     * 1. 职责：执行 `LwM2MBootstrapSecurityStore` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2MBootstrapSecurityStore(EditableBootstrapConfigStore bootstrapConfigStore, LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator, LwM2mTransportContext context, LwM2mTransportServerHelper helper) {
        this.bootstrapConfigStore = bootstrapConfigStore;
        this.lwM2MCredentialsSecurityInfoValidator = lwM2MCredentialsSecurityInfoValidator;
        this.context = context;
        this.helper = helper;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getAllByEndpoint` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endpoint, BOOTSTRAP);
            SecurityInfo securityInfo = this.addValueToStore(store, endpoint);
            return securityInfo == null ? null : Collections.singletonList(store.getSecurityInfo()).iterator();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getByIdentity` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SecurityInfo getByIdentity(String identity) {
        try {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(identity, BOOTSTRAP);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
                /* add value to store  from BootstrapJson */
                this.setBootstrapConfigSecurityInfo(store);
                BootstrapConfig bsConfig = store.getBootstrapConfig();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (bsConfig.security != null) {
                    try {
                        bootstrapConfigStore.add(store.getEndpoint(), bsConfig);
                    // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                    } catch (InvalidConfigurationException e) {
                        log.trace("Invalid Bootstrap Configuration", e);
                        return null;
                    }
                }
            }
            return store.getSecurityInfo();
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (LwM2MAuthException e) {
            log.trace("Bootstrap Registration failed: No pre-shared key found for [identity: {}]", identity);
            return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getX509ByEndpoint` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbLwM2MSecurityInfo getX509ByEndpoint(String endPoint) {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endPoint, BOOTSTRAP);
            this.addValueToStore(store, store.getEndpoint());
            return store;
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `setBootstrapConfigSecurityInfo` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setBootstrapConfigSecurityInfo(TbLwM2MSecurityInfo store) {
        /* BootstrapConfig */
        LwM2MBootstrapConfig lwM2MBootstrapConfig = this.getParametersBootstrap(store);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (lwM2MBootstrapConfig != null) {
            BootstrapConfig bootstrapConfig = lwM2MBootstrapConfig.getLwM2MBootstrapConfig();
            store.setBootstrapConfig(bootstrapConfig);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getParametersBootstrap` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private LwM2MBootstrapConfig getParametersBootstrap(TbLwM2MSecurityInfo store) {
        LwM2MBootstrapConfig lwM2MBootstrapConfig = store.getBootstrapCredentialConfig();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (lwM2MBootstrapConfig != null) {
            UUID sessionUUiD = UUID.randomUUID();
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            TransportProtos.SessionInfoProto sessionInfo = helper.getValidateSessionInfo(store.getMsg(), sessionUUiD.getMostSignificantBits(), sessionUUiD.getLeastSignificantBits());
            bsSessions.put(store.getEndpoint(), sessionInfo);
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            context.getTransportService().registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(null, null, null, sessionInfo, context.getTransportService()));
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (this.getValidatedSecurityMode(lwM2MBootstrapConfig)) {
                return lwM2MBootstrapConfig;
            } else {
                log.error(" [{}] Different values SecurityMode between of client and profile.", store.getEndpoint());
                log.error("{} getParametersBootstrap: [{}] Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR, store.getEndpoint());
                String logMsg = String.format("%s: Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR);
                helper.sendParametersOnThingsboardTelemetry(helper.getKvStringtoThingsboard(LOG_LWM2M_TELEMETRY, logMsg), sessionInfo);
                return null;
            }
        }
        log.error("Unable to decode Json or Certificate for [{}]", store.getEndpoint());
        return null;
    }

    /**
     * Bootstrap security have to sync between (bootstrapServer in credential and  bootstrapServer in profile)
     * and (lwm2mServer  in credential and lwm2mServer  in profile
     *
     * @return false if not sync between SecurityMode of Bootstrap credential and profile
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `getValidatedSecurityMode` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean getValidatedSecurityMode(LwM2MBootstrapConfig lwM2MBootstrapConfig) {
        LwM2MSecurityMode bootstrapServerSecurityMode = lwM2MBootstrapConfig.getBootstrapServer().getSecurityMode();
        LwM2MSecurityMode lwm2mServerSecurityMode = lwM2MBootstrapConfig.getLwm2mServer().getSecurityMode();
        AtomicBoolean validBs = new AtomicBoolean(true);
        AtomicBoolean validLw = new AtomicBoolean(true);
        lwM2MBootstrapConfig.getServerConfiguration().forEach(serverCredential -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (((AbstractLwM2MBootstrapServerCredential) serverCredential).isBootstrapServerIs()) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!bootstrapServerSecurityMode.equals(serverCredential.getSecurityMode())) {
                    validBs.set(false);
                }
            } else {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!lwm2mServerSecurityMode.equals(serverCredential.getSecurityMode())) {
                    validLw.set(false);
                }
            }
        });
        return validBs.get() && validLw.get();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getSessionByEndpoint` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TransportProtos.SessionInfoProto getSessionByEndpoint(String endpoint) {
        return bsSessions.get(endpoint);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `removeSessionByEndpoint` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TransportProtos.SessionInfoProto removeSessionByEndpoint(String endpoint) {
        return bsSessions.remove(endpoint);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getBootstrapConfigByEndpoint` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapConfig getBootstrapConfigByEndpoint(String endpoint) {
        return bootstrapConfigStore.getAll().get(endpoint);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addValueToStore` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SecurityInfo addValueToStore(TbLwM2MSecurityInfo store, String endpoint) {
        /* add value to store  from BootstrapJson */
        SecurityInfo securityInfo = null;
        if (store != null && store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
            securityInfo = store.getSecurityInfo();
            this.setBootstrapConfigSecurityInfo(store);
            BootstrapConfig bsConfigNew = store.getBootstrapConfig();
            if (bsConfigNew != null) {
                try {
                    boolean bootstrapServerUpdateEnable = ((Lwm2mDeviceProfileTransportConfiguration) store.getDeviceProfile().getProfileData().getTransportConfiguration()).isBootstrapServerUpdateEnable();
                    if (!bootstrapServerUpdateEnable) {
                        Optional<Map.Entry<Integer, BootstrapConfig.ServerSecurity>> securities = bsConfigNew.security.entrySet().stream().filter(sec -> sec.getValue().bootstrapServer).findAny();
                        if (securities.isPresent()) {
                            bsConfigNew.security.entrySet().remove(securities.get());
                            int serverSortId = securities.get().getValue().serverId;
                            Optional<Map.Entry<Integer, BootstrapConfig.ServerConfig>> serverConfigs = bsConfigNew.servers.entrySet().stream().filter(serv -> (serv.getValue()).shortId == serverSortId).findAny();
                            if (serverConfigs.isPresent()) {
                                bsConfigNew.servers.entrySet().remove(serverConfigs.get());
                            }
                        }
                    }
                    for (String config : bootstrapConfigStore.getAll().keySet()) {
                        if (config.equals(endpoint)) {
                            bootstrapConfigStore.remove(config);
                        }
                    }
                    bootstrapConfigStore.add(endpoint, bsConfigNew);
                } catch (InvalidConfigurationException e) {
                    if (e.getMessage().contains("Psk identity") && e.getMessage().contains("already used for this bootstrap server")) {
                        log.trace("Invalid Bootstrap Configuration", e);
                    } else {
                        log.error("Invalid Bootstrap Configuration", e);
                    }
                }
            }
        }
        return securityInfo;
    }

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MBootstrapSecurityStore` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}