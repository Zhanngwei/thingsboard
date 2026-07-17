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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. `AlarmComment` 是 ThingsBoard Common Data 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasName`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
@Builder
@AllArgsConstructor
public class AlarmComment extends BaseData<AlarmCommentId> implements HasName {
    /**
     * 告警ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Alarm id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId alarmId;
    /**
     * 用户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 4, value = "JSON object with User id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private UserId userId;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 5, value = "Defines origination of comment. System type means comment was created by TB. OTHER type means comment was created by user.", example = "SYSTEM/OTHER", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private AlarmCommentType type;
    /**
     * `comment` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 6, value = "JSON object with text of comment.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @NoXss
    @Length(fieldName = "comment", max = 10000)
    @EqualsAndHashCode.Include
    private transient JsonNode comment;

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the alarm comment Id. " +
            "Specify this field to update the alarm comment. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm." )
    @Override
    public AlarmCommentId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the alarm comment creation, in milliseconds", example = "1634058704567", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：创建 `AlarmComment` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AlarmComment() {
        super();
    }

    /**
     * 功能：创建 `AlarmComment` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public AlarmComment(AlarmCommentId id) {
        super(id);
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ApiModelProperty(position = 5, required = true, value = "representing comment text", example = "Please take a look")
    public String getName() {
        return comment.toString();
    }

    /**
     * 功能：创建 `AlarmComment` 实例，并初始化必要字段。
     * 参数：
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmComment(AlarmComment alarmComment) {
        super(alarmComment.getId());
        this.createdTime = alarmComment.getCreatedTime();
        this.alarmId = alarmComment.getAlarmId();
        this.type = alarmComment.getType();
        this.comment = alarmComment.getComment();
        this.userId = alarmComment.getUserId();
    }
}
