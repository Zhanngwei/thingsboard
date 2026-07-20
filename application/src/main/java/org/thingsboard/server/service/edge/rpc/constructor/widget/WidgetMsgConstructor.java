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
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.service.edge.rpc.constructor.MsgConstructor;

import java.util.List;

/**
 * 中文说明：
 * 1. `WidgetMsgConstructor` 是 ThingsBoard Application 中定义部件能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `MsgConstructor`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface WidgetMsgConstructor extends MsgConstructor {

    /**
     * 功能：执行 `constructWidgetsBundleUpdateMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * - `widgets`：数据列表。
     * 返回：处理结果。
     */
    WidgetsBundleUpdateMsg constructWidgetsBundleUpdateMsg(UpdateMsgType msgType, WidgetsBundle widgetsBundle, List<String> widgets);

    /**
     * 功能：执行 `constructWidgetsBundleDeleteMsg` 对应的处理。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * 返回：处理结果。
     */
    WidgetsBundleUpdateMsg constructWidgetsBundleDeleteMsg(WidgetsBundleId widgetsBundleId);

    /**
     * 功能：执行 `constructWidgetTypeUpdateMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `widgetTypeDetails`：类型。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    WidgetTypeUpdateMsg constructWidgetTypeUpdateMsg(UpdateMsgType msgType, WidgetTypeDetails widgetTypeDetails, EdgeVersion edgeVersion);

    /**
     * 功能：执行 `constructWidgetTypeDeleteMsg` 对应的处理。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    WidgetTypeUpdateMsg constructWidgetTypeDeleteMsg(WidgetTypeId widgetTypeId);
}
