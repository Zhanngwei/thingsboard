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
package org.thingsboard.server.service.entitiy.asset;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;

import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbAssetService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@AllArgsConstructor
public class DefaultTbAssetService extends AbstractTbEntityService implements TbAssetService {

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    private final AssetService assetService;
    private final TbAssetProfileCache assetProfileCache;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset save(Asset asset, User user) throws Exception {
        ActionType actionType = asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = asset.getTenantId();
        try {
            if (TB_SERVICE_QUEUE.equals(asset.getType())) {
                throw new ThingsboardException("Unable to save asset with type " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else if (asset.getAssetProfileId() != null) {
                AssetProfile assetProfile = assetProfileCache.get(tenantId, asset.getAssetProfileId());
                if (assetProfile != null && TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                    throw new ThingsboardException("Unable to save asset with profile " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            Asset savedAsset = checkNotNull(assetService.saveAsset(asset));
            autoCommit(user, savedAsset.getId());
            notificationEntityService.logEntityAction(tenantId, savedAsset.getId(), savedAsset, asset.getCustomerId(),
                    actionType, user);
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAsset.getId(),
                    asset.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), asset, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    @Transactional
    public void delete(Asset asset, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            removeAlarmsByOriginatorId(tenantId, assetId);
            assetService.deleteAsset(tenantId, assetId);
            notificationEntityService.logEntityAction(tenantId, assetId, asset, asset.getCustomerId(), actionType, user, assetId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetId, ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e,
                    assetId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignAssetToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, customerId));
            notificationEntityService.logEntityAction(tenantId, assetId, savedAsset, customerId, actionType, user,
                    assetId.toString(), customerId.toString(), customer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e,
                    assetId.toString(), customerId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignAssetToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset unassignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId));
            CustomerId customerId = customer.getId();
            notificationEntityService.logEntityAction(tenantId, assetId, savedAsset, customerId, actionType, user,
                    assetId.toString(), customerId.toString(), customer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignAssetToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset assignAssetToPublicCustomer(TenantId tenantId, AssetId assetId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, publicCustomer.getId()));
            CustomerId customerId = publicCustomer.getId();
            notificationEntityService.logEntityAction(tenantId, assetId, savedAsset, customerId, actionType, user,
                    assetId.toString(), customerId.toString(), publicCustomer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignAssetToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToEdge(tenantId, assetId, edgeId));
            notificationEntityService.logEntityAction(tenantId, assetId, savedAsset, savedAsset.getCustomerId(),
                    actionType, user, assetId.toString(), edgeId.toString(), edge.getName());
            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignAssetFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `asset`：`asset` 参数。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, Asset asset, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        AssetId assetId = asset.getId();
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromEdge(tenantId, assetId, edgeId));
            notificationEntityService.logEntityAction(tenantId, assetId, asset, asset.getCustomerId(),
                    actionType, user, assetId.toString(), edgeId.toString(), edge.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbAssetService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
