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
package org.thingsboard.server.common.data.sync.ie;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.ThrowingRunnable;

/**
 * 中文说明：
 * 1. 类目的：`EntityImportResult` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
public class EntityImportResult<E extends ExportableEntity<? extends EntityId>> {

    /**
     * 实体对象，用于描述当前业务场景。
     */
    private E savedEntity;
    private E oldEntity;
    /**
     * 实体，用于区分不同处理分支。
     */
    private EntityType entityType;

    private ThrowingRunnable saveReferencesCallback = () -> {};
    private ThrowingRunnable sendEventsCallback = () -> {};

    /**
     * 是否满足`updatedAllExternalIds`条件。
     */
    private boolean updatedAllExternalIds = true;

    /**
     * 是否满足`created`条件。
     */
    private boolean created;
    private boolean updated;
    /**
     * 是否满足`updatedRelatedEntities`条件。
     */
    private boolean updatedRelatedEntities;

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    public void addSaveReferencesCallback(ThrowingRunnable callback) {
        this.saveReferencesCallback = this.saveReferencesCallback.andThen(callback);
    }

    /**
     * 功能：保存或创建回调。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    public void addSendEventsCallback(ThrowingRunnable callback) {
        this.sendEventsCallback = this.sendEventsCallback.andThen(callback);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EntityImportResult` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
