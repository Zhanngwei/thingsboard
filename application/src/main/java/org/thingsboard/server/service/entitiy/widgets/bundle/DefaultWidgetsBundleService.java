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

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

/**
 * 中文说明：
 * 1. `DefaultWidgetsBundleService` 是 ThingsBoard Application 中负责 `Widgets Bundle` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbWidgetsBundleService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultWidgetsBundleService extends AbstractTbEntityService implements TbWidgetsBundleService {

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundle save(WidgetsBundle widgetsBundle, User user) throws Exception {
        ActionType actionType = widgetsBundle.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = widgetsBundle.getTenantId();
        try {
            WidgetsBundle savedWidgetsBundle = checkNotNull(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
            autoCommit(user, savedWidgetsBundle.getId());
            notificationEntityService.logEntityAction(tenantId, savedWidgetsBundle.getId(), savedWidgetsBundle,
                    null, actionType, user);
            return savedWidgetsBundle;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.WIDGETS_BUNDLE), widgetsBundle, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(WidgetsBundle widgetsBundle, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = widgetsBundle.getTenantId();
        try {
            widgetsBundleService.deleteWidgetsBundle(widgetsBundle.getTenantId(), widgetsBundle.getId());
            notificationEntityService.logEntityAction(tenantId, widgetsBundle.getId(), widgetsBundle, null, actionType, user);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.WIDGETS_BUNDLE), actionType, user, e, widgetsBundle.getId());
            throw e;
        }
    }

    /**
     * 功能：更新部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeIds`：类型。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void updateWidgetsBundleWidgetTypes(WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds, User user) throws Exception {
        widgetTypeService.updateWidgetsBundleWidgetTypes(user.getTenantId(), widgetsBundleId, widgetTypeIds);
        autoCommit(user, widgetsBundleId);
    }

    /**
     * 功能：更新部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetFqns`：数据列表。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void updateWidgetsBundleWidgetFqns(WidgetsBundleId widgetsBundleId, List<String> widgetFqns, User user) throws Exception {
        widgetTypeService.updateWidgetsBundleWidgetFqns(user.getTenantId(), widgetsBundleId, widgetFqns);
        autoCommit(user, widgetsBundleId);
    }
}
