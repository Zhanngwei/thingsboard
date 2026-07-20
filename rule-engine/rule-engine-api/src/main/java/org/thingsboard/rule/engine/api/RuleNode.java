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
 * 1. `RuleNode` 是 ThingsBoard Rule Engine API 中声明规则节点元数据的注解类型。
 * 2. 它为被标注的类型或成员提供框架可读取的描述信息。
 * 3. 注解属性定义调用方可以声明的配置内容和默认值。
 * 4. 它直接协作于读取该注解的扫描器、注册器或运行框架。
 * 5. 独立注解可以用声明式方式表达规则，避免调用方编写重复注册代码。
 * 6. 阅读时重点关注注解目标、保留策略和每个属性的默认语义。
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
