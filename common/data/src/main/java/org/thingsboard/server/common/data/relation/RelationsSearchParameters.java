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
package org.thingsboard.server.common.data.relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

/**
 * Created by ashvayka on 03.05.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`RelationsSearchParameters` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
@AllArgsConstructor
public class RelationsSearchParameters {

    /**
     * `rootId`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "Root entity id to start search from.", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private UUID rootId;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 2, value = "Type of the root entity.")
    private EntityType rootType;
    /**
     * `direction` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 3, value = "Type of the root entity.")
    private EntitySearchDirection direction;
    /**
     * 关系，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 4, value = "Type of the relation.")
    private RelationTypeGroup relationTypeGroup;
    /**
     * `maxLevel` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 5, value = "Maximum level of the search depth.")
    private int maxLevel = 1;
    /**
     * 是否满足`fetchLastLevelOnly`条件。
     */
    @ApiModelProperty(position = 6, value = "Fetch entities that match the last level of search. Useful to find Devices that are strictly 'maxLevel' relations away from the root entity.")
    private boolean fetchLastLevelOnly;

    /**
     * 功能：创建 `RelationsSearchParameters` 实例，并初始化必要字段。
     * 参数：
     * - `entityId`：实体IDID。
     * - `direction`：`direction` 参数。
     * - `maxLevel`：`maxLevel` 参数。
     * - `fetchLastLevelOnly`：`fetchLastLevelOnly` 参数。
     * 返回：新创建的对象实例。
     */
    public RelationsSearchParameters(EntityId entityId, EntitySearchDirection direction, int maxLevel, boolean fetchLastLevelOnly) {
        this(entityId, direction, maxLevel, RelationTypeGroup.COMMON, fetchLastLevelOnly);
    }

    /**
     * 功能：创建 `RelationsSearchParameters` 实例，并初始化必要字段。
     * 参数：
     * - `entityId`：实体IDID。
     * - `direction`：`direction` 参数。
     * - `maxLevel`：`maxLevel` 参数。
     * - `relationTypeGroup`：类型。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public RelationsSearchParameters(EntityId entityId, EntitySearchDirection direction, int maxLevel, RelationTypeGroup relationTypeGroup, boolean fetchLastLevelOnly) {
        this.rootId = entityId.getId();
        this.rootType = entityId.getEntityType();
        this.direction = direction;
        this.maxLevel = maxLevel;
        this.relationTypeGroup = relationTypeGroup;
        this.fetchLastLevelOnly = fetchLastLevelOnly;
    }

    /**
     * 功能：获取实体ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public EntityId getEntityId() {
        return EntityIdFactory.getByTypeAndUuid(rootType, rootId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RelationsSearchParameters` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
