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
package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

/**
 * 中文说明：`TbOriginatorTypeSwitchNode` 是发起实体类型切换节点规则节点，用于根据消息类型、实体类型、关系、脚本或告警状态判断消息路由。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`EmptyNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "entity type switch",
        configClazz = EmptyNodeConfiguration.class,
        relationTypes = {}, // should always be empty. We add the relation types for this node in AnnotationComponentDiscoveryService.
        nodeDescription = "Route incoming messages by Message Originator Type",
        nodeDetails = "Routes messages to chain according to the entity type ('Device', 'Asset', etc.).<br><br>" +
                "Output connections: <i>Message originator type</i> or <code>Failure</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbOriginatorTypeSwitchNode extends TbAbstractTypeSwitchNode {

    /**
     * 功能：获取关系。
     * 参数：
     * - `ctx`：处理上下文。
     * - `originator`：`originator` 参数。
     * 返回：文本结果。
     */
    @Override
    protected String getRelationType(TbContext ctx, EntityId originator) {
        return originator.getEntityType().getNormalName();
    }

    /*
     * 本类总结：`TbOriginatorTypeSwitchNode` 负责根据消息类型、实体类型、关系、脚本或告警状态判断消息路由；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
