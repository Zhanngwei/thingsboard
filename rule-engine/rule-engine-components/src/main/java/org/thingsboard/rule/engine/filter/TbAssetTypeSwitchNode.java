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
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "asset profile switch",
        customRelations = true,
        relationTypes = {"default"},
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Route incoming messages based on the name of the asset profile",
        nodeDetails = "Route incoming messages based on the name of the asset profile. The asset profile name is case-sensitive.<br><br>" +
                "Output connections: <i>Asset profile name</i> or <code>Failure</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
/**
 * 中文说明：`TbAssetTypeSwitchNode` 是Asset类型切换节点规则节点，用于根据消息类型、实体类型、关系、脚本或告警状态判断消息路由。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`EmptyNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbAssetTypeSwitchNode extends TbAbstractTypeSwitchNode {

    @Override
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbAssetTypeSwitchNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    protected String getRelationType(TbContext ctx, EntityId originator) throws TbNodeException {
        if (!EntityType.ASSET.equals(originator.getEntityType())) {
            throw new TbNodeException("Unsupported originator type: " + originator.getEntityType().getNormalName() + "!" +
                    " Only " + EntityType.ASSET.getNormalName() + " type is allowed.");
        }
        AssetProfile assetProfile = ctx.getAssetProfileCache().get(ctx.getTenantId(), (AssetId) originator);
        if (assetProfile == null) {
            throw new TbNodeException("Asset profile for entity id: " + originator.getId() + " wasn't found!");
        }
        return assetProfile.getName();
    }

    /*
     * 本类总结：`TbAssetTypeSwitchNode` 负责根据消息类型、实体类型、关系、脚本或告警状态判断消息路由；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
