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
package org.thingsboard.server.service.edge.rpc.constructor.resource;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `ResourceMsgConstructorV1` 是 ThingsBoard Application 中创建或提供消息对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseResourceMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class ResourceMsgConstructorV1 extends BaseResourceMsgConstructor {

    /**
     * 功能：执行 `constructResourceUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `tbResource`：`tbResource` 参数。
     * 返回：处理结果。
     */
    @Override
    public ResourceUpdateMsg constructResourceUpdatedMsg(UpdateMsgType msgType, TbResource tbResource) {
        if (ResourceType.IMAGE.equals(tbResource.getResourceType())) {
            // Exclude support for a recently added resource type when dealing with older Edges
            // to maintain compatibility and avoid potential issues.
            return null;
        }
        ResourceUpdateMsg.Builder builder = ResourceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tbResource.getId().getId().getMostSignificantBits())
                .setIdLSB(tbResource.getId().getId().getLeastSignificantBits())
                .setTitle(tbResource.getTitle())
                .setResourceKey(tbResource.getResourceKey())
                .setResourceType(tbResource.getResourceType().name())
                .setFileName(tbResource.getFileName());
        if (tbResource.getData() != null) {
            builder.setData(tbResource.getEncodedData());
        }
        if (tbResource.getEtag() != null) {
            builder.setEtag(tbResource.getEtag());
        }
        if (TenantId.SYS_TENANT_ID.equals(tbResource.getTenantId())) {
            builder.setIsSystem(true);
        }
        return builder.build();
    }
}
