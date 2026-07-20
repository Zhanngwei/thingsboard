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
 * 1. `NotificationRequest` 是 ThingsBoard Common Data 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasTenantId`、`HasName`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
