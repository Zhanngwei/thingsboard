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
package org.thingsboard.server.service.apiusage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`TenantApiUsageState` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class TenantApiUsageState extends BaseApiUsageState {
    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    @Setter
    private TenantProfileId tenantProfileId;
    /**
     * 租户，保存当前步骤读取或计算得到的内容。
     */
    @Getter
    @Setter
    private TenantProfileData tenantProfileData;

    /**
     * 功能：创建 `TenantApiUsageState` 实例，并初始化必要字段。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * - `apiUsageState`：`apiUsageState` 参数。
     * 返回：新创建的对象实例。
     */
    public TenantApiUsageState(TenantProfile tenantProfile, ApiUsageState apiUsageState) {
        super(apiUsageState);
        this.tenantProfileId = tenantProfile.getId();
        this.tenantProfileData = tenantProfile.getProfileData();
    }

    /**
     * 功能：创建 `TenantApiUsageState` 实例，并初始化必要字段。
     * 参数：
     * - `apiUsageState`：`apiUsageState` 参数。
     * 返回：新创建的对象实例。
     */
    public TenantApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    public long getProfileThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getProfileThreshold(key);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    public boolean getProfileFeatureEnabled(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getProfileFeatureEnabled(key);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    public long getProfileWarnThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getWarnThreshold(key);
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `feature`：`feature` 参数。
     * 返回：判断结果。
     */
    private Pair<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(ApiFeature feature) {
        ApiUsageStateValue featureValue = ApiUsageStateValue.ENABLED;
        for (ApiUsageRecordKey recordKey : ApiUsageRecordKey.getKeys(feature)) {
            long value = get(recordKey);
            boolean featureEnabled = getProfileFeatureEnabled(recordKey);
            ApiUsageStateValue tmpValue;
            if (featureEnabled) {
                long threshold = getProfileThreshold(recordKey);
                long warnThreshold = getProfileWarnThreshold(recordKey);
                if (threshold == 0 || value == 0 || value < warnThreshold) {
                    tmpValue = ApiUsageStateValue.ENABLED;
                } else if (value < threshold) {
                    tmpValue = ApiUsageStateValue.WARNING;
                } else {
                    tmpValue = ApiUsageStateValue.DISABLED;
                }
            } else {
                tmpValue = ApiUsageStateValue.DISABLED;
            }
            featureValue = ApiUsageStateValue.toMoreRestricted(featureValue, tmpValue);
        }
        return setFeatureValue(feature, featureValue) ? Pair.of(feature, featureValue) : null;
    }


    /**
     * 功能：校验状态。
     * 参数：无。
     * 返回：判断结果。
     */
    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThresholds() {
        return checkStateUpdatedDueToThreshold(new HashSet<>(Arrays.asList(ApiFeature.values())));
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `features`：`features` 参数。
     * 返回：判断结果。
     */
    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(Set<ApiFeature> features) {
        Map<ApiFeature, ApiUsageStateValue> result = new HashMap<>();
        for (ApiFeature feature : features) {
            Pair<ApiFeature, ApiUsageStateValue> tmp = checkStateUpdatedDueToThreshold(feature);
            if (tmp != null) {
                result.put(tmp.getFirst(), tmp.getSecond());
            }
        }
        return result;
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TenantApiUsageState` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
