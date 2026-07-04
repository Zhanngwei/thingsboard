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
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`SystemUtil` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public class SystemUtil {

    /**
     * 字段说明：
     * 1. 保存 `HARDWARE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final HardwareAbstractionLayer HARDWARE;

    static {
        HARDWARE = new SystemInfo().getHardware();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getMemoryUsage` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<Integer> getMemoryUsage() {
        try {
            GlobalMemory memory = HARDWARE.getMemory();
            long total = memory.getTotal();
            long available = memory.getAvailable();
            return Optional.of(toPercent(total - available, total));
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Throwable e) {
            log.debug("Failed to get memory usage!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTotalMemory` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<Long> getTotalMemory() {
        try {
            return Optional.of(HARDWARE.getMemory().getTotal());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Throwable e) {
            log.debug("Failed to get total memory!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCpuUsage` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<Integer> getCpuUsage() {
        try {
            return Optional.of((int) (HARDWARE.getProcessor().getSystemCpuLoad(1000) * 100.0));
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Throwable e) {
            log.debug("Failed to get cpu usage!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCpuCount` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<Integer> getCpuCount() {
        try {
            return Optional.of(HARDWARE.getProcessor().getLogicalProcessorCount());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Throwable e) {
            log.debug("Failed to get total cpu count!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getDiscSpaceUsage` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<Integer> getDiscSpaceUsage() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            long total = store.getTotalSpace();
            long available = store.getUsableSpace();
            return Optional.of(toPercent(total - available, total));
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Throwable e) {
            log.debug("Failed to get free disc space!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTotalDiscSpace` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Optional<Long> getTotalDiscSpace() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            return Optional.of(store.getTotalSpace());
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Throwable e) {
            log.debug("Failed to get total disc space!!!", e);
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toPercent` 对应的公共工具类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收输入参数后执行本地转换、校验或解析并返回结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static int toPercent(long used, long total) {
        BigDecimal u = new BigDecimal(used);
        BigDecimal t = new BigDecimal(total);
        BigDecimal i = new BigDecimal(100);
        return u.multiply(i).divide(t, RoundingMode.HALF_UP).intValue();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SystemUtil` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
