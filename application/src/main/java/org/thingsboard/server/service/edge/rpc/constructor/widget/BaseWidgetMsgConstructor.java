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
package org.thingsboard.server.service.edge.rpc.constructor.widget;

import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;

/**
 * 中文说明：
 * 1. `BaseWidgetMsgConstructor` 是 ThingsBoard Application 中创建或提供部件对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `WidgetMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public abstract class BaseWidgetMsgConstructor implements WidgetMsgConstructor {

    /**
     * 功能：执行 `constructWidgetsBundleDeleteMsg` 对应的处理。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundleUpdateMsg constructWidgetsBundleDeleteMsg(WidgetsBundleId widgetsBundleId) {
        return WidgetsBundleUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetsBundleId.getId().getMostSignificantBits())
                .setIdLSB(widgetsBundleId.getId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：执行 `constructWidgetTypeDeleteMsg` 对应的处理。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeUpdateMsg constructWidgetTypeDeleteMsg(WidgetTypeId widgetTypeId) {
        return WidgetTypeUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetTypeId.getId().getMostSignificantBits())
                .setIdLSB(widgetTypeId.getId().getLeastSignificantBits())
                .build();
    }
}
