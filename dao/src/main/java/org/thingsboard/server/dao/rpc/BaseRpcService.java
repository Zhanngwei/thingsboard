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
package org.thingsboard.server.dao.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

/**
 * 中文说明：
 * 1. 类目的：`BaseRpcService` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Service("RpcDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseRpcService implements RpcService {
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_RPC_ID = "Incorrect rpcId ";

    /**
     * RPC，用于读取或保存对应领域对象。
     */
    private final RpcDao rpcDao;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `rpc`：`rpc` 参数。
     * 返回：处理结果。
     */
    @Override
    public Rpc save(Rpc rpc) {
        log.trace("Executing save, [{}]", rpc);
        return rpcDao.save(rpc.getTenantId(), rpc);
    }

    /**
     * 功能：删除或清理RPC。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rpcId`：RPCID。
     * 返回：无。
     */
    @Override
    public void deleteRpc(TenantId tenantId, RpcId rpcId) {
        log.trace("Executing deleteRpc, tenantId [{}], rpcId [{}]", tenantId, rpcId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(rpcId, INCORRECT_RPC_ID + rpcId);
        rpcDao.removeById(tenantId, rpcId.getId());
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteAllRpcByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAllRpcByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantRpcRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：获取`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rpcId`：RPCID。
     * 返回：处理结果。
     */
    @Override
    public Rpc findById(TenantId tenantId, RpcId rpcId) {
        log.trace("Executing findById, tenantId [{}], rpcId [{}]", tenantId, rpcId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(rpcId, INCORRECT_RPC_ID + rpcId);
        return rpcDao.findById(tenantId, rpcId.getId());
    }

    /**
     * 功能：获取RPC。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rpcId`：RPCID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Rpc> findRpcByIdAsync(TenantId tenantId, RpcId rpcId) {
        log.trace("Executing findRpcByIdAsync, tenantId [{}], rpcId: [{}]", tenantId, rpcId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(rpcId, INCORRECT_RPC_ID + rpcId);
        return rpcDao.findByIdAsync(tenantId, rpcId.getId());
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `rpcStatus`：`rpcStatus` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        log.trace("Executing findAllByDeviceIdAndStatus, tenantId [{}], deviceId [{}], rpcStatus [{}], pageLink [{}]", tenantId, deviceId, rpcStatus, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return rpcDao.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Rpc> findAllByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink) {
        log.trace("Executing findAllByDeviceIdAndStatus, tenantId [{}], deviceId [{}], pageLink [{}]", tenantId, deviceId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return rpcDao.findAllByDeviceId(tenantId, deviceId, pageLink);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new RpcId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.RPC;
    }

    private PaginatedRemover<TenantId, Rpc> tenantRpcRemover =
            new PaginatedRemover<>() {
                @Override
                protected PageData<Rpc> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return rpcDao.findAllRpcByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Rpc entity) {
                    deleteRpc(tenantId, entity.getId());
                }
            };
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseRpcService` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
