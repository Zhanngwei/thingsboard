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
import org.thingsboard.server.common.data.relation.EntityRelation;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `RelationCompositeKey` 是 ThingsBoard DAO 中表示实体关系持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RelationCompositeKey implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    @Transient
    private static final long serialVersionUID = -4089175869616037592L;

    /**
     * `fromId`ID，用于定位对应业务对象。
     */
    private UUID fromId;
    private String fromType;
    /**
     * `toId`ID，用于定位对应业务对象。
     */
    private UUID toId;
    private String toType;
    /**
     * 关系，用于区分不同处理分支。
     */
    private String relationType;
    private String relationTypeGroup;

    /**
     * 功能：创建 `RelationCompositeKey` 实例，并初始化必要字段。
     * 参数：
     * - `relation`：`relation` 参数。
     * 返回：新创建的对象实例。
     */
    public RelationCompositeKey(EntityRelation relation) {
        this.fromId = relation.getFrom().getId();
        this.fromType = relation.getFrom().getEntityType().name();
        this.toId = relation.getTo().getId();
        this.toType = relation.getTo().getEntityType().name();
        this.relationType = relation.getType();
        this.relationTypeGroup = relation.getTypeGroup().name();
    }
}
