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
package org.thingsboard.server.service.entitiy.widgets.bundle;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.service.entitiy.SimpleTbEntityService;

import java.util.List;

/**
 * 中文说明：
 * 1. `TbWidgetsBundleService` 是 ThingsBoard Application 中定义 `Tb Widgets Bundle` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `SimpleTbEntityService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbWidgetsBundleService extends SimpleTbEntityService<WidgetsBundle> {

    /**
     * 功能：更新部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeIds`：类型。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void updateWidgetsBundleWidgetTypes(WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds, User user) throws Exception;

    /**
     * 功能：更新部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetFqns`：数据列表。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void updateWidgetsBundleWidgetFqns(WidgetsBundleId widgetsBundleId, List<String> widgetFqns, User user) throws Exception;

}
