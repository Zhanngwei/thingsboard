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
 * 1. 类目的：`AlarmComment` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`AlarmComment` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
