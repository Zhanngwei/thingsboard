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
 * 1. `EntityExportData` 是 ThingsBoard Common Data 中承载实体信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `ExportableEntity`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
