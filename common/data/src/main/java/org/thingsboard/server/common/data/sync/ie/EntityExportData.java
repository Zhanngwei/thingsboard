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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.sync.JsonTbEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`EntityExportData` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "entityType", include = As.EXISTING_PROPERTY, visible = true, defaultImpl = EntityExportData.class)
@JsonSubTypes({
        @Type(name = "DEVICE", value = DeviceExportData.class),
        @Type(name = "RULE_CHAIN", value = RuleChainExportData.class),
        @Type(name = "WIDGET_TYPE", value = WidgetTypeExportData.class),
        @Type(name = "WIDGETS_BUNDLE", value = WidgetsBundleExportData.class)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class EntityExportData<E extends ExportableEntity<? extends EntityId>> {

    public static final Comparator<EntityRelation> relationsComparator = Comparator
            .comparing(EntityRelation::getFrom, Comparator.comparing(EntityId::getId))
            .thenComparing(EntityRelation::getTo, Comparator.comparing(EntityId::getId))
            .thenComparing(EntityRelation::getTypeGroup)
            .thenComparing(EntityRelation::getType);

    public static final Comparator<AttributeExportData> attrComparator = Comparator
            .comparing(AttributeExportData::getKey).thenComparing(AttributeExportData::getLastUpdateTs);

    /**
     * 实体对象，用于描述当前业务场景。
     */
    @JsonProperty(index = 2)
    @JsonTbEntity
    private E entity;
    /**
     * 实体，用于区分不同处理分支。
     */
    @JsonProperty(index = 1)
    private EntityType entityType;

    /**
     * `relations`列表，用于保存一组待处理对象。
     */
    @JsonProperty(index = 100)
    private List<EntityRelation> relations;
    /**
     * `attributes`列表，用于保存一组待处理对象。
     */
    @JsonProperty(index = 101)
    private Map<String, List<AttributeExportData>> attributes;

    /**
     * 功能：执行 `sort` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public EntityExportData<E> sort() {
        if (relations != null && !relations.isEmpty()) {
            relations.sort(relationsComparator);
        }
        if (attributes != null && !attributes.isEmpty()) {
            attributes.values().forEach(list -> list.sort(attrComparator));
        }
        return this;
    }

    /**
     * 功能：获取`External Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public EntityId getExternalId() {
        return entity.getExternalId() != null ? entity.getExternalId() : entity.getId();
    }

    /**
     * 功能：判断凭据。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean hasCredentials() {
        return false;
    }

    /**
     * 功能：判断`Attributes`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean hasAttributes() {
        return attributes != null;
    }

    /**
     * 功能：判断`Relations`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean hasRelations() {
        return relations != null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityExportData` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
