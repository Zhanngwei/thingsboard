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
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_ALARM_ID;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_COMMENT;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_TYPE;

/**
 * 中文说明：
 * 1. `AbstractAlarmCommentEntity` 是 ThingsBoard DAO 中表示告警持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `AlarmComment`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractAlarmCommentEntity<T extends AlarmComment> extends BaseSqlEntity<T> implements BaseEntity<T> {

    /**
     * 告警ID，用于定位对应业务对象。
     */
    @Column(name = ALARM_COMMENT_ALARM_ID, columnDefinition = "uuid")
    private UUID alarmId;

    /**
     * 用户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ALARM_COMMENT_USER_ID)
    private UUID userId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = ALARM_COMMENT_TYPE)
    private AlarmCommentType type;

    /**
     * `comment` 字段，保存当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ALARM_COMMENT_COMMENT)
    private JsonNode comment;

    /**
     * 功能：创建 `AbstractAlarmCommentEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractAlarmCommentEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractAlarmCommentEntity` 实例，并初始化必要字段。
     * 参数：
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractAlarmCommentEntity(AlarmComment alarmComment) {
        if (alarmComment.getId() != null) {
            this.setUuid(alarmComment.getUuidId());
        }
        this.setCreatedTime(alarmComment.getCreatedTime());
        this.alarmId = alarmComment.getAlarmId().getId();
        if (alarmComment.getUserId() != null) {
            this.userId = alarmComment.getUserId().getId();
        }
        if (alarmComment.getType() != null) {
            this.type = alarmComment.getType();
        }
        this.setComment(alarmComment.getComment());
    }

    /**
     * 功能：创建 `AbstractAlarmCommentEntity` 实例，并初始化必要字段。
     * 参数：
     * - `alarmCommentEntity`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AbstractAlarmCommentEntity(AlarmCommentEntity alarmCommentEntity) {
        this.setId(alarmCommentEntity.getId());
        this.setCreatedTime(alarmCommentEntity.getCreatedTime());
        this.userId = alarmCommentEntity.getUserId();
        this.alarmId = alarmCommentEntity.getAlarmId();
        this.type = alarmCommentEntity.getType();
        this.comment = alarmCommentEntity.getComment();
    }
    /**
     * 功能：执行 `toAlarmComment` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected AlarmComment toAlarmComment() {
        AlarmComment alarmComment = new AlarmComment(new AlarmCommentId(id));
        alarmComment.setCreatedTime(createdTime);
        alarmComment.setAlarmId(new AlarmId(alarmId));
        if (userId != null) {
            alarmComment.setUserId(new UserId(userId));
        }
        alarmComment.setType(type);
        alarmComment.setComment(comment);
        return alarmComment;
    }
}
