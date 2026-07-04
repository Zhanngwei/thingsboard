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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`TbNodeUpgradeUtils` 是ThingsBoard Application 模块中的应用服务支撑类型，用于承载服务端运行期的数据、依赖或流程控制。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 生命周期：由 Spring 容器、Actor System、Web 请求或队列消费流程管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO/Helper。
 */
public class TbNodeUpgradeUtils {

    /**
     * 方法说明：
     * 1. 职责：执行 `upgradeConfigurationAndVersion` 对应的应用服务支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器、Actor System、Web 请求或队列消费流程管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static void upgradeConfigurationAndVersion(RuleNode node, RuleNodeClassInfo nodeInfo) {
        JsonNode oldConfiguration = node.getConfiguration();
        int configurationVersion = node.getConfigurationVersion();

        int currentVersion = nodeInfo.getCurrentVersion();
        var configClass = nodeInfo.getAnnotation().configClazz();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (oldConfiguration == null || !oldConfiguration.isObject()) {
            log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                            "Current configuration is null or not a json object. " +
                            "Going to set default configuration ... ",
                    node.getId(), node.getType(), configurationVersion, currentVersion);
            node.setConfiguration(getDefaultConfig(configClass));
        } else {
            var tbVersionedNode = getTbVersionedNode(nodeInfo);
            try {
                JsonNode queueName = oldConfiguration.get(QUEUE_NAME);
                TbPair<Boolean, JsonNode> upgradeResult = tbVersionedNode.upgrade(configurationVersion, oldConfiguration);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (upgradeResult.getFirst()) {
                    node.setConfiguration(upgradeResult.getSecond());
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (nodeInfo.getAnnotation().hasQueueName() && queueName != null && queueName.isTextual()) {
                        node.setQueueName(queueName.asText());
                    }
                }
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                try {
                    JacksonUtil.treeToValue(oldConfiguration, configClass);
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                } catch (Exception ex) {
                    log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                                    "Going to set default configuration ... ",
                            node.getId(), node.getType(), configurationVersion, currentVersion, e);
                    node.setConfiguration(getDefaultConfig(configClass));
                }
            }
        }
        node.setConfigurationVersion(currentVersion);
    }

    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `getTbVersionedNode` 对应的应用服务支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器、Actor System、Web 请求或队列消费流程管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static TbNode getTbVersionedNode(RuleNodeClassInfo nodeInfo) {
        return (TbNode) nodeInfo.getClazz().getDeclaredConstructor().newInstance();
    }

    @SneakyThrows
    /**
     * 方法说明：
     * 1. 职责：执行 `getDefaultConfig` 对应的应用服务支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器、Actor System、Web 请求或队列消费流程管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static JsonNode getDefaultConfig(Class<? extends NodeConfiguration> configClass) {
        return JacksonUtil.valueToTree(configClass.getDeclaredConstructor().newInstance().defaultConfiguration());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbNodeUpgradeUtils` 在 ThingsBoard Application 模块 中承担应用服务支撑类型职责，核心目的是承载服务端运行期的数据、依赖或流程控制。
 * 2. 核心流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
