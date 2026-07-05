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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Collections;
import java.util.Set;

import static org.thingsboard.server.service.sync.ie.importing.impl.DashboardImportService.WIDGET_CONFIG_PROCESSED_FIELDS_PATTERN;

/**
 * 中文说明：
 * 1. 类目的：`DashboardExportService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
@Service
@TbCoreComponent
public class DashboardExportService extends BaseEntityExportService<DashboardId, Dashboard, EntityExportData<Dashboard>> {

    /**
     * 功能：更新`Related Entities`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `dashboard`：`dashboard` 参数。
     * - `exportData`：待处理数据。
     * 返回：无。
     */
    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Dashboard dashboard, EntityExportData<Dashboard> exportData) {
        if (CollectionUtils.isNotEmpty(dashboard.getAssignedCustomers())) {
            dashboard.getAssignedCustomers().forEach(customerInfo -> {
                customerInfo.setCustomerId(getExternalIdOrElseInternal(ctx, customerInfo.getCustomerId()));
            });
        }
        for (JsonNode entityAlias : dashboard.getEntityAliasesConfig()) {
            replaceUuidsRecursively(ctx, entityAlias, Set.of("id"), null);
        }
        for (JsonNode widgetConfig : dashboard.getWidgetsConfig()) {
            replaceUuidsRecursively(ctx, JacksonUtil.getSafely(widgetConfig, "config", "actions"), Collections.emptySet(), WIDGET_CONFIG_PROCESSED_FIELDS_PATTERN);
        }
        imageService.inlineImages(dashboard);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DASHBOARD);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardExportService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
