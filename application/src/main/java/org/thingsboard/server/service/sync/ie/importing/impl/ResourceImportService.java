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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

/**
 * 中文说明：
 * 1. 类目的：`ResourceImportService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
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
public class ResourceImportService extends BaseEntityImportService<TbResourceId, TbResource, EntityExportData<TbResource>> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ResourceService resourceService;
    private final ImageService imageService;

    /**
     * 功能：更新`Owner`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resource`：`resource` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwner(TenantId tenantId, TbResource resource, IdProvider idProvider) {
        resource.setTenantId(tenantId);
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `resource`：`resource` 参数。
     * - `oldResource`：`oldResource` 参数。
     * - `exportData`：待处理数据。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    protected TbResource prepare(EntitiesImportCtx ctx, TbResource resource, TbResource oldResource, EntityExportData<TbResource> exportData, IdProvider idProvider) {
        return resource;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `ctx`：处理上下文。
     * - `resource`：`resource` 参数。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected TbResource findExistingEntity(EntitiesImportCtx ctx, TbResource resource, IdProvider idProvider) {
        TbResource existingResource = super.findExistingEntity(ctx, resource, idProvider);
        if (existingResource == null && ctx.isFindExistingByName()) {
            existingResource = resourceService.findResourceByTenantIdAndKey(ctx.getTenantId(), resource.getResourceType(), resource.getResourceKey());
        }
        return existingResource;
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
    protected boolean compare(EntitiesImportCtx ctx, EntityExportData<TbResource> exportData, TbResource prepared, TbResource existing) {
        return true;
    }

    /**
     * 功能：执行 `deepCopy` 对应的处理。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：处理结果。
     */
    @Override
    protected TbResource deepCopy(TbResource resource) {
        return new TbResource(resource);
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `resource`：`resource` 参数。
     * - `exportData`：待处理数据。
     * - `idProvider`：`idProvider` 参数。
     * 返回：处理结果。
     */
    @Override
    protected TbResource saveOrUpdate(EntitiesImportCtx ctx, TbResource resource, EntityExportData<TbResource> exportData, IdProvider idProvider) {
        if (resource.getResourceType() == ResourceType.IMAGE) {
            return new TbResource(imageService.saveImage(resource));
        } else {
            resource = resourceService.saveResource(resource);
            resource.setData(null);
            resource.setPreview(null);
            return resource;
        }
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `savedResource`：`savedResource` 参数。
     * - `oldResource`：`oldResource` 参数。
     * 返回：无。
     */
    @Override
    protected void onEntitySaved(User user, TbResource savedResource, TbResource oldResource) throws ThingsboardException {
        super.onEntitySaved(user, savedResource, oldResource);
        clusterService.onResourceChange(savedResource, null);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.TB_RESOURCE;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ResourceImportService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
