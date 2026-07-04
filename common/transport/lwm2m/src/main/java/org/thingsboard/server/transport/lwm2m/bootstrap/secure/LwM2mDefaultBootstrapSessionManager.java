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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSession;
import org.eclipse.leshan.server.bootstrap.DefaultBootstrapSessionManager;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.model.LwM2mBootstrapModelProvider;
import org.eclipse.leshan.server.model.StandardBootstrapModelProvider;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapConfigStoreTaskProvider;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapTaskProvider;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`LwM2mDefaultBootstrapSessionManager` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2mDefaultBootstrapSessionManager extends DefaultBootstrapSessionManager {

    /**
     * 字段说明：
     * 1. 保存 `bsSecurityStore` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final BootstrapSecurityStore bsSecurityStore;
    private final SecurityChecker securityChecker;
    /**
     * 字段说明：
     * 1. 保存 `tasksProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final LwM2MBootstrapTaskProvider tasksProvider;
    private final LwM2mBootstrapModelProvider modelProvider;
    /**
     * 字段说明：
     * 1. 保存 `transportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TransportService transportService;

    /**
     * Create a {@link DefaultBootstrapSessionManager} using a default {@link SecurityChecker} to accept or refuse new
     * {@link BootstrapSession}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by default {@link SecurityChecker}.
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `LwM2mDefaultBootstrapSessionManager` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, BootstrapConfigStore configStore, TransportService transportService) {
        this(bsSecurityStore, new SecurityChecker(), new LwM2MBootstrapConfigStoreTaskProvider(configStore),
                new StandardBootstrapModelProvider());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        this.transportService = transportService;
    }

    /**
     * Create a {@link DefaultBootstrapSessionManager}.
     *
     * @param bsSecurityStore the {@link BootstrapSecurityStore} used by {@link SecurityChecker}.
     * @param securityChecker used to accept or refuse new {@link BootstrapSession}.
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `LwM2mDefaultBootstrapSessionManager` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2mDefaultBootstrapSessionManager(BootstrapSecurityStore bsSecurityStore, SecurityChecker securityChecker,
                                               LwM2MBootstrapTaskProvider tasksProvider, LwM2mBootstrapModelProvider modelProvider) {
        super(bsSecurityStore, securityChecker, tasksProvider, modelProvider);
        this.bsSecurityStore = bsSecurityStore;
        this.securityChecker = securityChecker;
        this.tasksProvider = tasksProvider;
        this.modelProvider = modelProvider;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `begin` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity) {
        boolean authorized = true;
        Iterator<SecurityInfo> securityInfos = null;
        try {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (bsSecurityStore != null && securityChecker != null) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (clientIdentity.isPSK()) {
                    SecurityInfo securityInfo = bsSecurityStore.getByIdentity(clientIdentity.getPskIdentity());
                    securityInfos = Collections.singletonList(securityInfo).iterator();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                } else if (!clientIdentity.isX509()) {
                    securityInfos = bsSecurityStore.getAllByEndpoint(request.getEndpointName());
                }
                authorized = this.checkSecurityInfo(request.getEndpointName(), clientIdentity, securityInfos);
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (LwM2MAuthException e) {
            authorized = false;
        }
        DefaultBootstrapSession session = new DefaultBootstrapSession(request, clientIdentity, authorized);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (authorized) {
            try {
                this.tasksProvider.put(session.getEndpoint());
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (InvalidConfigurationException e){
                log.error("Failed put to lwM2MBootstrapSessionClients by endpoint [{}]", request.getEndpointName(), e);
            }
            this.sendLogs(request.getEndpointName(),
                    String.format("%s: Bootstrap session started...", LOG_LWM2M_INFO, request.getEndpointName()));
        }
        return session;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `hasConfigFor` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean hasConfigFor(BootstrapSession session) {
        BootstrapTaskProvider.Tasks firstTasks = this.tasksProvider.getTasks(session, null);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (firstTasks == null) {
            return false;
        }
        initTasks(session, firstTasks);
        return true;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initTasks` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void initTasks(BootstrapSession bssession, BootstrapTaskProvider.Tasks tasks) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bssession;
        // set models
        if (tasks.supportedObjects != null)
            session.setModel(modelProvider.getObjectModel(session, tasks.supportedObjects));

        // set Requests to Send
        log.info("tasks.requestsToSend = [{}]", tasks.requestsToSend);
        session.setRequests(tasks.requestsToSend);

        // prepare list where we will store Responses
        session.setResponses(new ArrayList<LwM2mResponse>(tasks.requestsToSend.size()));

        // is last Tasks ?
        session.setMoreTasks(!tasks.last);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getFirstRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapDownlinkRequest<? extends LwM2mResponse> getFirstRequest(BootstrapSession bsSession) {
        return nextRequest(bsSession);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `nextRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected BootstrapDownlinkRequest<? extends LwM2mResponse> nextRequest(BootstrapSession bsSession) {
        DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsToSend = session.getRequests();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!requestsToSend.isEmpty()) {
            // get next requests
            return requestsToSend.remove(0);
        } else {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (session.hasMoreTasks()) {
                BootstrapTaskProvider.Tasks nextTasks = this.tasksProvider.getTasks(session, session.getResponses());
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (nextTasks == null) {
                    session.setMoreTasks(false);
                    return new BootstrapFinishRequest();
                }

                initTasks(session, nextTasks);
                return nextRequest(bsSession);
            } else {
                return new BootstrapFinishRequest();
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onResponseSuccess` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapPolicy onResponseSuccess(BootstrapSession bsSession,
                                             BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            String msg = String.format("%s: receives success response for:  %s  %s %s", LOG_LWM2M_INFO,
                    request.getClass().getSimpleName(), request.getPath().toString(), response.toString());
            this.sendLogs(bsSession.getEndpoint(), msg);

            // on success for NOT bootstrap finish request we send next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on success for bootstrap finish request we stop the session
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: receives success response for bootstrap finish.", LOG_LWM2M_INFO));
            this.tasksProvider.remove(bsSession.getEndpoint());
            return BootstrapPolicy.finished();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onResponseError` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapPolicy onResponseError(BootstrapSession bsSession,
                                           BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
        if (!(request instanceof BootstrapFinishRequest)) {
            // store response
            DefaultBootstrapSession session = (DefaultBootstrapSession) bsSession;
            session.getResponses().add(response);
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: %s %s receives error response %s ", LOG_LWM2M_INFO,
                            request.getClass().getSimpleName(),
                            request.getPath().toString(), response.toString()));
            // on response error for NOT bootstrap finish request we continue any sending next request
            return BootstrapPolicy.continueWith(nextRequest(bsSession));
        } else {
            // on response error for bootstrap finish request we stop the session
            this.sendLogs(bsSession.getEndpoint(),
                    String.format("%s: error response for request bootstrap finish. Stop the session: %s", LOG_LWM2M_ERROR, bsSession.toString()));
            this.tasksProvider.remove(bsSession.getEndpoint());
            return BootstrapPolicy.failed();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onRequestFailure` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapPolicy onRequestFailure(BootstrapSession bsSession,
                                            BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
        this.sendLogs(bsSession.getEndpoint(),
                String.format("%s: %s %s failed because of %s", LOG_LWM2M_ERROR, request.getClass().getSimpleName(),
                        request.getPath().toString(), cause.toString()));
        return BootstrapPolicy.failed();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `end` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void end(BootstrapSession bsSession) {
        this.sendLogs(bsSession.getEndpoint(), String.format("%s: Bootstrap session finished.", LOG_LWM2M_INFO));
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `failed` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
        this.sendLogs(bsSession.getEndpoint(), String.format("%s: Bootstrap session failed because of %s", LOG_LWM2M_ERROR,
                cause.toString()));
        this.tasksProvider.remove(bsSession.getEndpoint());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendLogs` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void sendLogs(String endpointName, String logMsg) {
        log.info("Endpoint: [{}] [{}]", endpointName, logMsg);
        transportService.log(((LwM2MBootstrapSecurityStore) bsSecurityStore).getSessionByEndpoint(endpointName), logMsg);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkSecurityInfo` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean checkSecurityInfo(String endpoint, Identity clientIdentity, Iterator<SecurityInfo> securityInfos) {
        if (clientIdentity.isX509()) {
            return clientIdentity.getX509CommonName().equals(endpoint)
                    & ((LwM2MBootstrapSecurityStore) bsSecurityStore).getBootstrapConfigByEndpoint(endpoint) != null;
        } else {
            return securityChecker.checkSecurityInfos(endpoint, clientIdentity, securityInfos);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mDefaultBootstrapSessionManager` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
