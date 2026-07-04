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
package org.thingsboard.server.transport.lwm2m.server.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MModelConfigStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbLwM2mTransportComponent
/**
 * 中文说明：
 * 1. 类目的：`LwM2MModelConfigServiceImpl` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2MModelConfigServiceImpl implements LwM2MModelConfigService {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `modelStore` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    TbLwM2MModelConfigStore modelStore;

    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `downlinkMsgHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2mDownlinkMsgHandler downlinkMsgHandler;
    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `uplinkMsgHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2mUplinkMsgHandler uplinkMsgHandler;
    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `clientContext` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2mClientContext clientContext;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `logService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private LwM2MTelemetryLogService logService;

    /**
     * 字段说明：
     * 1. 保存 `currentModelConfigs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    ConcurrentMap<String, LwM2MModelConfig> currentModelConfigs;

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
        List<LwM2MModelConfig> models = modelStore.getAll();
        log.debug("Fetched model configs: {}", models);
        currentModelConfigs = models.stream()
                .collect(Collectors.toConcurrentMap(LwM2MModelConfig::getEndpoint, m -> m, (existing, replacement) -> existing));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendUpdates` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendUpdates(LwM2mClient lwM2mClient) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(lwM2mClient.getEndpoint());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (modelConfig == null || modelConfig.isEmpty()) {
            return;
        }

        doSend(lwM2mClient, modelConfig);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendUpdates` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void sendUpdates(LwM2mClient lwM2mClient, LwM2MModelConfig newModelConfig) {
        String endpoint = lwM2mClient.getEndpoint();
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (modelConfig == null || modelConfig.isEmpty()) {
            modelConfig = newModelConfig;
            currentModelConfigs.put(endpoint, modelConfig);
        } else {
            modelConfig.merge(newModelConfig);
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (lwM2mClient.isAsleep()) {
            modelStore.put(modelConfig);
        } else {
            doSend(lwM2mClient, modelConfig);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `doSend` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void doSend(LwM2mClient lwM2mClient, LwM2MModelConfig modelConfig) {
        log.trace("Send LwM2M Model updates: [{}]", modelConfig);

        String endpoint = lwM2mClient.getEndpoint();

        Map<String, ObjectAttributes> attrToAdd = modelConfig.getAttributesToAdd();
        attrToAdd.forEach((id, attributes) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                    .attributes(attributes)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToAdd.remove(id);
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> attrToRemove = modelConfig.getAttributesToRemove();
        attrToRemove.forEach((id) -> {
            TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(id)
                    .attributes(new ObjectAttributes())
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendWriteAttributesRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        attrToRemove.remove(id);
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MWriteAttributesCallback(logService, lwM2mClient, id))
            );
        });

        Set<String> toRead = modelConfig.getToRead();
        toRead.forEach(id -> {
            TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendReadRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toRead.remove(id);
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MReadCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toObserve = modelConfig.getToObserve();
        toObserve.forEach(id -> {
            TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toObserve.remove(id);
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MObserveCallback(uplinkMsgHandler, logService, lwM2mClient, id))
            );
        });

        Set<String> toCancelObserve = modelConfig.getToCancelObserve();
        toCancelObserve.forEach(id -> {
            TbLwM2MCancelObserveRequest request = TbLwM2MCancelObserveRequest.builder().versionedId(id)
                    .timeout(clientContext.getRequestTimeout(lwM2mClient)).build();
            downlinkMsgHandler.sendCancelObserveRequest(lwM2mClient, request,
                    createDownlinkProxyCallback(() -> {
                        toCancelObserve.remove(id);
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (modelConfig.isEmpty()) {
                            modelStore.remove(endpoint);
                        }
                    }, new TbLwM2MCancelObserveCallback(logService, lwM2mClient, id))
            );
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDownlinkProxyCallback` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <R, T> DownlinkRequestCallback<R, T> createDownlinkProxyCallback(Runnable processRemove, DownlinkRequestCallback<R, T> callback) {
        return new DownlinkRequestCallback<>() {
            @Override
            public void onSuccess(R request, T response) {
                processRemove.run();
                callback.onSuccess(request, response);
            }

            @Override
            public void onValidationError(String params, String msg) {
                processRemove.run();
                callback.onValidationError(params, msg);
            }

            @Override
            public void onError(String params, Exception e) {
                try {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (e instanceof TimeoutException) {
                        return;
                    }
                    processRemove.run();
                } finally {
                    callback.onError(params, e);
                }
            }

        };
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `persistUpdates` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void persistUpdates(String endpoint) {
        LwM2MModelConfig modelConfig = currentModelConfigs.get(endpoint);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (modelConfig != null && !modelConfig.isEmpty()) {
            modelStore.put(modelConfig);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `removeUpdates` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void removeUpdates(String endpoint) {
        currentModelConfigs.remove(endpoint);
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `destroy` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void destroy() {
        currentModelConfigs.values().forEach(model -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (model != null && !model.isEmpty()) {
                modelStore.put(model);
            }
        });
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MModelConfigServiceImpl` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
