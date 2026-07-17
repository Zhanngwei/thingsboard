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
package org.thingsboard.server.dao.usage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * 中文说明：
 * 1. `BasicUsageInfoService` 是 ThingsBoard DAO 中负责用量统计的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `UsageInfoService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BasicUsageInfoService implements UsageInfoService {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final EntityCountService countService;
    private final ApiUsageStateService apiUsageStateService;
    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    private final TimeseriesService tsService;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Lazy
    private final TbTenantProfileCache tenantProfileCache;

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public UsageInfo getUsageInfo(TenantId tenantId) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        UsageInfo usageInfo = new UsageInfo();
        usageInfo.setDevices(countService.countByTenantIdAndEntityType(tenantId, EntityType.DEVICE));
        usageInfo.setMaxDevices(profileConfiguration.getMaxDevices());
        usageInfo.setAssets(countService.countByTenantIdAndEntityType(tenantId, EntityType.ASSET));
        usageInfo.setMaxAssets(profileConfiguration.getMaxAssets());
        usageInfo.setCustomers(countService.countByTenantIdAndEntityType(tenantId, EntityType.CUSTOMER));
        usageInfo.setMaxCustomers(profileConfiguration.getMaxCustomers());
        usageInfo.setUsers(countService.countByTenantIdAndEntityType(tenantId, EntityType.USER));
        usageInfo.setMaxUsers(profileConfiguration.getMaxUsers());
        usageInfo.setDashboards(countService.countByTenantIdAndEntityType(tenantId, EntityType.DASHBOARD));
        usageInfo.setMaxDashboards(profileConfiguration.getMaxDashboards());

        usageInfo.setMaxAlarms(profileConfiguration.getMaxCreatedAlarms());
        usageInfo.setMaxTransportMessages(profileConfiguration.getMaxTransportMessages());
        usageInfo.setMaxJsExecutions(profileConfiguration.getMaxJSExecutions());
        usageInfo.setMaxTbelExecutions(profileConfiguration.getMaxTbelExecutions());
        usageInfo.setMaxEmails(profileConfiguration.getMaxEmails());
        usageInfo.setMaxSms(profileConfiguration.getMaxSms());
        usageInfo.setSmsEnabled(profileConfiguration.getSmsEnabled());
        ApiUsageState apiUsageState = apiUsageStateService.findTenantApiUsageState(tenantId);
        if (apiUsageState != null) {
            Collection<String> keys = Arrays.asList(
                    ApiUsageRecordKey.TRANSPORT_MSG_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.JS_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.TBEL_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.EMAIL_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.SMS_EXEC_COUNT.getApiCountKey(),
                    ApiUsageRecordKey.CREATED_ALARMS_COUNT.getApiCountKey());
            try {
                List<TsKvEntry> entries = tsService.findLatest(tenantId, apiUsageState.getId(), keys).get();
                usageInfo.setTransportMessages(getLongValueFromTsEntries(entries, ApiUsageRecordKey.TRANSPORT_MSG_COUNT.getApiCountKey()));
                usageInfo.setJsExecutions(getLongValueFromTsEntries(entries, ApiUsageRecordKey.JS_EXEC_COUNT.getApiCountKey()));
                usageInfo.setTbelExecutions(getLongValueFromTsEntries(entries, ApiUsageRecordKey.TBEL_EXEC_COUNT.getApiCountKey()));
                usageInfo.setEmails(getLongValueFromTsEntries(entries, ApiUsageRecordKey.EMAIL_EXEC_COUNT.getApiCountKey()));
                usageInfo.setSms(getLongValueFromTsEntries(entries, ApiUsageRecordKey.SMS_EXEC_COUNT.getApiCountKey()));
                usageInfo.setAlarms(getLongValueFromTsEntries(entries, ApiUsageRecordKey.CREATED_ALARMS_COUNT.getApiCountKey()));
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to fetch api usage values from timeseries!");
            }
        }
        return usageInfo;
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `entries`：数据列表。
     * - `key`：键。
     * 返回：数值结果。
     */
    private long getLongValueFromTsEntries(List<TsKvEntry> entries, String key) {
        Optional<TsKvEntry> entryOpt = entries.stream().filter(e -> e.getKey().equals(key)).findFirst();
        return entryOpt.map(entry -> entry.getLongValue().orElse(0L)).orElse(0L);
    }
}
