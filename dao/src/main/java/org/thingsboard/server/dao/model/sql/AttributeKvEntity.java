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

import lombok.Data;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

import static org.thingsboard.server.dao.model.ModelConstants.BOOLEAN_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.DOUBLE_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.JSON_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.LAST_UPDATE_TS_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.LONG_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.STRING_VALUE_COLUMN;

/**
 * 中文说明：
 * 1. `AttributeKvEntity` 是 ThingsBoard DAO 中表示属性持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `ToData`、`Serializable`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@Entity
@Table(name = "attribute_kv")
public class AttributeKvEntity implements ToData<AttributeKvEntry>, Serializable {

    /**
     * `id`ID，用于定位对应业务对象。
     */
    @EmbeddedId
    private AttributeKvCompositeKey id;

    /**
     * 是否满足值条件。
     */
    @Column(name = BOOLEAN_VALUE_COLUMN)
    private Boolean booleanValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = STRING_VALUE_COLUMN)
    private String strValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = LONG_VALUE_COLUMN)
    private Long longValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = DOUBLE_VALUE_COLUMN)
    private Double doubleValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = JSON_VALUE_COLUMN)
    private String jsonValue;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = LAST_UPDATE_TS_COLUMN)
    private Long lastUpdateTs;

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public AttributeKvEntry toData() {
        KvEntry kvEntry = null;
        if (strValue != null) {
            kvEntry = new StringDataEntry(id.getAttributeKey(), strValue);
        } else if (booleanValue != null) {
            kvEntry = new BooleanDataEntry(id.getAttributeKey(), booleanValue);
        } else if (doubleValue != null) {
            kvEntry = new DoubleDataEntry(id.getAttributeKey(), doubleValue);
        } else if (longValue != null) {
            kvEntry = new LongDataEntry(id.getAttributeKey(), longValue);
        } else if (jsonValue != null) {
            kvEntry = new JsonDataEntry(id.getAttributeKey(), jsonValue);
        }

        return new BaseAttributeKvEntry(kvEntry, lastUpdateTs);
    }
}
