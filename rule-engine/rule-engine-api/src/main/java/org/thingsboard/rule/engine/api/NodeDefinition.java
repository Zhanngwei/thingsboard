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
     * 中文说明：节点详细说明，来源于 RuleNode 注解或节点定义构建流程；生命周期与节点定义缓存一致。
     */
    private String details;
    /**
     * 中文说明：节点简短描述，来源于 RuleNode 注解；用于规则链编辑器展示节点用途。
     */
    private String description;
    /**
     * 中文说明：是否允许入边，来源于 RuleNode 注解；用于 UI 连接校验和规则链结构约束。
     */
    private boolean inEnabled;
    /**
     * 中文说明：是否允许出边，来源于 RuleNode 注解；用于 UI 连接校验和消息路由能力展示。
     */
    private boolean outEnabled;
    /**
     * 中文说明：节点支持的关系类型，来源于 RuleNode 注解；生命周期与节点定义一致，用于规则链连线。
     */
    String[] relationTypes;
    /**
     * 中文说明：是否允许自定义关系，来源于 RuleNode 注解；用于决定 UI 是否允许用户输入任意关系名。
     */
    boolean customRelations;
    /**
     * 中文说明：是否为规则链节点，来源于 RuleNode 注解；用于嵌套规则链输入/输出语义展示。
     */
    boolean ruleChainNode;
    /**
     * 中文说明：默认配置 JSON，来源于 NodeConfiguration.defaultConfiguration 转换结果；用于新建节点时填充配置。
     */
    JsonNode defaultConfiguration;
    /**
     * 中文说明：节点需要的前端资源，来源于 RuleNode 注解；用于加载自定义配置 UI。
     */
    String[] uiResources;
    /**
     * 中文说明：前端配置指令名称，来源于 RuleNode 注解；用于选择节点配置表单。
     */
    String configDirective;
    /**
     * 中文说明：节点图标名称，来源于 RuleNode 注解；用于规则链 UI 展示。
     */
    String icon;
    /**
     * 中文说明：节点图标 URL，来源于 RuleNode 注解；用于外部图标展示。
     */
    String iconUrl;
    /**
     * 中文说明：节点文档地址，来源于 RuleNode 注解；用于 UI 帮助链接。
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
