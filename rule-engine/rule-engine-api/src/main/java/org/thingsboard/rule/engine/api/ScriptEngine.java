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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. 职责：定义 Rule Engine 规则节点执行脚本的异步接口。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的脚本执行子系统。
 * 3. 协作对象：与脚本节点、{@link TbContext#createScriptEngine}、{@link org.thingsboard.server.common.msg.TbMsg} 和脚本运行时协作。
 * 4. 生命周期：由 TbContext 根据脚本语言和脚本文本创建，随节点配置或节点生命周期存在，销毁时释放脚本运行时资源。
 * 5. 设计原因：不同脚本节点需要返回不同类型结果，统一接口能复用脚本引擎创建、异步执行和销毁逻辑。
 * 6. 设计模式：Strategy，具体脚本引擎实现封装 JavaScript/TBEL 等语言差异。
 * 7. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；执行结果直接驱动 Rule Engine 消息转换、过滤和路由。
 */
public interface ScriptEngine {

    /**
     * 中文说明：
     * 1. 方法职责：异步执行更新类脚本，返回一组新的消息。
     * 2. 输入参数：msg 是当前规则链正在处理的消息。
     * 3. 返回值：ListenableFuture 包装的 TbMsg 列表，用于后续消息路由。
     * 4. 调用时机：转换或脚本节点需要生成多条输出消息时调用。
     * 5. 调用方：脚本转换节点、消息生成节点。
     * 6. 使用流程：属于 Rule Engine 消息转换流程。
     * 7. 线程安全：取决于具体脚本引擎实现；异步执行需保护脚本上下文并发访问。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；直接涉及 Rule Engine。
     */
    ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg);

    /**
     * 中文说明：
     * 1. 方法职责：异步执行生成类脚本，基于上一条消息生成一条新消息。
     * 2. 输入参数：prevMsg 是生成逻辑的上下文消息。
     * 3. 返回值：ListenableFuture 包装的新 TbMsg。
     * 4. 调用时机：脚本生成节点处理消息时调用。
     * 5. 调用方：消息生成类规则节点。
     * 6. 使用流程：属于 Rule Engine 消息生成流程。
     * 7. 线程安全：由具体脚本引擎保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；直接驱动 Rule Engine 后续路由。
     */
    ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg);

    /**
     * 中文说明：
     * 1. 方法职责：异步执行过滤脚本。
     * 2. 输入参数：msg 是待判断的规则消息。
     * 3. 返回值：ListenableFuture 包装的 Boolean，true 表示通过，false 表示过滤。
     * 4. 调用时机：过滤节点处理消息时调用。
     * 5. 调用方：脚本过滤节点。
     * 6. 使用流程：属于 Rule Engine 消息过滤流程。
     * 7. 线程安全：由具体脚本引擎保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；直接影响 Rule Engine 关系路由。
     */
    ListenableFuture<Boolean> executeFilterAsync(TbMsg msg);

    /**
     * 中文说明：
     * 1. 方法职责：异步执行分支脚本，返回输出关系集合。
     * 2. 输入参数：msg 是当前规则消息。
     * 3. 返回值：ListenableFuture 包装的关系类型集合。
     * 4. 调用时机：切换/分支节点需要按脚本结果选择关系时调用。
     * 5. 调用方：脚本 switch 节点。
     * 6. 使用流程：属于 Rule Engine 动态关系路由流程。
     * 7. 线程安全：由具体脚本引擎保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；直接影响 Rule Engine 关系路由。
     */
    ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg);

    /**
     * 中文说明：
     * 1. 方法职责：异步执行返回 JSON 的脚本。
     * 2. 输入参数：msg 是脚本输入消息。
     * 3. 返回值：ListenableFuture 包装的 JsonNode。
     * 4. 调用时机：节点需要脚本输出结构化 JSON 时调用。
     * 5. 调用方：转换节点、外部调用节点或自定义脚本节点。
     * 6. 使用流程：属于 Rule Engine 脚本计算流程。
     * 7. 线程安全：由具体脚本引擎保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine。
     */
    ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg);

    /**
     * 中文说明：
     * 1. 方法职责：异步执行返回字符串的脚本。
     * 2. 输入参数：msg 是脚本输入消息。
     * 3. 返回值：ListenableFuture 包装的字符串结果。
     * 4. 调用时机：节点需要脚本生成文本、主题、URL 或外部载荷时调用。
     * 5. 调用方：转换节点、外部调用节点或自定义脚本节点。
     * 6. 使用流程：属于 Rule Engine 脚本计算流程。
     * 7. 线程安全：由具体脚本引擎保证。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine。
     */
    ListenableFuture<String> executeToStringAsync(TbMsg msg);

    /**
     * 中文说明：
     * 1. 方法职责：释放脚本引擎资源。
     * 2. 输入参数：无。
     * 3. 返回值：无。
     * 4. 调用时机：规则节点销毁、配置变更或脚本引擎不再使用时调用。
     * 5. 调用方：持有脚本引擎的规则节点。
     * 6. 使用流程：属于 Rule Engine 节点生命周期清理流程。
     * 7. 线程安全：实现需处理执行中任务与销毁并发。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine 资源释放。
     */
    void destroy();

}

/*
 * 本类总结：
 * 1. 核心职责：统一规则节点脚本的异步执行能力。
 * 2. 核心流程：节点通过 TbContext 创建 ScriptEngine，调用对应 execute 方法获得异步结果，节点销毁时释放资源。
 * 3. 关键依赖：TbMsg、JsonNode、ListenableFuture 和具体脚本语言运行时。
 * 4. 学习重点：脚本执行以异步 Future 返回结果，结果类型决定后续转换、过滤或关系路由。
 */
