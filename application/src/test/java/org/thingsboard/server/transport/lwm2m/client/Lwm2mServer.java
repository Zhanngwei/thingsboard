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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link LwM2mInstanceEnabler} for the Server (1) object.
 */
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`Lwm2mServer` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class Lwm2mServer extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Lwm2mServer.class);

    private final static List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 6, 7, 8, 22);

    /**
     * 字段说明：
     * 1. 保存 `shortServerId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int shortServerId;
    private long lifetime;
    /**
     * 字段说明：
     * 1. 保存 `defaultMinPeriod` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Long defaultMinPeriod;
    private Long defaultMaxPeriod;
    /**
     * 字段说明：
     * 1. 保存 `binding` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EnumSet<BindingMode> binding;
    private BindingMode preferredTransport;
    /**
     * 字段说明：
     * 1. 保存 `notifyWhenDisable` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean notifyWhenDisable;

    /**
     * 方法说明：
     * 1. 职责：执行 `Lwm2mServer` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Lwm2mServer() {
        // should only be used at bootstrap time
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `Lwm2mServer` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Lwm2mServer(int shortServerId, long lifetime, EnumSet<BindingMode> binding, boolean notifyWhenDisable,
                       BindingMode preferredTransport) {
        this.shortServerId = shortServerId;
        this.lifetime = lifetime;
        this.binding = binding;
        this.notifyWhenDisable = notifyWhenDisable;
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        this.preferredTransport = preferredTransport;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `Lwm2mServer` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Lwm2mServer(int shortServerId, long lifetime) {
        this(shortServerId, lifetime, EnumSet.of(BindingMode.U), false, BindingMode.U);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `read` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!identity.isSystem())
            LOG.debug("Read on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (resourceid) {
        case 0: // short server ID
            return ReadResponse.success(resourceid, shortServerId);

        case 1: // lifetime
            return ReadResponse.success(resourceid, lifetime);

        case 2: // default min period
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (null == defaultMinPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMinPeriod);

        case 3: // default max period
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (null == defaultMaxPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMaxPeriod);

        case 6: // notification storing when disable or offline
            return ReadResponse.success(resourceid, notifyWhenDisable);

        case 7: // binding
            return ReadResponse.success(resourceid, BindingMode.toString(binding));

        case 22: // preferred transport
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            if (preferredTransport == null)
                return ReadResponse.notFound();
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            return ReadResponse.success(resourceid, preferredTransport.toString());

        default:
            return super.read(identity, resourceid);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `write` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceid, LwM2mResource value) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!identity.isSystem())
            log.debug("Write on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (resourceid) {
        case 0:
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            int previousShortServerId = shortServerId;
            shortServerId = ((Long) value.getValue()).intValue();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (previousShortServerId != shortServerId)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 1:
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            long previousLifetime = lifetime;
            lifetime = (Long) value.getValue();
            if (previousLifetime != lifetime)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 2:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMinPeriod = defaultMinPeriod;
            defaultMinPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMinPeriod, defaultMinPeriod))
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 3:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMaxPeriod = defaultMaxPeriod;
            defaultMaxPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMaxPeriod, defaultMaxPeriod))
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 6: // notification storing when disable or offline
            if (value.getType() != Type.BOOLEAN) {
                return WriteResponse.badRequest("invalid type");
            }
            boolean previousNotifyWhenDisable = notifyWhenDisable;
            notifyWhenDisable = (boolean) value.getValue();
            if (previousNotifyWhenDisable != notifyWhenDisable)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 7: // binding
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                EnumSet<BindingMode> previousBinding = binding;
                binding = BindingMode.parse((String) value.getValue());
                if (!Objects.equals(previousBinding, binding))
                    fireResourceChange(resourceid);
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }
        case 22: // preferredTransport
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                BindingMode previousPreferedTransport = preferredTransport;
                preferredTransport = BindingMode.valueOf((String) value.getValue());
                if (!Objects.equals(previousPreferedTransport, preferredTransport))
                    fireResourceChange(resourceid);
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }

        default:
            return super.write(identity, replace, resourceid, value);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `execute` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
        log.info("Execute on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);
        if (resourceid == 8) {
            getLwM2mClient().triggerRegistrationUpdate(identity);
            return ExecuteResponse.success();
        } else if (resourceid == 9) {
            boolean success = getLwM2mClient().triggerClientInitiatedBootstrap(true);
            if (success) {
                return ExecuteResponse.success();
            }
            else {
                return ExecuteResponse.badRequest("probably no bootstrap server configured");
            }
        } else {
            return super.execute(identity, resourceid, params);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `reset` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void reset(int resourceid) {
        switch (resourceid) {
        case 2:
            defaultMinPeriod = null;
            break;
        case 3:
            defaultMaxPeriod = null;
            break;
        default:
            super.reset(resourceid);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getAvailableResourceIds` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`Lwm2mServer` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
