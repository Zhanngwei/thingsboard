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
     * 中文说明：
     * 1. 方法职责：初始化节点实例并解析节点配置。
     * 2. 输入参数：ctx 是节点运行上下文，configuration 是规则节点 JSON 配置包装。
     * 3. 返回值：无；初始化失败通过 TbNodeException 抛出。
     * 4. 调用时机：规则链加载、节点创建或配置更新后调用。
     * 5. 调用方：Rule Engine 节点运行时/Actor 初始化流程。
     * 6. 使用流程：属于 Rule Engine 节点生命周期的初始化阶段。
     * 7. 线程安全：节点实现通常在消息处理前初始化；实现类需要自行保证初始化状态对后续线程可见。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、缓存、MQTT、数据库；由 Actor/Rule Engine 调用，具体实现可能获取缓存或外部资源。
     */
    void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    /**
     * 中文说明：
     * 1. 方法职责：处理进入当前规则节点的一条消息。
     * 2. 输入参数：ctx 是节点上下文，msg 是当前规则链消息。
     * 3. 返回值：无；处理结果通过 ctx.tellNext/tellSuccess/tellFailure 等方法路由。
     * 4. 调用时机：Rule Engine Actor 将消息分发到当前节点时调用。
     * 5. 调用方：规则链 Actor/节点执行器。
     * 6. 使用流程：属于 Rule Engine 核心消息处理流程。
     * 7. 线程安全：同一节点是否并发执行取决于运行时调度；实现类应避免未保护的共享可变状态。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口本身不直接操作事务、缓存、MQTT、数据库；通过 Actor 调用并直接涉及 Rule Engine，具体节点可能访问这些能力。
     */
    void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException;

    /**
     * 中文说明：
     * 1. 方法职责：释放节点持有的资源。
     * 2. 输入参数：无。
     * 3. 返回值：无。
     * 4. 调用时机：规则链停止、节点被移除、配置刷新或应用关闭时调用。
     * 5. 调用方：Rule Engine 节点生命周期管理流程。
     * 6. 使用流程：属于 Rule Engine 节点销毁阶段。
     * 7. 线程安全：默认实现无状态线程安全；有资源的实现需处理销毁与异步回调并发。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：默认不涉及事务、缓存、MQTT、Actor、数据库；具体实现可能关闭外部客户端或移除监听器。
     */
    default void destroy() {
    }

    /**
     * 中文说明：
     * 1. 方法职责：处理队列分区变化通知。
     * 2. 输入参数：ctx 是节点上下文，msg 是分区变化消息。
     * 3. 返回值：无。
     * 4. 调用时机：集群队列分区重新分配或节点所属分区变化时调用。
     * 5. 调用方：Rule Engine 队列/Actor 调度流程。
     * 6. 使用流程：属于 Rule Engine 集群分区感知流程。
     * 7. 线程安全：默认实现无状态线程安全；具体实现若维护分区状态需自行同步。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：默认不涉及事务、缓存、MQTT、数据库；通过 Actor/队列调度触发，直接涉及 Rule Engine。
     */
    default void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
    }

    /**
     * Upgrades the configuration from a specific version to the current version specified in the
     * {@link RuleNode} annotation for the instance of {@link TbNode}.
     *
     * @param fromVersion        The version from which the configuration needs to be upgraded.
     * @param oldConfiguration   The old configuration to be upgraded.
     * @return                   A pair consisting of a Boolean flag indicating the success of the upgrade
     *                           and a JsonNode representing the upgraded configuration.
     * @throws TbNodeException   If an error occurs during the upgrade process.
     *
     * 中文说明：
     * 1. 方法职责：把旧版本节点配置升级为当前 RuleNode.version 声明的版本。
     * 2. 输入参数：fromVersion 是旧配置版本，oldConfiguration 是旧配置 JSON。
     * 3. 返回值：TbPair<Boolean, JsonNode>，Boolean 表示是否执行了升级，JsonNode 是升级后的配置。
     * 4. 调用时机：规则节点配置版本低于当前节点版本时调用。
     * 5. 调用方：Rule Engine 配置加载或迁移流程。
     * 6. 使用流程：属于 Rule Engine 节点配置升级流程。
     * 7. 线程安全：默认实现无状态线程安全；具体实现若使用共享转换器需保证并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine 配置迁移。
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
