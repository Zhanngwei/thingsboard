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
import org.thingsboard.server.common.data.alarm.EntityAlarm;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `EntityAlarmCompositeKey` 是 ThingsBoard DAO 中表示告警持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EntityAlarmCompositeKey implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    @Transient
    private static final long serialVersionUID = -245388185894468450L;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    private UUID entityId;
    private UUID alarmId;

    /**
     * 功能：创建 `EntityAlarmCompositeKey` 实例，并初始化必要字段。
     * 参数：
     * - `entityAlarm`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityAlarmCompositeKey(EntityAlarm entityAlarm) {
        this.entityId = entityAlarm.getEntityId().getId();
        this.alarmId = entityAlarm.getAlarmId().getId();
    }
}
