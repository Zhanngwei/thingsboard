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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

/**
 * 中文说明：
 * 1. `ApiUsageLimitNotificationInfo` 是 ThingsBoard Common Data 中承载通知信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `RuleOriginatedNotificationInfo`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiUsageLimitNotificationInfo implements RuleOriginatedNotificationInfo {

    /**
     * 功能项，表示当前对象的对应属性。
     */
    private ApiFeature feature;
    private ApiUsageRecordKey recordKey;
    /**
     * 状态，表示当前对象所处状态。
     */
    private ApiUsageStateValue status;
    private String limit;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private String currentValue;
    private TenantId tenantId;
    /**
     * 租户，用于标识或展示当前对象。
     */
    private String tenantName;

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "feature", feature.getLabel(),
                "unitLabel", recordKey.getUnitLabel(),
                "status", status.name().toLowerCase(),
                "limit", limit,
                "currentValue", currentValue,
                "tenantId", tenantId.toString(),
                "tenantName", tenantName
        );
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TenantId getAffectedTenantId() {
        return tenantId;
    }

}
