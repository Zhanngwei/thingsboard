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
package org.thingsboard.server.service.sync.vc.data;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`EntitiesExportCtx` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
@Slf4j
@Data
public abstract class EntitiesExportCtx<R extends VersionCreateRequest> {

    /**
     * 用户对象，用于描述当前业务场景。
     */
    protected final User user;
    protected final CommitGitRequest commit;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    protected final R request;
    private final List<ListenableFuture<Void>> futures;
    /**
     * `externalIdMap`映射关系，用于按键查找对应值。
     */
    private final Map<EntityId, EntityId> externalIdMap;

    /**
     * 功能：创建 `EntitiesExportCtx` 实例，并初始化必要字段。
     * 参数：
     * - `user`：`user` 参数。
     * - `commit`：`commit` 参数。
     * - `request`：请求对象。
     * 返回：新创建的对象实例。
     */
    public EntitiesExportCtx(User user, CommitGitRequest commit, R request) {
        this.user = user;
        this.commit = commit;
        this.request = request;
        this.futures = new ArrayList<>();
        this.externalIdMap = new HashMap<>();
    }

    /**
     * 功能：创建 `EntitiesExportCtx` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    protected <T extends R> EntitiesExportCtx(EntitiesExportCtx<T> other) {
        this.user = other.getUser();
        this.commit = other.getCommit();
        this.request = other.getRequest();
        this.futures = other.getFutures();
        this.externalIdMap = other.getExternalIdMap();
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * 返回：无。
     */
    public void add(ListenableFuture<Void> future) {
        futures.add(future);
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantId getTenantId() {
        return user.getTenantId();
    }

    /**
     * 功能：构建配置。
     * 参数：
     * - `config`：配置对象。
     * 返回：匹配的数据集合。
     */
    protected static EntityExportSettings buildExportSettings(VersionCreateConfig config) {
        return EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .build();
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public abstract EntityExportSettings getSettings();

    /**
     * 功能：获取`External Id`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    public <ID extends EntityId> ID getExternalId(ID internalId) {
        var result = externalIdMap.get(internalId);
        log.debug("[{}][{}] Local cache {} for id", internalId.getEntityType(), internalId.getId(), result != null ? "hit" : "miss");
        return (ID) result;
    }

    /**
     * 功能：执行 `putExternalId` 对应的处理。
     * 参数：
     * - `internalId`：`internalId`ID。
     * - `externalId`：`externalId`ID。
     * 返回：无。
     */
    public void putExternalId(EntityId internalId, EntityId externalId) {
        log.debug("[{}][{}] Local cache put: {}", internalId.getEntityType(), internalId.getId(), externalId);
        externalIdMap.put(internalId, externalId != null ? externalId : internalId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntitiesExportCtx` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
