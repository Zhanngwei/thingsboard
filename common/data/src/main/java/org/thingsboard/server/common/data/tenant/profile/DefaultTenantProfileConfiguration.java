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
 * 1. 类目的：`DefaultTenantProfileConfiguration` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTenantProfileConfiguration` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
