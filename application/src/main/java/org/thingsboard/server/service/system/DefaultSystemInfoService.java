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
package org.thingsboard.server.service.system;

import com.google.common.util.concurrent.FutureCallback;
import com.google.protobuf.ProtocolStringList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.SystemInfo;
import org.thingsboard.server.common.data.SystemInfoData;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.SystemUtil.getCpuCount;
import static org.thingsboard.common.util.SystemUtil.getCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getDiscSpaceUsage;
import static org.thingsboard.common.util.SystemUtil.getMemoryUsage;
import static org.thingsboard.common.util.SystemUtil.getTotalDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getTotalMemory;

/**
 * 中文说明：
 * 1. 类目的：`DefaultSystemInfoService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultSystemInfoService extends TbApplicationEventListener<PartitionChangeEvent> implements SystemInfoService {

    public static final FutureCallback<Integer> CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Integer result) {
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to persist system info", t);
        }
    };

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final DiscoveryService discoveryService;
    private final TelemetrySubscriptionService telemetryService;
    /**
     * 状态，用于发起外部调用或协议交互。
     */
    private final TbApiUsageStateClient apiUsageStateClient;
    private final AdminSettingsService adminSettingsService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final OAuth2Service oAuth2Service;
    private final MailService mailService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final SmsService smsService;
    private volatile ScheduledExecutorService scheduler;

    /**
     * 信息对象，表示当前对象的对应属性。
     */
    @Value("${metrics.system_info.persist_frequency:60}")
    private int systemInfoPersistFrequencySeconds;
    /**
     * 信息对象，表示当前对象的对应属性。
     */
    @Value("#{${metrics.system_info.ttl:7} * 86400}")
    private int systemInfoTtlSeconds;

    /**
     * 功能：处理事件。
     * 参数：
     * - `partitionChangeEvent`：分区标识或分区信息。
     * 返回：无。
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            boolean myPartition = partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition();
            synchronized (this) {
                if (myPartition) {
                    if (scheduler == null) {
                        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-system-info-scheduler"));
                        scheduler.scheduleWithFixedDelay(this::saveCurrentSystemInfo, 0, systemInfoPersistFrequencySeconds, TimeUnit.SECONDS);
                    }
                } else {
                    destroy();
                }
            }
        }
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        if (discoveryService.isMonolith()) {
            systemInfo.setMonolith(true);
            systemInfo.setSystemData(Collections.singletonList(createSystemInfoData(serviceInfoProvider.generateNewServiceInfoWithCurrentSystemInfo())));
        } else {
            systemInfo.setSystemData(getSystemData(serviceInfoProvider.getServiceInfo()));
        }

        return systemInfo;
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：无。
     * 返回：无。
     */
    protected void saveCurrentSystemInfo() {
        if (discoveryService.isMonolith()) {
            saveCurrentMonolithSystemInfo();
        } else {
            saveCurrentClusterSystemInfo();
        }
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public FeaturesInfo getFeaturesInfo() {
        FeaturesInfo featuresInfo = new FeaturesInfo();
        featuresInfo.setEmailEnabled(isEmailEnabled());
        featuresInfo.setSmsEnabled(smsService.isConfigured(TenantId.SYS_TENANT_ID));
        featuresInfo.setOauthEnabled(oAuth2Service.findOAuth2Info().isEnabled());
        featuresInfo.setTwoFaEnabled(isTwoFaEnabled());
        featuresInfo.setNotificationEnabled(isSlackEnabled());
        return featuresInfo;
    }

    /**
     * 功能：判断邮箱。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isEmailEnabled() {
        try {
            mailService.testConnection(TenantId.SYS_TENANT_ID);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 功能：判断`Two Fa Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isTwoFaEnabled() {
        AdminSettings twoFaSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "twoFaSettings");
        if (twoFaSettings != null) {
            var providers = twoFaSettings.getJsonValue().get("providers");
            if (providers != null) {
                return providers.size() > 0;
            }
        }
        return false;
    }

    /**
     * 功能：判断`Slack Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isSlackEnabled() {
        AdminSettings notifications = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "notifications");
        if (notifications != null) {
            return notifications.getJsonValue().get("deliveryMethodsConfigs").has("SLACK");
        }
        return false;
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：无。
     * 返回：无。
     */
    private void saveCurrentClusterSystemInfo() {
        long ts = System.currentTimeMillis();
        List<SystemInfoData> clusterSystemData = getSystemData(serviceInfoProvider.getServiceInfo());
        BasicTsKvEntry clusterDataKv = new BasicTsKvEntry(ts, new JsonDataEntry("clusterSystemData", JacksonUtil.toString(clusterSystemData)));
        doSave(Arrays.asList(new BasicTsKvEntry(ts, new BooleanDataEntry("clusterMode", true)), clusterDataKv));
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：无。
     * 返回：无。
     */
    private void saveCurrentMonolithSystemInfo() {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> tsList = new ArrayList<>();
        tsList.add(new BasicTsKvEntry(ts, new BooleanDataEntry("clusterMode", false)));
        getCpuUsage().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("cpuUsage", (long) v))));
        getMemoryUsage().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("memoryUsage", (long) v))));
        getDiscSpaceUsage().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("discUsage", (long) v))));

        getCpuCount().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("cpuCount", (long) v))));
        getTotalMemory().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("totalMemory", v))));
        getTotalDiscSpace().ifPresent(v -> tsList.add(new BasicTsKvEntry(ts, new LongDataEntry("totalDiscSpace", v))));

        doSave(tsList);
    }

    /**
     * 功能：执行 `doSave` 对应的处理。
     * 参数：
     * - `telemetry`：数据列表。
     * 返回：无。
     */
    private void doSave(List<TsKvEntry> telemetry) {
        ApiUsageState apiUsageState = apiUsageStateClient.getApiUsageState(TenantId.SYS_TENANT_ID);
        telemetryService.saveAndNotifyInternal(TenantId.SYS_TENANT_ID, apiUsageState.getId(), telemetry, systemInfoTtlSeconds, CALLBACK);
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `serviceInfo`：服务对象。
     * 返回：匹配的数据集合。
     */
    private List<SystemInfoData> getSystemData(ServiceInfo serviceInfo) {
        List<SystemInfoData> clusterSystemData = new ArrayList<>();
        clusterSystemData.add(createSystemInfoData(serviceInfo));
        this.discoveryService.getOtherServers()
                .stream()
                .map(this::createSystemInfoData)
                .forEach(clusterSystemData::add);
        return clusterSystemData;
    }

    /**
     * 功能：保存或创建数据。
     * 参数：
     * - `serviceInfo`：服务对象。
     * 返回：处理结果。
     */
    private SystemInfoData createSystemInfoData(ServiceInfo serviceInfo) {
        ProtocolStringList serviceTypes = serviceInfo.getServiceTypesList();
        SystemInfoData infoData = new SystemInfoData();
        infoData.setServiceId(serviceInfo.getServiceId());
        infoData.setServiceType(serviceTypes.size() > 1 ? "MONOLITH" : serviceTypes.get(0));

        infoData.setCpuUsage(serviceInfo.getSystemInfo().getCpuUsage());
        infoData.setMemoryUsage(serviceInfo.getSystemInfo().getMemoryUsage());
        infoData.setDiscUsage(serviceInfo.getSystemInfo().getDiskUsage());

        infoData.setCpuCount(serviceInfo.getSystemInfo().getCpuCount());
        infoData.setTotalMemory(serviceInfo.getSystemInfo().getTotalMemory());
        infoData.setTotalDiscSpace(serviceInfo.getSystemInfo().getTotalDiscSpace());

        return infoData;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultSystemInfoService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
