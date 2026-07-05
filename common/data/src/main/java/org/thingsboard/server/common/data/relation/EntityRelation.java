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
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.validation.Length;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. 类目的：`EntityRelation` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Slf4j
@ApiModel
public class EntityRelation implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2807343040519543363L;

    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_TYPE = "ManagedByEdge";
    public static final String CONTAINS_TYPE = "Contains";
    /**
     * 类型常量，用于统一引用固定值。
     */
    public static final String MANAGES_TYPE = "Manages";

    /**
     * `from` 字段，保存当前对象的对应属性。
     */
    private EntityId from;
    private EntityId to;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Length(fieldName = "type")
    private String type;
    private RelationTypeGroup typeGroup;
    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    private transient JsonNode additionalInfo;
    /**
     * 扩展信息列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] additionalInfoBytes;

    /**
     * 功能：创建 `EntityRelation` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EntityRelation() {
        super();
    }

    /**
     * 功能：创建 `EntityRelation` 实例，并初始化必要字段。
     * 参数：
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `type`：类型。
     * 返回：新创建的对象实例。
     */
    public EntityRelation(EntityId from, EntityId to, String type) {
        this(from, to, type, RelationTypeGroup.COMMON);
    }

    /**
     * 功能：创建 `EntityRelation` 实例，并初始化必要字段。
     * 参数：
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `type`：类型。
     * - `typeGroup`：类型。
     * 返回：新创建的对象实例。
     */
    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        this(from, to, type, typeGroup, null);
    }

    /**
     * 功能：创建 `EntityRelation` 实例，并初始化必要字段。
     * 参数：
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `type`：类型。
     * - `typeGroup`：类型。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup, JsonNode additionalInfo) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.typeGroup = typeGroup;
        this.additionalInfo = additionalInfo;
    }

    /**
     * 功能：创建 `EntityRelation` 实例，并初始化必要字段。
     * 参数：
     * - `entityRelation`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityRelation(EntityRelation entityRelation) {
        this.from = entityRelation.getFrom();
        this.to = entityRelation.getTo();
        this.type = entityRelation.getType();
        this.typeGroup = entityRelation.getTypeGroup();
        this.additionalInfo = entityRelation.getAdditionalInfo();
    }

    /**
     * 功能：获取`From`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with [from] Entity Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public EntityId getFrom() {
        return from;
    }

    /**
     * 功能：更新`From`。
     * 参数：
     * - `from`：`from` 参数。
     * 返回：无。
     */
    public void setFrom(EntityId from) {
        this.from = from;
    }

    /**
     * 功能：获取`To`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 2, value = "JSON object with [to] Entity Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public EntityId getTo() {
        return to;
    }

    /**
     * 功能：更新`To`。
     * 参数：
     * - `to`：`to` 参数。
     * 返回：无。
     */
    public void setTo(EntityId to) {
        this.to = to;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 3, value = "String value of relation type.", example = "Contains")
    public String getType() {
        return type;
    }

    /**
     * 功能：更新类型。
     * 参数：
     * - `type`：类型。
     * 返回：无。
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 4, value = "Represents the type group of the relation.", example = "COMMON")
    public RelationTypeGroup getTypeGroup() {
        return typeGroup;
    }

    /**
     * 功能：更新类型。
     * 参数：
     * - `typeGroup`：类型。
     * 返回：无。
     */
    public void setTypeGroup(RelationTypeGroup typeGroup) {
        this.typeGroup = typeGroup;
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 5, value = "Additional parameters of the relation", dataType = "com.fasterxml.jackson.databind.JsonNode")
    public JsonNode getAdditionalInfo() {
        return BaseDataWithAdditionalInfo.getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    /**
     * 功能：更新扩展信息。
     * 参数：
     * - `addInfo`：`addInfo` 参数。
     * 返回：无。
     */
    public void setAdditionalInfo(JsonNode addInfo) {
        BaseDataWithAdditionalInfo.setJson(addInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityRelation that = (EntityRelation) o;

        if (from != null ? !from.equals(that.from) : that.from != null) return false;
        if (to != null ? !to.equals(that.to) : that.to != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return typeGroup == that.typeGroup;
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (typeGroup != null ? typeGroup.hashCode() : 0);
        return result;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityRelation` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
