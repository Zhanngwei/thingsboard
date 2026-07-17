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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.sync.ie.WidgetsBundleExportData;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

/**
 * 中文说明：
 * 1. `WidgetsBundleImportService` 是 ThingsBoard Application 中负责 `Widgets Bundle Import` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `BaseEntityImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetsBundleImportService extends BaseEntityImportService<WidgetsBundleId, WidgetsBundle, WidgetsBundleExportData> {

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, WidgetsBundle widgetsBundle, IdProvider idProvider) {
        widgetsBundle.setTenantId(tenantId);
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * - `old`：`old` 参数。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected WidgetsBundle prepare(EntitiesImportCtx ctx, WidgetsBundle widgetsBundle, WidgetsBundle old, WidgetsBundleExportData exportData, IdProvider idProvider) {
        return widgetsBundle;
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected WidgetsBundle saveOrUpdate(EntitiesImportCtx ctx, WidgetsBundle widgetsBundle, WidgetsBundleExportData exportData, IdProvider idProvider) {
        if (CollectionsUtil.isNotEmpty(exportData.getWidgets())) {
            exportData.getWidgets().forEach(widgetTypeNode -> {
                String bundleAlias = widgetTypeNode.remove("bundleAlias").asText();
                String alias = widgetTypeNode.remove("alias").asText();
                String fqn = String.format("%s.%s", bundleAlias, alias);
                exportData.addFqn(fqn);
                WidgetTypeDetails widgetType = JacksonUtil.treeToValue(widgetTypeNode, WidgetTypeDetails.class);
                widgetType.setTenantId(ctx.getTenantId());
                widgetType.setFqn(fqn);
                var existingWidgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(ctx.getTenantId(), fqn);
                if (existingWidgetType == null) {
                    widgetType.setId(null);
                } else {
                    widgetType.setId(existingWidgetType.getId());
                    widgetType.setCreatedTime(existingWidgetType.getCreatedTime());
                }
                widgetTypeService.saveWidgetType(widgetType);
            });
        }
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        widgetTypeService.updateWidgetsBundleWidgetFqns(ctx.getTenantId(), savedWidgetsBundle.getId(), exportData.getFqns());
        return savedWidgetsBundle;
    }

    /**
     * 功能：执行 `compare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `exportData`：待处理数据。
     * - `prepared`：`prepared` 参数。
     * - `existing`：`existing` 参数。
     * 返回：判断结果。
     */
    @Override
    protected boolean compare(EntitiesImportCtx ctx, WidgetsBundleExportData exportData, WidgetsBundle prepared, WidgetsBundle existing) {
        return true;
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：处理结果。
     */
    @Override
    protected WidgetsBundle deepCopy(WidgetsBundle widgetsBundle) {
        return new WidgetsBundle(widgetsBundle);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

}
