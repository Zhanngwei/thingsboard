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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Getter;

/**
 * Define the behavior of a write request.
 */
/**
 * 中文说明：
 * 1. 类目的：`LwM2MOperationType` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public enum LwM2MOperationType {

    READ(0, "Read", true),
    READ_COMPOSITE(1, "ReadComposite", false, true),
    DISCOVER(2, "Discover", true),
    DISCOVER_ALL(3, "DiscoverAll", false),
    OBSERVE_READ_ALL(4, "ObserveReadAll", false),

    OBSERVE(5, "Observe", true),
    OBSERVE_COMPOSITE(6, "ObserveComposite", false, true),
    OBSERVE_CANCEL(7, "ObserveCancel", true),
    OBSERVE_COMPOSITE_CANCEL(8, "ObserveCompositeCancel", false, true),
    OBSERVE_CANCEL_ALL(9, "ObserveCancelAll", false),
    EXECUTE(10, "Execute", true),
    /**
     * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
     * section 5.3.3 of the LW M2M spec).
     * if all resources are to be replaced
     */
    WRITE_REPLACE(11, "WriteReplace", true),

    /**
     * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
     * 5.3.3 of the LW M2M spec).
     * if this is a partial update request
     */
    WRITE_UPDATE(12, "WriteUpdate", true),
    WRITE_COMPOSITE(14, "WriteComposite", false, true),
    WRITE_ATTRIBUTES(15, "WriteAttributes", true),
    DELETE(16, "Delete", true),
    CREATE(17, "Create", true);
    // only for RPC - future
//    FW_UPDATE(18, "FirmwareUpdate", false),
//    FW_READ_INFO(19, "FirmwareReadInfo", false),
//    SW_UPDATE(20, "SoftwareUpdate", false),
//    SW_READ_INFO(21, "SoftwareReadInfo", false),
//    SW_UNINSTALL(22, "SoftwareUninstall", false);


    @Getter
    /**
     * 字段说明：
     * 1. 保存 `code` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final int code;
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `type` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final String type;
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `hasObjectId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final boolean hasObjectId;

    @Getter
    /**
     * 字段说明：
     * 1. 保存 `composite` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final boolean composite;

    LwM2MOperationType(int code, String type, boolean hasObjectId) {
        this(code, type, hasObjectId, false);
    }

    LwM2MOperationType(int code, String type, boolean hasObjectId, boolean composite) {
        this.code = code;
        this.type = type;
        this.hasObjectId = hasObjectId;
        this.composite = composite;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (hasObjectId && composite) {
            throw new IllegalArgumentException("Can't set both Composite and hasObjectId for the same operation!");
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `fromType` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static LwM2MOperationType fromType(String type) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (LwM2MOperationType to : LwM2MOperationType.values()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MOperationType` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
