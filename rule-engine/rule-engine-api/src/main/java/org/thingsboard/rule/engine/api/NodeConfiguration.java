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
 * 中文说明：
 * 1. 职责：定义规则节点配置对象必须提供默认配置的最小契约。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API，被所有具体节点配置类实现。
 * 3. 协作对象：与 {@link RuleNode#configClazz()}、{@link TbNodeConfiguration} 和节点初始化逻辑协作。
 * 4. 生命周期：接口由应用启动后的节点定义扫描流程识别，具体实现随规则节点配置实例存在。
 * 5. 设计原因：用统一接口约束默认配置生成，避免 Rule Engine 通过反射猜测每种配置类的构造细节。
 * 6. 设计模式：Template 契约形式，Rule Engine 定义调用点，具体配置类提供默认对象。
 * 7. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库；直接属于 Rule Engine 配置流程。
 */
public interface NodeConfiguration<T extends NodeConfiguration> {

    /**
     * 中文说明：
     * 1. 方法职责：返回当前配置类型的默认配置实例。
     * 2. 输入参数：无。
     * 3. 返回值：类型参数 T 对应的默认配置对象。
     * 4. 调用时机：Rule Engine 构建节点定义、初始化节点配置或 UI 需要默认 JSON 时调用。
     * 5. 调用方：节点定义扫描、规则节点配置转换和管理端配置渲染流程。
     * 6. 使用流程：被 Rule Engine 节点配置生命周期使用。
     * 7. 线程安全：接口不保存状态；线程安全取决于实现类是否返回共享可变对象，推荐返回新实例。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：不直接涉及事务、缓存、MQTT、Actor 通信和数据库；直接涉及 Rule Engine。
     */
    T defaultConfiguration();

}

/*
 * 本类总结：
 * 1. 核心职责：为所有规则节点配置类提供默认配置生成契约。
 * 2. 核心流程：节点定义或配置初始化流程通过 defaultConfiguration 获取默认对象。
 * 3. 关键依赖：RuleNode 注解、TbNodeConfiguration 包装对象和具体节点配置类。
 * 4. 学习重点：配置类不是普通 POJO，它通过该接口被 Rule Engine 统一发现和初始化。
 */
