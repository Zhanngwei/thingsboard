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

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. 职责：作为规则节点运行状态的 API 占位类型，保留节点状态扩展的类型边界。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的节点状态抽象层。
 * 3. 协作对象：与 {@link TbContext} 中的 RuleNodeState 查询/保存方法和规则节点状态持久化流程相关。
 * 4. 生命周期：类本身无字段，状态生命周期由具体 RuleNodeState 数据对象和 Rule Engine 上下文管理。
 * 5. 设计原因：用独立类型表达“节点状态”概念，避免 API 层直接暴露未来可能变化的内部状态实现。
 * 6. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库；作为 Rule Engine 状态语义的类型入口。
 */
public final class TbNodeState {
}

/*
 * 本类总结：
 * 1. 核心职责：保留规则节点状态 API 的类型位置。
 * 2. 核心流程：当前没有运行时代码，状态读写由 TbContext 和 RuleNodeState 相关方法承载。
 * 3. 关键依赖：TbContext、RuleNodeState 和 Rule Engine 状态持久化流程。
 * 4. 学习重点：空类型也可以用于稳定 API 边界，为后续状态模型演进预留空间。
 */
