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
package org.thingsboard.server.service.stats;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 中文说明：
 * 1. 类目的：`DevicesStatisticsTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=2",
        "usage.stats.gauge_report_interval=1",
        "usage.stats.devices.report_interval=3",
        "state.defaultStateCheckIntervalInSec=3",
        "state.defaultInactivityTimeoutInSec=10",
})
public class DevicesStatisticsTest extends AbstractControllerTest {

    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Autowired
    private TbApiUsageStateService apiUsageStateService;
    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    @Autowired
    private TimeseriesService timeseriesService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceStateService deviceStateService;

    /**
     * 状态ID，用于定位对应业务对象。
     */
    private ApiUsageStateId apiUsageStateId;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        apiUsageStateId = apiUsageStateService.getApiUsageState(tenantId).getId();
    }

    /**
     * 功能：验证`Devices Activity Stats`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDevicesActivityStats() throws Exception {
        int activeDevicesCount = 5;
        List<Device> activeDevices = new ArrayList<>();
        for (int i = 1; i <= activeDevicesCount; i++) {
            String name = "active_device_" + i;
            Device device = createDevice(name, name);
            activeDevices.add(device);
        }
        int inactiveDevicesCount = 10;
        List<Device> inactiveDevices = new ArrayList<>();
        for (int i = 1; i <= inactiveDevicesCount; i++) {
            String name = "inactive_device_" + i;
            Device device = createDevice(name, name);
            inactiveDevices.add(device);
        }

        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, false)).isZero();
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, true)).isZero();
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, false)).isEqualTo(activeDevicesCount + inactiveDevicesCount);
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, true)).isEqualTo(activeDevicesCount + inactiveDevicesCount);
                });

        for (Device device : activeDevices) {
            deviceStateService.onDeviceActivity(tenantId, device.getId(), System.currentTimeMillis());
        }

        await().atMost(40, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, false)).isEqualTo(activeDevicesCount);
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, true)).isEqualTo(activeDevicesCount);
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, false)).isEqualTo(inactiveDevicesCount);
                });
    }

    /**
     * 功能：获取`Latest Stats`。
     * 参数：
     * - `key`：键。
     * - `hourly`：`hourly` 参数。
     * 返回：数值结果。
     */
    @SneakyThrows
    private Long getLatestStats(ApiUsageRecordKey key, boolean hourly) {
        return timeseriesService.findLatest(tenantId, apiUsageStateId, List.of(key.getApiCountKey() + (hourly ? "Hourly" : "")))
                .get().stream().findFirst().flatMap(KvEntry::getLongValue).orElse(null);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DevicesStatisticsTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
