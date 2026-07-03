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

import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 中文说明：
 * 1. 职责：标记一个 {@link TbNode} 实现类是可被 Rule Engine 发现、注册和展示的规则节点类型。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的节点元数据声明层。
 * 3. 协作对象：与节点扫描器、规则链 UI、{@link NodeConfiguration}、规则节点运行时和组件注册流程协作。
 * 4. 生命周期：注解元数据在应用启动或组件扫描时读取，随后用于节点定义、UI 配置和运行时实例化。
 * 5. 设计原因：节点元数据必须随节点实现类发布，使用运行时注解可避免外部注册表与代码实现不一致。
 * 6. 设计模式：元数据驱动的 Factory/Strategy 注册，Rule Engine 根据注解决定如何创建和路由节点。
 * 7. 技术关联：注解本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库；直接驱动 Rule Engine 节点发现和配置流程。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuleNode {

    /**
     * 中文说明：声明节点组件类型，来源于节点实现类的静态元数据；用于 UI 分类和 Rule Engine 注册。
     */
    ComponentType type();

    /**
     * 中文说明：声明节点显示名称，来源于节点实现类；用于规则链编辑器和节点定义列表。
     */
    String name();

    /**
     * 中文说明：声明节点简短描述，来源于节点实现类；用于 UI 帮助文本和节点目录。
     */
    String nodeDescription();

    /**
     * 中文说明：声明节点详细说明，来源于节点实现类；用于 UI 展示更完整的行为解释。
     */
    String nodeDetails();

    /**
     * 中文说明：声明节点配置类，来源于节点实现类；Rule Engine 用它生成默认配置并反序列化节点配置 JSON。
     */
    Class<? extends NodeConfiguration> configClazz();

    /**
     * 中文说明：声明集群模式，来源于节点实现类；用于决定节点是否支持在集群中分布式执行。
     */
    ComponentClusteringMode clusteringMode() default ComponentClusteringMode.ENABLED;

    /**
     * 中文说明：声明节点是否支持自定义队列名，来源于节点实现类；用于 Rule Engine 队列路由配置。
     */
    boolean hasQueueName() default false;

    /**
     * 中文说明：声明节点是否允许入边，来源于节点实现类；用于规则链 UI 和节点连接校验。
     */
    boolean inEnabled() default true;

    /**
     * 中文说明：声明节点是否允许出边，来源于节点实现类；用于规则链 UI 和消息路由校验。
     */
    boolean outEnabled() default true;

    /**
     * 中文说明：声明节点作用域，来源于节点实现类；用于区分系统级、租户级等组件可见性。
     */
    ComponentScope scope() default ComponentScope.TENANT;

    /**
     * 中文说明：声明节点默认输出关系类型，来源于节点实现类；用于规则链连接建议和消息路由约束。
     */
    String[] relationTypes() default {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE};

    /**
     * 中文说明：声明节点 UI 资源，来源于节点实现类；用于加载规则链编辑器所需的前端资源。
     */
    String[] uiResources() default {};

    /**
     * 中文说明：声明节点配置指令，来源于节点实现类；用于前端选择对应的配置表单。
     */
    String configDirective() default "";

    /**
     * 中文说明：声明节点图标名称，来源于节点实现类；用于规则链 UI 展示。
     */
    String icon() default "";

    /**
     * 中文说明：声明节点图标 URL，来源于节点实现类；用于自定义或外部图标展示。
     */
    String iconUrl() default "";

    /**
     * 中文说明：声明节点文档地址，来源于节点实现类；用于 UI 跳转到官方或扩展文档。
     */
    String docUrl() default "";

    /**
     * 中文说明：声明节点是否允许自定义关系，来源于节点实现类；用于决定 UI 是否限制 relationTypes。
     */
    boolean customRelations() default false;

    /**
     * 中文说明：声明节点是否代表嵌套规则链节点，来源于节点实现类；用于 Rule Engine 处理输入/输出栈。
     */
    boolean ruleChainNode() default false;

    /**
     * 中文说明：声明节点支持的规则链类型，来源于节点实现类；用于区分 Core 与 Edge 规则链可用性。
     */
    RuleChainType[] ruleChainTypes() default {RuleChainType.CORE, RuleChainType.EDGE};

    /**
     * 中文说明：声明节点配置版本，来源于节点实现类；用于 {@link TbNode#upgrade(int, com.fasterxml.jackson.databind.JsonNode)} 配置升级流程。
     */
    int version() default 0;

}

/*
 * 本类总结：
 * 1. 核心职责：用运行时注解声明规则节点的注册、UI 和配置元数据。
 * 2. 核心流程：应用启动扫描 TbNode 实现类，读取 RuleNode 注解，构建节点定义并驱动实例化和 UI 展示。
 * 3. 关键依赖：TbNode、NodeConfiguration、ComponentType、ComponentScope、RuleChainType 和连接关系类型。
 * 4. 学习重点：ThingsBoard Rule Engine 通过注解把节点实现、配置模型和前端展示元数据绑定在一起。
 */
