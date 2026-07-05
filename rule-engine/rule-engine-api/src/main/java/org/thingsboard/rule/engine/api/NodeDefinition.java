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
import lombok.Data;

/**
 * 中文说明：
 * 1. 职责：承载规则节点对外展示和配置所需的节点定义信息。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的节点元数据 DTO。
 * 3. 协作对象：与 {@link RuleNode} 注解、管理端规则链 UI、节点配置默认值和组件扫描流程协作。
 * 4. 生命周期：应用启动或节点元数据刷新时由扫描流程构建，随后用于前端展示和规则节点配置。
 * 5. 设计原因：注解是代码侧元数据，NodeDefinition 是运行期可序列化的数据载体，便于传给 UI 或 API。
 * 6. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor、数据库；直接服务 Rule Engine 节点发现和 UI 配置流程。
 */
@Data
public class NodeDefinition {

    /**
     * `details` 字段，保存当前对象的对应属性。
     */
    private String details;
    /**
     * 描述信息，用于展示或标识当前对象。
     */
    private String description;
    /**
     * 是否启用`in`。
     */
    private boolean inEnabled;
    /**
     * 是否启用`out`。
     */
    private boolean outEnabled;
    /**
     * 关系列表，用于保存一组待处理对象。
     */
    String[] relationTypes;
    /**
     * 是否满足`customRelations`条件。
     */
    boolean customRelations;
    /**
     * 是否满足规则链条件。
     */
    boolean ruleChainNode;
    /**
     * `defaultConfiguration`，保存当前对象的配置选项。
     */
    JsonNode defaultConfiguration;
    /**
     * `uiResources`列表，用于保存一组待处理对象。
     */
    String[] uiResources;
    /**
     * 配置，保存当前对象的配置选项。
     */
    String configDirective;
    /**
     * `icon` 字段，保存当前对象的对应属性。
     */
    String icon;
    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    String iconUrl;
    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    String docUrl;

}

/*
 * 本类总结：
 * 1. 核心职责：把 RuleNode 注解中的节点元数据转换成运行期可传输的数据结构。
 * 2. 核心流程：组件扫描读取注解和默认配置，构建 NodeDefinition，前端规则链编辑器读取并展示。
 * 3. 关键依赖：RuleNode、NodeConfiguration、JsonNode 和规则链 UI。
 * 4. 学习重点：Rule Engine 节点元数据既用于运行时注册，也用于前端配置体验。
 */
