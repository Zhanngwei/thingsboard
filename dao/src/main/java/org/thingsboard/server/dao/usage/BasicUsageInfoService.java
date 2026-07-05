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
 * 1. 类目的：`BasicUsageInfoService` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
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

/*
 * 本类总结：
 * 1. 核心职责：`BasicUsageInfoService` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
