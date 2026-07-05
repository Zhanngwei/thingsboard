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
package org.thingsboard.server.common.data.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`NotificationRequest` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest extends BaseData<NotificationRequestId> implements HasTenantId, HasName {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    /**
     * `targets`列表，用于保存一组待处理对象。
     */
    @NotEmpty
    private List<UUID> targets;

    /**
     * `templateId`ID，用于定位对应业务对象。
     */
    private NotificationTemplateId templateId;
    /**
     * `template` 字段，保存当前对象的对应属性。
     */
    @Valid
    private NotificationTemplate template;
    /**
     * 信息对象，表示当前对象的对应属性。
     */
    @Valid
    private NotificationInfo info;
    /**
     * 配置，保存当前对象的配置选项。
     */
    @Valid
    private NotificationRequestConfig additionalConfig;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    private EntityId originatorEntityId;
    private NotificationRuleId ruleId;

    /**
     * 状态，表示当前对象所处状态。
     */
    private NotificationRequestStatus status;

    /**
     * `stats` 字段，保存当前对象的对应属性。
     */
    private NotificationRequestStats stats;

    /**
     * 功能：创建 `NotificationRequest` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationRequest(NotificationRequest other) {
        super(other);
        this.tenantId = other.tenantId;
        this.targets = other.targets;
        this.templateId = other.templateId;
        this.template = other.template;
        this.info = other.info;
        this.additionalConfig = other.additionalConfig;
        this.originatorEntityId = other.originatorEntityId;
        this.ruleId = other.ruleId;
        this.status = other.status;
        this.stats = other.stats;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    @Override
    public String getName() {
        return "To targets " + targets;
    }

    /**
     * 功能：获取`Sender Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public UserId getSenderId() {
        return originatorEntityId instanceof UserId ? (UserId) originatorEntityId : null;
    }

    /**
     * 功能：判断`Sent`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isSent() {
        return status == NotificationRequestStatus.SENT;
    }

    /**
     * 功能：判断`Scheduled`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean isScheduled() {
        return status == NotificationRequestStatus.SCHEDULED;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationRequest` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
