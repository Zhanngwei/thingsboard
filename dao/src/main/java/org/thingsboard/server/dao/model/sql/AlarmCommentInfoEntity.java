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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;

/**
 * 中文说明：
 * 1. `AlarmCommentInfoEntity` 是 ThingsBoard DAO 中表示告警持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `AbstractAlarmCommentEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmCommentInfoEntity extends AbstractAlarmCommentEntity<AlarmCommentInfo> {

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String firstName;
    private String lastName;

    /**
     * 邮箱，用于展示或标识当前对象。
     */
    private String email;

    /**
     * 功能：创建 `AlarmCommentInfoEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AlarmCommentInfoEntity() {
        super();
    }

    /**
     * 功能：创建 `AlarmCommentInfoEntity` 实例，并初始化必要字段。
     * 参数：
     * - `alarmCommentEntity`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AlarmCommentInfoEntity(AlarmCommentEntity alarmCommentEntity) {
        super(alarmCommentEntity);
    }

    /**
     * 功能：创建 `AlarmCommentInfoEntity` 实例，并初始化必要字段。
     * 参数：
     * - `alarmCommentEntity`：实体对象。
     * - `firstName`：名称。
     * - `lastName`：名称。
     * - `email`：`email` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmCommentInfoEntity(AlarmCommentEntity alarmCommentEntity, String firstName, String lastName, String email) {
        super(alarmCommentEntity);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public AlarmCommentInfo toData() {
        return new AlarmCommentInfo(super.toAlarmComment(), this.firstName, this.lastName, this.email);
    }
}
