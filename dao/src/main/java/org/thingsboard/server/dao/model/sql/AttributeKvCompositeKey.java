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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ATTRIBUTE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ATTRIBUTE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;

/**
 * 中文说明：
 * 1. `AttributeKvCompositeKey` 是 ThingsBoard DAO 中表示属性持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class AttributeKvCompositeKey implements Serializable {
    /**
     * 实体，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_COLUMN)
    private EntityType entityType;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    private UUID entityId;
    /**
     * 属性，用于区分不同处理分支。
     */
    @Column(name = ATTRIBUTE_TYPE_COLUMN)
    private String attributeType;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Column(name = ATTRIBUTE_KEY_COLUMN)
    private String attributeKey;
}
