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
package org.thingsboard.server.common.data.tenant.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfileType;

/**
 * 中文说明：
 * 1. `DefaultTenantProfileConfiguration` 是 ThingsBoard Common Data 中描述租户行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `TenantProfileConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DefaultTenantProfileConfiguration implements TenantProfileConfiguration {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -7134932690332578595L;

    /**
     * `maxDevices` 字段，保存当前对象的对应属性。
     */
    private long maxDevices;
    private long maxAssets;
    /**
     * `maxCustomers` 字段，保存当前对象的对应属性。
     */
    private long maxCustomers;
    private long maxUsers;
    /**
     * `maxDashboards` 字段，保存当前对象的对应属性。
     */
    private long maxDashboards;
    private long maxRuleChains;
    /**
     * `maxResourcesInBytes` 字段，保存当前对象的对应属性。
     */
    private long maxResourcesInBytes;
    private long maxOtaPackagesInBytes;
    /**
     * `maxResourceSize` 字段，保存当前对象的对应属性。
     */
    private long maxResourceSize;

    /**
     * 租户，承载当前步骤需要处理的内容。
     */
    private String transportTenantMsgRateLimit;
    private String transportTenantTelemetryMsgRateLimit;
    /**
     * 租户，保存当前步骤读取或计算得到的内容。
     */
    private String transportTenantTelemetryDataPointsRateLimit;
    private String transportDeviceMsgRateLimit;
    /**
     * 设备，承载当前步骤需要处理的内容。
     */
    private String transportDeviceTelemetryMsgRateLimit;
    private String transportDeviceTelemetryDataPointsRateLimit;

    /**
     * 租户，用于控制数量、位置或分页范围。
     */
    private String tenantEntityExportRateLimit;
    private String tenantEntityImportRateLimit;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private String tenantNotificationRequestsRateLimit;
    private String tenantNotificationRequestsPerRuleRateLimit;

    /**
     * 传输层，表示当前对象的对应属性。
     */
    private long maxTransportMessages;
    private long maxTransportDataPoints;
    /**
     * `maxREExecutions` 字段，保存当前对象的对应属性。
     */
    private long maxREExecutions;
    private long maxJSExecutions;
    /**
     * `maxTbelExecutions` 字段，保存当前对象的对应属性。
     */
    private long maxTbelExecutions;
    private long maxDPStorageDays;
    /**
     * 规则节点，承载当前步骤需要处理的内容。
     */
    private int maxRuleNodeExecutionsPerMessage;
    private long maxEmails;
    /**
     * 是否启用`sms`。
     */
    private Boolean smsEnabled;
    private long maxSms;
    /**
     * `maxCreatedAlarms` 字段，保存当前对象的对应属性。
     */
    private long maxCreatedAlarms;

    /**
     * 租户，保存当前对象的配置选项。
     */
    private String tenantServerRestLimitsConfiguration;
    private String customerServerRestLimitsConfiguration;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    private int maxWsSessionsPerTenant;
    private int maxWsSessionsPerCustomer;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    private int maxWsSessionsPerRegularUser;
    private int maxWsSessionsPerPublicUser;
    /**
     * 队列，承载当前步骤需要处理的内容。
     */
    private int wsMsgQueueLimitPerSession;
    private long maxWsSubscriptionsPerTenant;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    private long maxWsSubscriptionsPerCustomer;
    private long maxWsSubscriptionsPerRegularUser;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    private long maxWsSubscriptionsPerPublicUser;
    private String wsUpdatesPerSessionRateLimit;

    /**
     * 租户，保存当前对象的配置选项。
     */
    private String cassandraQueryTenantRateLimitsConfiguration;

    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    private String edgeEventRateLimits;
    private String edgeEventRateLimitsPerEdge;
    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    private String edgeUplinkMessagesRateLimits;
    private String edgeUplinkMessagesRateLimitsPerEdge;

    /**
     * `defaultStorageTtlDays` 字段，保存当前对象的对应属性。
     */
    private int defaultStorageTtlDays;
    private int alarmsTtlDays;
    /**
     * RPC，表示当前对象的对应属性。
     */
    private int rpcTtlDays;
    private int queueStatsTtlDays;
    /**
     * 规则引擎，表示当前对象的对应属性。
     */
    private int ruleEngineExceptionsTtlDays;

    /**
     * 阈值，用于判断是否达到处理条件。
     */
    private double warnThreshold;

    /**
     * 功能：获取配置。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    @Override
    public long getProfileThreshold(ApiUsageRecordKey key) {
        switch (key) {
            case TRANSPORT_MSG_COUNT:
                return maxTransportMessages;
            case TRANSPORT_DP_COUNT:
                return maxTransportDataPoints;
            case JS_EXEC_COUNT:
                return maxJSExecutions;
            case TBEL_EXEC_COUNT:
                return maxTbelExecutions;
            case RE_EXEC_COUNT:
                return maxREExecutions;
            case STORAGE_DP_COUNT:
                return maxDPStorageDays;
            case EMAIL_EXEC_COUNT:
                return maxEmails;
            case SMS_EXEC_COUNT:
                return maxSms;
            case CREATED_ALARMS_COUNT:
                return maxCreatedAlarms;
        }
        return 0L;
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    @Override
    public boolean getProfileFeatureEnabled(ApiUsageRecordKey key) {
        switch (key) {
            case SMS_EXEC_COUNT:
                return smsEnabled == null || Boolean.TRUE.equals(smsEnabled);
            default:
                return true;
        }
    }

    /**
     * 功能：获取阈值。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    @Override
    public long getWarnThreshold(ApiUsageRecordKey key) {
        return (long) (getProfileThreshold(key) * (warnThreshold > 0.0 ? warnThreshold : 0.8));
    }

    /**
     * 功能：获取数量限制。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：数值结果。
     */
    public long getEntitiesLimit(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return maxDevices;
            case ASSET:
                return maxAssets;
            case CUSTOMER:
                return maxCustomers;
            case USER:
                return maxUsers;
            case DASHBOARD:
                return maxDashboards;
            case RULE_CHAIN:
                return maxRuleChains;
            default:
                return 0;
        }
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TenantProfileType getType() {
        return TenantProfileType.DEFAULT;
    }

    /**
     * 功能：获取规则节点。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getMaxRuleNodeExecsPerMessage() {
        return maxRuleNodeExecutionsPerMessage;
    }
}
