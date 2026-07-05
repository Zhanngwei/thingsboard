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
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.concurrent.ExecutionException;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. 职责：定义所有 Rule Engine 规则节点必须实现的生命周期和消息处理契约。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的核心节点抽象。
 * 3. 协作对象：与 {@link TbContext}、{@link TbNodeConfiguration}、{@link org.thingsboard.server.common.msg.TbMsg}、规则链 Actor 和节点注解 {@link RuleNode} 协作。
 * 4. 生命周期：节点实例由 Rule Engine 创建，先 init 初始化，再多次 onMsg 处理消息，分区变化时可接收通知，销毁时调用 destroy。
 * 5. 设计原因：Rule Engine 需要统一调度不同类型节点，用接口约束生命周期可避免运行时反射调用不稳定。
 * 6. 设计模式：Strategy/Template，运行时按统一模板调用节点生命周期，具体节点提供不同策略。
 * 7. 技术关联：接口本身不直接涉及事务、缓存、MQTT、数据库；通过 Rule Engine Actor 调度节点，具体实现可能涉及这些能力。
 */
public interface TbNode {

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException;

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    default void destroy() {
    }

    /**
     * 功能：处理分区。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    default void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
    }

    /**
     * 功能：执行 `upgrade` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * - `oldConfiguration`：配置对象。
     * 返回：处理结果。
     */
    default TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return new TbPair<>(false, oldConfiguration);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：定义所有规则节点统一的初始化、消息处理、分区变化和销毁生命周期。
 * 2. 核心流程：Rule Engine 创建节点并调用 init，消息进入时调用 onMsg，分区变化时调用 onPartitionChangeMsg，停止时调用 destroy。
 * 3. 关键依赖：TbContext、TbNodeConfiguration、TbMsg、PartitionChangeMsg、RuleNode 注解和规则链 Actor。
 * 4. 学习重点：规则节点不是普通服务方法，而是由 Actor/Rule Engine 运行时按生命周期调度的策略对象。
 */
