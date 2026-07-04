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
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapUtil;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.server.bootstrap.BootstrapUtil.toWriteRequest;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.BOOTSTRAP_DEFAULT_SHORT_ID;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`LwM2MBootstrapConfigStoreTaskProvider` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class LwM2MBootstrapConfigStoreTaskProvider implements LwM2MBootstrapTaskProvider {

    /**
     * 字段说明：
     * 1. 保存 `readWriteLock` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected final ReadWriteLock readWriteLock;
    protected final Lock writeLock;

    /**
     * 字段说明：
     * 1. 保存 `store` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private BootstrapConfigStore store;

    /**
     * 字段说明：
     * 1. 保存 `supportedObjects` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Map<Integer, String> supportedObjects;

    /**
     * Map<sEndpoint, LwM2MBootstrapClientInstanceIds: securityInstances, serverInstances>
     */
    /**
     * 字段说明：
     * 1. 保存 `lwM2MBootstrapSessionClients` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected Map<String, LwM2MBootstrapClientInstanceIds> lwM2MBootstrapSessionClients;

    /**
     * 方法说明：
     * 1. 职责：执行 `LwM2MBootstrapConfigStoreTaskProvider` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public LwM2MBootstrapConfigStoreTaskProvider(BootstrapConfigStore store) {
        this.store = store;
        this.lwM2MBootstrapSessionClients = new ConcurrentHashMap<>();
        readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getTasks` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponse) {
        BootstrapConfig config = store.get(session.getEndpoint(), session.getIdentity(), session);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (config == null) {
            return null;
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (previousResponse == null && shouldStartWithDiscover(config)) {
            Tasks tasks = new Tasks();
            tasks.requestsToSend = new ArrayList<>(1);
            tasks.requestsToSend.add(new BootstrapDiscoverRequest());
            tasks.last = false;
            return tasks;
        } else {
            Tasks tasks = new Tasks();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (this.supportedObjects == null) {
                initSupportedObjectsDefault();
            }
            // add supportedObjects
            tasks.supportedObjects = this.supportedObjects;
            // handle bootstrap discover response
            if (previousResponse != null) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (previousResponse.get(0) instanceof BootstrapDiscoverResponse) {
                    BootstrapDiscoverResponse discoverResponse = (BootstrapDiscoverResponse) previousResponse.get(0);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (discoverResponse.isSuccess()) {
                        this.initAfterBootstrapDiscover(discoverResponse);
                        findSecurityInstanceId(discoverResponse.getObjectLinks(), session.getEndpoint());
                    } else {
                        log.warn(
                                "Bootstrap Discover return error {} : to continue bootstrap session without autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID) == null) {
                        log.error(
                                "Unable to find bootstrap server instance in Security Object (0) in response {}: unable to continue bootstrap session with autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                        return null;
                    }
                    tasks.requestsToSend = new ArrayList<>(1);
                    tasks.requestsToSend.add(new BootstrapReadRequest("/1"));
                    tasks.last = false;
                    return tasks;
                }
                BootstrapReadResponse readResponse = (BootstrapReadResponse) previousResponse.get(0);
                Integer bootstrapServerIdOld = null;
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (readResponse.isSuccess()) {
                    findServerInstanceId(readResponse, session.getEndpoint());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().size() > 0 && this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getServerInstances().size() > 0) {
                        bootstrapServerIdOld = this.findBootstrapServerId(session.getEndpoint());
                    }
                } else {
                    log.warn(
                            "Bootstrap ReadResponse return error {} : to continue bootstrap session without find Server Instance Id. {}",
                            readResponse, session);
                }
                // create requests from config
                tasks.requestsToSend = this.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat(),
                        bootstrapServerIdOld, session.getEndpoint());
            } else {
                // create requests from config
                tasks.requestsToSend = BootstrapUtil.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat());
            }
            return tasks;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `shouldStartWithDiscover` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean shouldStartWithDiscover(BootstrapConfig config) {
        return config.autoIdForSecurityObject;
    }

    /**
     * "Short Server ID": This Resource MUST be set when the Bootstrap-Server Resource has a value of 'false'.
     * The values ID:0 and ID:65535 values MUST NOT be used for identifying the LwM2M Server.
     * "Short Server ID":
     * - Link Instance (lwm2m Server) hase linkParams with key = "ssid" value = "shortId" (ver lvm2m = 1.1).
     * - Link Instance (bootstrap Server) hase not linkParams with key = "ssid" (ver lvm2m = 1.1).
     */
    /**
     * 方法说明：
     * 1. 职责：执行 `findSecurityInstanceId` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void findSecurityInstanceId(Link[] objectLinks, String endpoint) {
        log.info("Object after discover: [{}]", objectLinks);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Link link : objectLinks) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (link.getUriReference().startsWith("/0/")) {
                try {
                    LwM2mPath path = new LwM2mPath(link.getUriReference());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (path.isObjectInstance()) {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (link.getLinkParams().containsKey("ssid")) {
                            int serverId = Integer.parseInt(link.getLinkParams().get("ssid").getUnquoted());
                            if (!lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(serverId)) {
                                lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(serverId, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid lwm2mSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                            lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(Integer.valueOf(link.getLinkParams().get("ssid").getUnquoted()), path.getObjectInstanceId());
                        } else {
                            if (!this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(0)) {
                                this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(BOOTSTRAP_DEFAULT_SHORT_ID, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid bootstrapSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore if this is not a LWM2M path
                    log.error("Invalid LwM2MPath starting by \"/0/\"");
                }
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `findServerInstanceId` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void findServerInstanceId(BootstrapReadResponse readResponse, String endpoint) {
        try {
            ((LwM2mObject) readResponse.getContent()).getInstances().values().forEach(instance -> {
                var shId = OPAQUE.equals(instance.getResource(0).getType()) ? new BigInteger((byte[]) instance.getResource(0).getValue()).intValue() : instance.getResource(0).getValue();
                int shortId;
                if (shId instanceof Long) {
                    shortId = ((Long) shId).intValue();
                } else {
                    shortId = (int) shId;
                }
                this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().put(shortId, instance.getId());
            });
        } catch (Exception e) {
            log.error("Failed find Server Instance Id. ", e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `findBootstrapServerId` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Integer findBootstrapServerId(String endpoint) {
        Integer bootstrapServerIdOld = null;
        Map<Integer, Integer> filteredMap = this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().entrySet()
                .stream().filter(x -> !this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(x.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (filteredMap.size() > 0) {
            bootstrapServerIdOld = filteredMap.keySet().stream().findFirst().get();
        }
        return bootstrapServerIdOld;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getStore` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BootstrapConfigStore getStore() {
        return this.store;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initAfterBootstrapDiscover` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void initAfterBootstrapDiscover(BootstrapDiscoverResponse response) {
        Link[] links = response.getObjectLinks();
        Arrays.stream(links).forEach(link -> {
            LwM2mPath path = new LwM2mPath(link.getUriReference());
            if (!path.isRoot() && path.getObjectId() < 3) {
                if (path.isObject()) {
                    String ver = link.getLinkParams().get("ver") != null ? link.getLinkParams().get("ver").getUnquoted() : "1.0";
                    this.supportedObjects.put(path.getObjectId(), ver);
                }
            }
        });
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `toRequests` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> toRequests(BootstrapConfig bootstrapConfig,
                                                                              ContentFormat contentFormat,
                                                                              Integer bootstrapServerIdOld,
                                                                              String endpoint) {
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = new ArrayList<>();
        Set<String> pathsDelete = new HashSet<>();
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsWrite = new ArrayList<>();
        boolean isBsServer = false;
        boolean isLwServer = false;
        /** Map<serverId ("Short Server ID"), InstanceId> */
        Map<Integer, Integer> instances = new HashMap<>();
        Integer bootstrapServerIdNew = null;
        // handle security
        int lwm2mSecurityInstanceId = 0;
        int bootstrapSecurityInstanceId = this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID);
        for (BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfig.security).values()) {
            if (security.bootstrapServer) {
                requestsWrite.add(toWriteRequest(bootstrapSecurityInstanceId, security, contentFormat));
                isBsServer = true;
                bootstrapServerIdNew = security.serverId;
                instances.put(security.serverId, bootstrapSecurityInstanceId);
            } else {
                if (lwm2mSecurityInstanceId == bootstrapSecurityInstanceId) {
                    lwm2mSecurityInstanceId++;
                }
                requestsWrite.add(toWriteRequest(lwm2mSecurityInstanceId, security, contentFormat));
                instances.put(security.serverId, lwm2mSecurityInstanceId);
                isLwServer = true;
                if (!isBsServer && this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(security.serverId) &&
                        lwm2mSecurityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId)) {
                    pathsDelete.add("/0/" + this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId));
                }
                /**
                 * If there is an instance in the serverInstances with serverId which we replace in the securityInstances
                 */
                // find serverId in securityInstances by id (instance)
                Integer serverIdOld = null;
                for (Map.Entry<Integer, Integer> entry : this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().entrySet()) {
                    if (entry.getValue().equals(lwm2mSecurityInstanceId)) {
                        serverIdOld = entry.getKey();
                    }
                }
                if (!isBsServer && serverIdOld != null && this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(serverIdOld)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(serverIdOld));
                }
                lwm2mSecurityInstanceId++;
            }
        }
        // handle server
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> server : bootstrapConfig.servers.entrySet()) {
            int securityInstanceId = instances.get(server.getValue().shortId);
            requestsWrite.add(toWriteRequest(securityInstanceId, server.getValue(), contentFormat));
            if (!isBsServer) {
                /** Delete instance if bootstrapServerIdNew not equals bootstrapServerIdOld or securityInstanceBsIdNew not equals serverInstanceBsIdOld */
                if (bootstrapServerIdNew != null && server.getValue().shortId == bootstrapServerIdNew &&
                        (bootstrapServerIdNew != bootstrapServerIdOld || securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld))) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld));
                    /** Delete instance if serverIdNew is present in serverInstances and  securityInstanceIdOld by serverIdNew not equals serverInstanceIdOld */
                } else if (this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(server.getValue().shortId) &&
                        securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId));
                }
            }
        }
        // handle acl
        for (Map.Entry<Integer, BootstrapConfig.ACLConfig> acl : bootstrapConfig.acls.entrySet()) {
            requestsWrite.add(toWriteRequest(acl.getKey(), acl.getValue(), contentFormat));
        }
        // handle delete
        if (isBsServer && isLwServer) {
            requests.add(new BootstrapDeleteRequest("/0"));
            requests.add(new BootstrapDeleteRequest("/1"));
        } else {
            pathsDelete.forEach(pathDelete -> requests.add(new BootstrapDeleteRequest(pathDelete)));
        }
        // handle write
        if (requestsWrite.size() > 0) {
            requests.addAll(requestsWrite);
        }
        return (requests);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `initSupportedObjectsDefault` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void initSupportedObjectsDefault() {
        this.supportedObjects = new HashMap<>();
        this.supportedObjects.put(0, "1.1");
        this.supportedObjects.put(1, "1.1");
        this.supportedObjects.put(2, "1.0");
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `remove` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void remove(String endpoint) {
        writeLock.lock();
        try {
            this.lwM2MBootstrapSessionClients.remove(endpoint);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `put` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void put(String endpoint) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            this.lwM2MBootstrapSessionClients.put(endpoint, new LwM2MBootstrapClientInstanceIds());
        } finally {
            writeLock.unlock();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MBootstrapConfigStoreTaskProvider` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
