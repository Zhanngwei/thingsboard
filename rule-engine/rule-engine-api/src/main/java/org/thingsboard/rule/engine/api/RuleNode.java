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
     * 功能：执行 `type` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    ComponentType type();

    /**
     * 功能：执行 `name` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    String name();

    /**
     * 功能：执行 `nodeDescription` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    String nodeDescription();

    /**
     * 功能：执行 `nodeDetails` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    String nodeDetails();

    /**
     * 功能：执行 `configClazz` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    Class<? extends NodeConfiguration> configClazz();

    /**
     * 功能：执行 `clusteringMode` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    ComponentClusteringMode clusteringMode() default ComponentClusteringMode.ENABLED;

    /**
     * 功能：判断队列名称。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean hasQueueName() default false;

    /**
     * 功能：执行 `inEnabled` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean inEnabled() default true;

    /**
     * 功能：执行 `outEnabled` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean outEnabled() default true;

    /**
     * 功能：执行 `scope` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    ComponentScope scope() default ComponentScope.TENANT;

    /**
     * 功能：执行 `relationTypes` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    String[] relationTypes() default {TbNodeConnectionType.SUCCESS, TbNodeConnectionType.FAILURE};

    /**
     * 功能：执行 `uiResources` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    String[] uiResources() default {};

    /**
     * 功能：执行 `configDirective` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    String configDirective() default "";

    /**
     * 功能：执行 `icon` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    String icon() default "";

    /**
     * 功能：执行 `iconUrl` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    String iconUrl() default "";

    /**
     * 功能：执行 `docUrl` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    String docUrl() default "";

    /**
     * 功能：执行 `customRelations` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean customRelations() default false;

    /**
     * 功能：执行 `ruleChainNode` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean ruleChainNode() default false;

    /**
     * 功能：执行 `ruleChainTypes` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    RuleChainType[] ruleChainTypes() default {RuleChainType.CORE, RuleChainType.EDGE};

    /**
     * 功能：执行 `version` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
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
