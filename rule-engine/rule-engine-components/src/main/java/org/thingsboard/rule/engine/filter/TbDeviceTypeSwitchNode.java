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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

/**
 * 中文说明：
 * 1. `TbDeviceTypeSwitchNode` 是 ThingsBoard Rule Engine Components 中处理设备的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbAbstractTypeSwitchNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "device profile switch",
        customRelations = true,
        relationTypes = {"default"},
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Route incoming messages based on the name of the device profile",
        nodeDetails = "Route incoming messages based on the name of the device profile. The device profile name is case-sensitive<br><br>" +
                "Output connections: <i>Device profile name</i> or <code>Failure</code>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbDeviceTypeSwitchNode extends TbAbstractTypeSwitchNode {

    /**
     * 功能：获取关系。
     * 参数：
     * - `ctx`：处理上下文。
     * - `originator`：`originator` 参数。
     * 返回：文本结果。
     */
    @Override
    protected String getRelationType(TbContext ctx, EntityId originator) throws TbNodeException {
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            throw new TbNodeException("Unsupported originator type: " + originator.getEntityType().getNormalName() +
                    "! Only " + EntityType.DEVICE.getNormalName() + " type is allowed.");
        }
        DeviceProfile deviceProfile = ctx.getDeviceProfileCache().get(ctx.getTenantId(), (DeviceId) originator);
        if (deviceProfile == null) {
            throw new TbNodeException("Device profile for entity id: " + originator.getId() + " wasn't found!");
        }
        return deviceProfile.getName();
    }
}
