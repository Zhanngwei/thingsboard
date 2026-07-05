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

import lombok.Data;

/**
 * 中文说明：
 * 1. 职责：表示没有业务参数的规则节点配置对象，同时保留配置版本字段用于后续兼容升级。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的节点配置抽象层，被具体规则节点作为默认配置模型复用。
 * 3. 协作对象：与 {@link NodeConfiguration}、节点注解中的配置类型以及 Rule Engine 节点初始化流程协作。
 * 4. 生命周期：由规则节点初始化或配置反序列化流程创建，随单个节点配置实例存在，不持有运行期资源。
 * 5. 设计原因：空配置节点仍需要统一实现配置接口，避免每个无配置节点重复声明一个占位配置类。
 * 6. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库；只作为 Rule Engine 配置模型参与节点生命周期。
 */
@Data
public class EmptyNodeConfiguration implements NodeConfiguration<EmptyNodeConfiguration> {

    /**
     * 版本号，表示当前对象的对应属性。
     */
    private int version;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EmptyNodeConfiguration defaultConfiguration() {
        return new EmptyNodeConfiguration();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：为无参数规则节点提供统一的默认配置对象。
 * 2. 核心流程：Rule Engine 需要默认配置时调用 defaultConfiguration 创建新实例。
 * 3. 关键依赖：NodeConfiguration 配置接口和节点配置反序列化流程。
 * 4. 学习重点：即使节点没有业务参数，也通过统一配置接口接入 Rule Engine 生命周期。
 */
