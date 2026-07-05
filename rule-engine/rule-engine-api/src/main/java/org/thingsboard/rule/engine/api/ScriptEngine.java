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
     * 功能：执行`Update Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg);

    /**
     * 功能：执行`Generate Async`。
     * 参数：
     * - `prevMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg);

    /**
     * 功能：执行`Filter Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> executeFilterAsync(TbMsg msg);

    /**
     * 功能：执行`Switch Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg);

    /**
     * 功能：执行JSON。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg);

    /**
     * 功能：执行`To String Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<String> executeToStringAsync(TbMsg msg);

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
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
