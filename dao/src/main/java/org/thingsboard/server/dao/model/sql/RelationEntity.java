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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_FROM_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_FROM_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TO_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TO_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TYPE_GROUP_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.RELATION_TYPE_PROPERTY;

/**
 * 中文说明：
 * 1. `RelationEntity` 是 ThingsBoard DAO 中表示实体关系持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `ToData`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = RELATION_TABLE_NAME)
@IdClass(RelationCompositeKey.class)
public final class RelationEntity implements ToData<EntityRelation> {

    /**
     * `fromId`ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = RELATION_FROM_ID_PROPERTY, columnDefinition = "uuid")
    private UUID fromId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Id
    @Column(name = RELATION_FROM_TYPE_PROPERTY)
    private String fromType;

    /**
     * `toId`ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = RELATION_TO_ID_PROPERTY, columnDefinition = "uuid")
    private UUID toId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Id
    @Column(name = RELATION_TO_TYPE_PROPERTY)
    private String toType;

    /**
     * 关系，用于区分不同处理分支。
     */
    @Id
    @Column(name = RELATION_TYPE_GROUP_PROPERTY)
    private String relationTypeGroup;

    /**
     * 关系，用于区分不同处理分支。
     */
    @Id
    @Column(name = RELATION_TYPE_PROPERTY)
    private String relationType;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `RelationEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RelationEntity() {
        super();
    }

    /**
     * 功能：创建 `RelationEntity` 实例，并初始化必要字段。
     * 参数：
     * - `relation`：`relation` 参数。
     * 返回：新创建的对象实例。
     */
    public RelationEntity(EntityRelation relation) {
        if (relation.getTo() != null) {
            this.toId = relation.getTo().getId();
            this.toType = relation.getTo().getEntityType().name();
        }
        if (relation.getFrom() != null) {
            this.fromId = relation.getFrom().getId();
            this.fromType = relation.getFrom().getEntityType().name();
        }
        this.relationType = relation.getType();
        this.relationTypeGroup = relation.getTypeGroup().name();
        this.additionalInfo = relation.getAdditionalInfo();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityRelation toData() {
        EntityRelation relation = new EntityRelation();
        if (toId != null && toType != null) {
            relation.setTo(EntityIdFactory.getByTypeAndUuid(toType, toId));
        }
        if (fromId != null && fromType != null) {
            relation.setFrom(EntityIdFactory.getByTypeAndUuid(fromType, fromId));
        }
        relation.setType(relationType);
        relation.setTypeGroup(RelationTypeGroup.valueOf(relationTypeGroup));
        relation.setAdditionalInfo(additionalInfo);
        return relation;
    }
}