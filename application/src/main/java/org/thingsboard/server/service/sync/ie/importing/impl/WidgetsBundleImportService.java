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
 * 1. 类目的：`WidgetsBundleImportService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
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

/*
 * 本类总结：
 * 1. 核心职责：`WidgetsBundleImportService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
