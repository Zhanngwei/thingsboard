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
package org.thingsboard.server.dao.model.sqlts.latest;

import lombok.Data;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.sqlts.latest.SearchTsKvLatestRepository;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TsKvLatestEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `AbstractTsKvEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@Entity
@Table(name = "ts_kv_latest")
@IdClass(TsKvLatestCompositeKey.class)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "tsKvLatestFindMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TsKvLatestEntity.class,
                                columns = {
                                        @ColumnResult(name = "entityId", type = UUID.class),
                                        @ColumnResult(name = "key", type = Integer.class),
                                        @ColumnResult(name = "strKey", type = String.class),
                                        @ColumnResult(name = "strValue", type = String.class),
                                        @ColumnResult(name = "boolValue", type = Boolean.class),
                                        @ColumnResult(name = "longValue", type = Long.class),
                                        @ColumnResult(name = "doubleValue", type = Double.class),
                                        @ColumnResult(name = "jsonValue", type = String.class),
                                        @ColumnResult(name = "ts", type = Long.class),

                                }
                        ),
                })
})
@NamedNativeQueries({
        @NamedNativeQuery(
                name = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID,
                query = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID_QUERY,
                resultSetMapping = "tsKvLatestFindMapping",
                resultClass = TsKvLatestEntity.class
        )
})
public final class TsKvLatestEntity extends AbstractTsKvEntity {

    /**
     * 功能：判断`Not Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null || jsonValue != null;
    }

    /**
     * 功能：创建 `TsKvLatestEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TsKvLatestEntity() {
    }

    /**
     * 功能：创建 `TsKvLatestEntity` 实例，并初始化必要字段。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `strKey`：键。
     * - `strValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TsKvLatestEntity(UUID entityId, Integer key, String strKey, String strValue, Boolean boolValue, Long longValue, Double doubleValue, String jsonValue, Long ts) {
        this.entityId = entityId;
        this.key = key;
        this.ts = ts;
        this.longValue = longValue;
        this.doubleValue = doubleValue;
        this.strValue = strValue;
        this.booleanValue = boolValue;
        this.jsonValue = jsonValue;
        this.strKey = strKey;
    }
}
