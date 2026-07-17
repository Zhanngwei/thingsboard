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
 * 1. `TenantApiUsageState` 是 ThingsBoard Application 中承载租户信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseApiUsageState`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
