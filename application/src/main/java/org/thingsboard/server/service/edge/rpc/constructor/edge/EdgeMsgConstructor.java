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
package org.thingsboard.server.service.edge.rpc.constructor.edge;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `EdgeMsgConstructor` 是 ThingsBoard Application 中创建或提供边缘节点对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class EdgeMsgConstructor {

    /**
     * 功能：执行 `constructEdgeConfiguration` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * 返回：处理结果。
     */
    public EdgeConfiguration constructEdgeConfiguration(Edge edge) {
        EdgeConfiguration.Builder builder = EdgeConfiguration.newBuilder()
                .setEdgeIdMSB(edge.getId().getId().getMostSignificantBits())
                .setEdgeIdLSB(edge.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setType(edge.getType())
                .setRoutingKey(edge.getRoutingKey())
                .setSecret(edge.getSecret())
                .setAdditionalInfo(JacksonUtil.toString(edge.getAdditionalInfo()))
                .setCloudType("CE");
        if (edge.getCustomerId() != null) {
            builder.setCustomerIdMSB(edge.getCustomerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(edge.getCustomerId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }
}
