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
package org.thingsboard.server.common.transport.service;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@TbTransportComponent
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultTransportTenantProfileCache` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
public class DefaultTransportTenantProfileCache implements TransportTenantProfileCache {

    private final Lock tenantProfileFetchLock = new ReentrantLock();
    private final ConcurrentMap<TenantProfileId, TenantProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantProfileId> tenantIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantProfileId, Set<TenantId>> tenantProfileIds = new ConcurrentHashMap<>();
    /**
     * 字段说明：
     * 1. 保存 `dataDecodingEncodingService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 字段说明：
     * 1. 保存 `rateLimitService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TransportRateLimitService rateLimitService;
    private TransportService transportService;

    @Lazy
    @Autowired
    /**
     * 方法说明：
     * 1. 职责：执行 `setRateLimitService` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setRateLimitService(TransportRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Lazy
    @Autowired
    /**
     * 方法说明：
     * 1. 职责：执行 `setTransportService` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setTransportService(TransportService transportService) {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        this.transportService = transportService;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `DefaultTransportTenantProfileCache` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DefaultTransportTenantProfileCache(DataDecodingEncodingService dataDecodingEncodingService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `get` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TenantProfile get(TenantId tenantId) {
        return getTenantProfile(tenantId);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `put` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TenantProfileUpdateResult put(ByteString profileBody) {
        Optional<TenantProfile> profileOpt = dataDecodingEncodingService.decode(profileBody.toByteArray());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (profileOpt.isPresent()) {
            TenantProfile newProfile = profileOpt.get();
            log.trace("[{}] put: {}", newProfile.getId(), newProfile);
            profiles.put(newProfile.getId(), newProfile);
            Set<TenantId> affectedTenants = tenantProfileIds.get(newProfile.getId());
            return new TenantProfileUpdateResult(newProfile, affectedTenants != null ? affectedTenants : Collections.emptySet());
        } else {
            log.warn("Failed to decode profile: {}", profileBody.toString());
            return new TenantProfileUpdateResult(null, Collections.emptySet());
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `put` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean put(TenantId tenantId, TenantProfileId profileId) {
        log.trace("[{}] put: {}", tenantId, profileId);
        TenantProfileId oldProfileId = tenantIds.get(tenantId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (oldProfileId != null && !oldProfileId.equals(profileId)) {
            tenantProfileIds.computeIfAbsent(oldProfileId, id -> ConcurrentHashMap.newKeySet()).remove(tenantId);
            tenantIds.put(tenantId, profileId);
            tenantProfileIds.computeIfAbsent(profileId, id -> ConcurrentHashMap.newKeySet()).add(tenantId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `remove` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Set<TenantId> remove(TenantProfileId profileId) {
        Set<TenantId> tenants = tenantProfileIds.remove(profileId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (tenants != null) {
            tenants.forEach(tenantIds::remove);
        }
        profiles.remove(profileId);
        return tenants;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantProfile` 对应的传输协议契约或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TenantProfile getTenantProfile(TenantId tenantId) {
        TenantProfile profile = null;
        TenantProfileId tenantProfileId = tenantIds.get(tenantId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (tenantProfileId != null) {
            profile = profiles.get(tenantProfileId);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (profile == null) {
            tenantProfileFetchLock.lock();
            try {
                tenantProfileId = tenantIds.get(tenantId);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (tenantProfileId != null) {
                    profile = profiles.get(tenantProfileId);
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (profile == null) {
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    TransportProtos.GetEntityProfileRequestMsg msg = TransportProtos.GetEntityProfileRequestMsg.newBuilder()
                            .setEntityType(EntityType.TENANT.name())
                            .setEntityIdMSB(tenantId.getId().getMostSignificantBits())
                            .setEntityIdLSB(tenantId.getId().getLeastSignificantBits())
                            .build();
                    // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                    TransportProtos.GetEntityProfileResponseMsg entityProfileMsg = transportService.getEntityProfile(msg);
                    Optional<TenantProfile> profileOpt = dataDecodingEncodingService.decode(entityProfileMsg.getData().toByteArray());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (profileOpt.isPresent()) {
                        profile = profileOpt.get();
                        TenantProfile existingProfile = profiles.get(profile.getId());
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (existingProfile != null) {
                            profile = existingProfile;
                        } else {
                            profiles.put(profile.getId(), profile);
                        }
                        tenantProfileIds.computeIfAbsent(profile.getId(), id -> ConcurrentHashMap.newKeySet()).add(tenantId);
                        tenantIds.put(tenantId, profile.getId());
                    } else {
                        log.warn("[{}] Can't decode tenant profile: {}", tenantId, entityProfileMsg.getData());
                        throw new RuntimeException("Can't decode tenant profile!");
                    }
                    Optional<ApiUsageState> apiStateOpt = dataDecodingEncodingService.decode(entityProfileMsg.getApiState().toByteArray());
                    apiStateOpt.ifPresent(apiUsageState -> rateLimitService.update(tenantId, apiUsageState.isTransportEnabled()));
                }
            } finally {
                tenantProfileFetchLock.unlock();
            }
        }
        return profile;
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTransportTenantProfileCache` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
