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
package org.thingsboard.server.service.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.query.EntityQueryRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.usagestats.DefaultTbApiUsageReportClient;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.INACTIVITY_ALARM_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.INACTIVITY_TIMEOUT;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_ACTIVITY_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

/**
 * 中文说明：
 * 1. 类目的：`DefaultDeviceStateServiceTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@ExtendWith(MockitoExtension.class)
public class DefaultDeviceStateServiceTest {

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Mock
    DeviceService deviceService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    AttributesService attributesService;
    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Mock
    TimeseriesService tsService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    TbClusterService clusterService;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Mock
    PartitionService partitionService;
    /**
     * 设备，表示当前对象所处状态。
     */
    @Mock
    DeviceStateData deviceStateDataMock;
    /**
     * 实体，用于读取或保存对应领域对象。
     */
    @Mock
    EntityQueryRepository entityQueryRepository;
    /**
     * 订阅，提供当前类调用的业务操作。
     */
    @Mock
    TelemetrySubscriptionService telemetrySubscriptionService;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Mock
    NotificationRuleProcessor notificationRuleProcessor;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Mock
    DefaultTbApiUsageReportClient defaultTbApiUsageReportClient;

    TenantId tenantId = new TenantId(UUID.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112"));
    DeviceId deviceId = DeviceId.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112");
    /**
     * `tpi` 字段，保存当前对象的对应属性。
     */
    TopicPartitionInfo tpi;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    DefaultDeviceStateService service;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setUp() {
        service = spy(new DefaultDeviceStateService(deviceService, attributesService, tsService, clusterService, partitionService, entityQueryRepository, null, defaultTbApiUsageReportClient, notificationRuleProcessor));
        ReflectionTestUtils.setField(service, "tsSubService", telemetrySubscriptionService);
        ReflectionTestUtils.setField(service, "defaultStateCheckIntervalInSec", 60);
        ReflectionTestUtils.setField(service, "defaultActivityStatsIntervalInSec", 60);
        ReflectionTestUtils.setField(service, "initFetchPackSize", 10);

        tpi = TopicPartitionInfo.builder().myPartition(true).build();
    }

    /**
     * 功能：验证 `givenDeviceBelongsToExternalPartition_whenOnDeviceConnect_thenCleansStateAndDoesNotReportConnect` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceBelongsToExternalPartition_whenOnDeviceConnect_thenCleansStateAndDoesNotReportConnect() {
        // GIVEN
        doReturn(true).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, System.currentTimeMillis());

        // THEN
        then(service).should().cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(service).should(never()).checkAndUpdateState(eq(deviceId), any());
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenNegativeLastConnectTime_whenOnDeviceConnect_thenSkipsThisEvent` 描述的测试场景。
     * 参数：
     * - `negativeLastConnectTime`：`negativeLastConnectTime` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -100, -1})
    public void givenNegativeLastConnectTime_whenOnDeviceConnect_thenSkipsThisEvent(long negativeLastConnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, negativeLastConnectTime);

        // THEN
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(service).should(never()).checkAndUpdateState(eq(deviceId), any());
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenOutdatedLastConnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent` 描述的测试场景。
     * 参数：
     * - `outdatedLastConnectTime`：`outdatedLastConnectTime` 参数。
     * - `currentLastConnectTime`：`currentLastConnectTime` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    public void givenOutdatedLastConnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent(long outdatedLastConnectTime, long currentLastConnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastConnectTime(currentLastConnectTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, outdatedLastConnectTime);

        // THEN
        then(service).should(never()).checkAndUpdateState(eq(deviceId), any());
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenDeviceBelongsToMyPartition_whenOnDeviceConnect_thenReportsConnect` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceBelongsToMyPartition_whenOnDeviceConnect_thenReportsConnect() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        service.deviceStates.put(deviceId, deviceStateData);
        long lastConnectTime = System.currentTimeMillis();

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, lastConnectTime);

        // THEN
        then(telemetrySubscriptionService).should().saveAttrAndNotify(
                eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(DataConstants.SERVER_SCOPE), eq(LAST_CONNECT_TIME), eq(lastConnectTime), any()
        );

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.CONNECT_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);
    }

    /**
     * 功能：验证 `givenDeviceBelongsToExternalPartition_whenOnDeviceDisconnect_thenCleansStateAndDoesNotReportDisconnect` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceBelongsToExternalPartition_whenOnDeviceDisconnect_thenCleansStateAndDoesNotReportDisconnect() {
        // GIVEN
        doReturn(true).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, System.currentTimeMillis());

        // THEN
        then(service).should().cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenNegativeLastDisconnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent` 描述的测试场景。
     * 参数：
     * - `negativeLastDisconnectTime`：`negativeLastDisconnectTime` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -100, -1})
    public void givenNegativeLastDisconnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent(long negativeLastDisconnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, negativeLastDisconnectTime);

        // THEN
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenOutdatedLastDisconnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent` 描述的测试场景。
     * 参数：
     * - `outdatedLastDisconnectTime`：`outdatedLastDisconnectTime` 参数。
     * - `currentLastDisconnectTime`：`currentLastDisconnectTime` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    public void givenOutdatedLastDisconnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent(long outdatedLastDisconnectTime, long currentLastDisconnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastDisconnectTime(currentLastDisconnectTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, outdatedLastDisconnectTime);

        // THEN
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenDeviceBelongsToMyPartition_whenOnDeviceDisconnect_thenReportsDisconnect` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceBelongsToMyPartition_whenOnDeviceDisconnect_thenReportsDisconnect() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        service.deviceStates.put(deviceId, deviceStateData);
        long lastDisconnectTime = System.currentTimeMillis();

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, lastDisconnectTime);

        // THEN
        then(telemetrySubscriptionService).should().saveAttrAndNotify(
                eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(DataConstants.SERVER_SCOPE),
                eq(LAST_DISCONNECT_TIME), eq(lastDisconnectTime), any()
        );

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.DISCONNECT_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);
    }

    /**
     * 功能：验证 `givenDeviceBelongsToExternalPartition_whenOnDeviceInactivity_thenCleansStateAndDoesNotReportInactivity` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceBelongsToExternalPartition_whenOnDeviceInactivity_thenCleansStateAndDoesNotReportInactivity() {
        // GIVEN
        doReturn(true).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, System.currentTimeMillis());

        // THEN
        then(service).should().cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);
        then(service).should(never()).fetchDeviceStateDataUsingSeparateRequests(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenNegativeLastInactivityTime_whenOnDeviceInactivity_thenSkipsThisEvent` 描述的测试场景。
     * 参数：
     * - `negativeLastInactivityTime`：`negativeLastInactivityTime` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -100, -1})
    public void givenNegativeLastInactivityTime_whenOnDeviceInactivity_thenSkipsThisEvent(long negativeLastInactivityTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, negativeLastInactivityTime);

        // THEN
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenReceivedInactivityTimeIsLessThanOrEqualToCurrentInactivityTime_whenOnDeviceInactivity_thenSkipsThisEvent` 描述的测试场景。
     * 参数：
     * - `outdatedLastInactivityTime`：`outdatedLastInactivityTime` 参数。
     * - `currentLastInactivityTime`：`currentLastInactivityTime` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    public void givenReceivedInactivityTimeIsLessThanOrEqualToCurrentInactivityTime_whenOnDeviceInactivity_thenSkipsThisEvent(
            long outdatedLastInactivityTime, long currentLastInactivityTime
    ) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastInactivityAlarmTime(currentLastInactivityTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, outdatedLastInactivityTime);

        // THEN
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：验证 `givenReceivedInactivityTimeIsLessThanOrEqualToCurrentActivityTime_whenOnDeviceInactivity_thenSkipsThisEvent` 描述的测试场景。
     * 参数：
     * - `outdatedLastInactivityTime`：`outdatedLastInactivityTime` 参数。
     * - `currentLastActivityTime`：`currentLastActivityTime` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    public void givenReceivedInactivityTimeIsLessThanOrEqualToCurrentActivityTime_whenOnDeviceInactivity_thenSkipsThisEvent(
            long outdatedLastInactivityTime, long currentLastActivityTime
    ) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastActivityTime(currentLastActivityTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, outdatedLastInactivityTime);

        // THEN
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    /**
     * 功能：执行 `provideOutdatedTimestamps` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> provideOutdatedTimestamps() {
        return Stream.of(
                Arguments.of(0, 0),
                Arguments.of(0, 100),
                Arguments.of(50, 100),
                Arguments.of(99, 100),
                Arguments.of(100, 100)
        );
    }

    /**
     * 功能：验证 `givenDeviceBelongsToMyPartition_whenOnDeviceInactivity_thenReportsInactivity` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceBelongsToMyPartition_whenOnDeviceInactivity_thenReportsInactivity() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        service.deviceStates.put(deviceId, deviceStateData);
        long lastInactivityTime = System.currentTimeMillis();

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, lastInactivityTime);

        // THEN
        then(telemetrySubscriptionService).should().saveAttrAndNotify(
                eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(DataConstants.SERVER_SCOPE),
                eq(INACTIVITY_ALARM_TIME), eq(lastInactivityTime), any()
        );
        then(telemetrySubscriptionService).should().saveAttrAndNotify(
                eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(DataConstants.SERVER_SCOPE),
                eq(ACTIVITY_STATE), eq(false), any()
        );

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should()
                .pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.INACTIVITY_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

        var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
        then(notificationRuleProcessor).should().process(notificationCaptor.capture());
        var actualNotification = notificationCaptor.getValue();
        assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
        assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
        assertThat(actualNotification.isActive()).isFalse();
    }

    /**
     * 功能：验证 `givenInactivityTimeoutReached_whenUpdateInactivityStateIfExpired_thenReportsInactivity` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInactivityTimeoutReached_whenUpdateInactivityStateIfExpired_thenReportsInactivity() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        given(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).willReturn(tpi);

        // WHEN
        service.updateInactivityStateIfExpired(System.currentTimeMillis(), deviceId, deviceStateData);

        // THEN
        then(telemetrySubscriptionService).should().saveAttrAndNotify(
                eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(DataConstants.SERVER_SCOPE),
                eq(INACTIVITY_ALARM_TIME), anyLong(), any()
        );
        then(telemetrySubscriptionService).should().saveAttrAndNotify(
                eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(DataConstants.SERVER_SCOPE),
                eq(ACTIVITY_STATE), eq(false), any()
        );

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should()
                .pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.INACTIVITY_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

        var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
        then(notificationRuleProcessor).should().process(notificationCaptor.capture());
        var actualNotification = notificationCaptor.getValue();
        assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
        assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
        assertThat(actualNotification.isActive()).isFalse();
    }

    /**
     * 功能：验证 `givenDeviceIdFromDeviceStatesMap_whenGetOrFetchDeviceStateData_thenNoStackOverflow` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceIdFromDeviceStatesMap_whenGetOrFetchDeviceStateData_thenNoStackOverflow() {
        service.deviceStates.put(deviceId, deviceStateDataMock);
        DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData).isEqualTo(deviceStateDataMock);
        verify(service, never()).fetchDeviceStateDataUsingSeparateRequests(deviceId);
    }

    /**
     * 功能：验证 `givenDeviceIdWithoutDeviceStateInMap_whenGetOrFetchDeviceStateData_thenFetchDeviceStateData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDeviceIdWithoutDeviceStateInMap_whenGetOrFetchDeviceStateData_thenFetchDeviceStateData() {
        service.deviceStates.clear();
        willReturn(deviceStateDataMock).given(service).fetchDeviceStateDataUsingSeparateRequests(deviceId);
        DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData).isEqualTo(deviceStateDataMock);
        verify(service).fetchDeviceStateDataUsingSeparateRequests(deviceId);
    }

    /**
     * 功能：验证 `givenPersistToTelemetryAndDefaultInactivityTimeoutFetched_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenPersistToTelemetryAndDefaultInactivityTimeoutFetched_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute() {
        var defaultInactivityTimeoutInSec = 60L;
        var latest =
                Map.of(
                        EntityKeyType.TIME_SERIES, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(defaultInactivityTimeoutInSec * 1000))),
                        EntityKeyType.SERVER_ATTRIBUTE, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(5000L)))
                );

        process(latest, defaultInactivityTimeoutInSec);
    }

    /**
     * 功能：验证 `givenPersistToTelemetryAndNoInactivityTimeoutFetchedFromTimeSeries_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenPersistToTelemetryAndNoInactivityTimeoutFetchedFromTimeSeries_whenTransformingToDeviceStateData_thenTryGetInactivityFromAttribute() {
        var defaultInactivityTimeoutInSec = 60L;
        var latest =
                Map.of(
                        EntityKeyType.SERVER_ATTRIBUTE, Map.of(INACTIVITY_TIMEOUT, new TsValue(0, Long.toString(5000L)))
                );

        process(latest, defaultInactivityTimeoutInSec);
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `latest`：键值映射。
     * - `defaultInactivityTimeoutInSec`：`defaultInactivityTimeoutInSec` 参数。
     * 返回：无。
     */
    private void process(Map<EntityKeyType, Map<String, TsValue>> latest, long defaultInactivityTimeoutInSec) {
        service.setDefaultInactivityTimeoutInSec(defaultInactivityTimeoutInSec);
        service.setDefaultInactivityTimeoutMs(defaultInactivityTimeoutInSec * 1000);
        service.setPersistToTelemetry(true);

        var deviceUuid = UUID.randomUUID();
        var deviceId = new DeviceId(deviceUuid);

        DeviceStateData deviceStateData = service.toDeviceStateData(new EntityData(deviceId, latest, Map.of()), new DeviceIdInfo(TenantId.SYS_TENANT_ID.getId(), UUID.randomUUID(), deviceUuid));

        assertThat(deviceStateData.getState().getInactivityTimeout()).isEqualTo(5000L);
    }

    /**
     * 功能：初始化或启动状态。
     * 参数：
     * - `timeout`：`timeout` 参数。
     * 返回：无。
     */
    private void initStateService(long timeout) throws InterruptedException {
        service.stop();
        reset(service, telemetrySubscriptionService);
        service.setDefaultInactivityTimeoutMs(timeout);
        service.init();
        when(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(tpi);
        when(entityQueryRepository.findEntityDataByQueryInternal(any())).thenReturn(new PageData<>());
        var deviceIdInfo = new DeviceIdInfo(tenantId.getId(), null, deviceId.getId());
        when(deviceService.findDeviceIdInfos(any()))
                .thenReturn(new PageData<>(List.of(deviceIdInfo), 0, 1, false));
        PartitionChangeEvent event = new PartitionChangeEvent(this, ServiceType.TB_CORE, Map.of(
                new QueueKey(ServiceType.TB_CORE), Collections.singleton(tpi)
        ));
        service.onApplicationEvent(event);
        Thread.sleep(100);
    }

    /**
     * 功能：执行 `increaseInactivityForInactiveDeviceTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void increaseInactivityForInactiveDeviceTest() throws Exception {
        final long defaultTimeout = 1;
        initStateService(defaultTimeout);
        DeviceState deviceState = DeviceState.builder().build();
        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        activityVerify(true);
        Thread.sleep(defaultTimeout);
        service.checkStates();
        activityVerify(false);

        reset(telemetrySubscriptionService);

        long increase = 100;
        long newTimeout = System.currentTimeMillis() - deviceState.getLastActivityTime() + increase;

        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, newTimeout);
        activityVerify(true);
        Thread.sleep(increase);
        service.checkStates();
        activityVerify(false);

        reset(telemetrySubscriptionService);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        activityVerify(true);
        Thread.sleep(newTimeout + 5);
        service.checkStates();
        activityVerify(false);
    }

    /**
     * 功能：执行 `increaseInactivityForActiveDeviceTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void increaseInactivityForActiveDeviceTest() throws Exception {
        final long defaultTimeout = 1000;
        initStateService(defaultTimeout);
        DeviceState deviceState = DeviceState.builder().build();
        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        activityVerify(true);

        reset(telemetrySubscriptionService);

        long increase = 100;
        long newTimeout = System.currentTimeMillis() - deviceState.getLastActivityTime() + increase;

        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, newTimeout);
        verify(telemetrySubscriptionService, never()).saveAttrAndNotify(any(), eq(deviceId), any(), eq(ACTIVITY_STATE), any(), any());
        Thread.sleep(defaultTimeout + increase);
        service.checkStates();
        activityVerify(false);

        reset(telemetrySubscriptionService);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        activityVerify(true);
        Thread.sleep(newTimeout);
        service.checkStates();
        activityVerify(false);
    }

    /**
     * 功能：执行 `increaseSmallInactivityForInactiveDeviceTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void increaseSmallInactivityForInactiveDeviceTest() throws Exception {
        final long defaultTimeout = 1;
        initStateService(defaultTimeout);
        DeviceState deviceState = DeviceState.builder().build();
        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        activityVerify(true);
        Thread.sleep(defaultTimeout);
        service.checkStates();
        activityVerify(false);

        reset(telemetrySubscriptionService);

        long newTimeout = 1;
        Thread.sleep(newTimeout);
        verify(telemetrySubscriptionService, never()).saveAttrAndNotify(any(), eq(deviceId), any(), eq(ACTIVITY_STATE), any(), any());
    }

    /**
     * 功能：执行 `decreaseInactivityForActiveDeviceTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void decreaseInactivityForActiveDeviceTest() throws Exception {
        final long defaultTimeout = 1000;
        initStateService(defaultTimeout);
        DeviceState deviceState = DeviceState.builder().build();
        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        activityVerify(true);

        verify(telemetrySubscriptionService, never()).saveAttrAndNotify(any(), eq(deviceId), any(), eq(ACTIVITY_STATE), any(), any());

        long newTimeout = 1;
        Thread.sleep(newTimeout);

        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, newTimeout);
        activityVerify(false);
        reset(telemetrySubscriptionService);

        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, defaultTimeout);
        activityVerify(true);
        Thread.sleep(defaultTimeout);
        service.checkStates();
        activityVerify(false);
    }

    /**
     * 功能：执行 `decreaseInactivityForInactiveDeviceTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void decreaseInactivityForInactiveDeviceTest() throws Exception {
        final long defaultTimeout = 1000;
        initStateService(defaultTimeout);
        DeviceState deviceState = DeviceState.builder().build();
        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        service.onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
        activityVerify(true);
        Thread.sleep(defaultTimeout);
        service.checkStates();
        activityVerify(false);
        reset(telemetrySubscriptionService);

        long newTimeout = 1;

        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, newTimeout);
        verify(telemetrySubscriptionService, never()).saveAttrAndNotify(any(), eq(deviceId), any(), eq(ACTIVITY_STATE), any(), any());
    }

    /**
     * 功能：执行 `activityVerify` 对应的处理。
     * 参数：
     * - `isActive`：`isActive` 参数。
     * 返回：无。
     */
    private void activityVerify(boolean isActive) {
        verify(telemetrySubscriptionService).saveAttrAndNotify(any(), eq(deviceId), any(), eq(ACTIVITY_STATE), eq(isActive), any());
    }

    /**
     * 功能：验证 `givenStateDataIsNull_whenUpdateActivityState_thenShouldCleanupDevice` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStateDataIsNull_whenUpdateActivityState_thenShouldCleanupDevice() {
        // GIVEN
        service.deviceStates.put(deviceId, deviceStateDataMock);

        // WHEN
        service.updateActivityState(deviceId, null, System.currentTimeMillis());

        // THEN
        assertThat(service.deviceStates.get(deviceId)).isNull();
        assertThat(service.deviceStates.size()).isEqualTo(0);
        assertThat(service.deviceStates.isEmpty()).isTrue();
    }


    /**
     * 功能：验证 `givenTestParameters_whenUpdateActivityState_thenShouldBeInTheExpectedStateAndPerformExpectedActions` 描述的测试场景。
     * 参数：
     * - `activityState`：`activityState` 参数。
     * - `previousActivityTime`：`previousActivityTime` 参数。
     * - `lastReportedActivity`：`lastReportedActivity` 参数。
     * - `inactivityAlarmTime`：`inactivityAlarmTime` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideParametersForUpdateActivityState")
    public void givenTestParameters_whenUpdateActivityState_thenShouldBeInTheExpectedStateAndPerformExpectedActions(
            boolean activityState, long previousActivityTime, long lastReportedActivity, long inactivityAlarmTime,
            long expectedInactivityAlarmTime, boolean shouldSetInactivityAlarmTimeToZero,
            boolean shouldUpdateActivityStateToActive
    ) {
        // GIVEN
        DeviceState deviceState = DeviceState.builder()
                .active(activityState)
                .lastActivityTime(previousActivityTime)
                .lastInactivityAlarmTime(inactivityAlarmTime)
                .inactivityTimeout(10000)
                .build();

        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        // WHEN
        service.updateActivityState(deviceId, deviceStateData, lastReportedActivity);

        // THEN
        assertThat(deviceState.isActive()).isEqualTo(true);
        assertThat(deviceState.getLastActivityTime()).isEqualTo(lastReportedActivity);
        then(telemetrySubscriptionService).should().saveAttrAndNotify(
                any(), eq(deviceId), any(), eq(LAST_ACTIVITY_TIME), eq(lastReportedActivity), any()
        );

        assertThat(deviceState.getLastInactivityAlarmTime()).isEqualTo(expectedInactivityAlarmTime);
        if (shouldSetInactivityAlarmTimeToZero) {
            then(telemetrySubscriptionService).should().saveAttrAndNotify(
                    any(), eq(deviceId), any(), eq(INACTIVITY_ALARM_TIME), eq(0L), any()
            );
        }

        if (shouldUpdateActivityStateToActive) {
            then(telemetrySubscriptionService).should().saveAttrAndNotify(
                    eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(SERVER_SCOPE), eq(ACTIVITY_STATE), eq(true), any()
            );

            var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
            var actualMsg = msgCaptor.getValue();
            assertThat(actualMsg.getType()).isEqualTo(TbMsgType.ACTIVITY_EVENT.name());
            assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

            var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
            then(notificationRuleProcessor).should().process(notificationCaptor.capture());
            var actualNotification = notificationCaptor.getValue();
            assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
            assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
            assertThat(actualNotification.isActive()).isTrue();
        }
    }

    /**
     * 功能：执行 `provideParametersForUpdateActivityState` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> provideParametersForUpdateActivityState() {
        return Stream.of(
                Arguments.of(true,  100, 120, 80,  80,  false, false),

                Arguments.of(true,  100, 120, 100, 100, false, false),

                Arguments.of(false, 100, 120, 110, 110, false, true),


                Arguments.of(true,  100, 100, 80,  80,  false, false),

                Arguments.of(true,  100, 100, 100, 100, false, false),

                Arguments.of(false, 100, 100, 110, 0,   true,  true),


                Arguments.of(false, 100, 110, 110, 0,   true,  true),

                Arguments.of(false, 100, 110, 120, 0,   true,  true),


                Arguments.of(true,  0,   0,   0,   0,   false, false),

                Arguments.of(false, 0,   0,   0,   0,   true,  true)
        );
    }

    /**
     * 功能：验证 `givenTestParameters_whenOnDeviceInactivityTimeout_thenShouldBeInTheExpectedStateAndPerformExpectedActions` 描述的测试场景。
     * 参数：
     * - `activityState`：`activityState` 参数。
     * - `newInactivityTimeout`：`newInactivityTimeout` 参数。
     * - `timeIncrement`：`timeIncrement` 参数。
     * - `expectedActivityState`：`expectedActivityState` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideParametersForDecreaseInactivityTimeout")
    public void givenTestParameters_whenOnDeviceInactivityTimeout_thenShouldBeInTheExpectedStateAndPerformExpectedActions(
            boolean activityState, long newInactivityTimeout, long timeIncrement, boolean expectedActivityState
    ) throws Exception {
        // GIVEN
        long defaultInactivityTimeout = 10000;
        initStateService(defaultInactivityTimeout);

        var currentTime = new AtomicLong(System.currentTimeMillis());

        DeviceState deviceState = DeviceState.builder()
                .active(activityState)
                .lastActivityTime(currentTime.get())
                .inactivityTimeout(defaultInactivityTimeout)
                .build();

        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        given(service.getCurrentTimeMillis()).willReturn(currentTime.addAndGet(timeIncrement));

        // WHEN
        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, newInactivityTimeout);

        // THEN
        assertThat(deviceState.getInactivityTimeout()).isEqualTo(newInactivityTimeout);
        assertThat(deviceState.isActive()).isEqualTo(expectedActivityState);
        if (activityState && !expectedActivityState) {
            then(telemetrySubscriptionService).should().saveAttrAndNotify(
                    any(), eq(deviceId), any(), eq(ACTIVITY_STATE), eq(false), any()
            );
        }
    }

    /**
     * 功能：执行 `provideParametersForDecreaseInactivityTimeout` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> provideParametersForDecreaseInactivityTimeout() {
        return Stream.of(
                Arguments.of(true, 1, 0, true),

                Arguments.of(true, 1, 1, false)
        );
    }

    /**
     * 功能：验证 `givenStateDataIsNull_whenUpdateInactivityTimeoutIfExpired_thenShouldCleanupDevice` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStateDataIsNull_whenUpdateInactivityTimeoutIfExpired_thenShouldCleanupDevice() {
        // GIVEN
        service.deviceStates.put(deviceId, deviceStateDataMock);

        // WHEN
        service.updateInactivityStateIfExpired(System.currentTimeMillis(), deviceId, null);

        // THEN
        assertThat(service.deviceStates.get(deviceId)).isNull();
        assertThat(service.deviceStates.size()).isEqualTo(0);
        assertThat(service.deviceStates.isEmpty()).isTrue();
    }

    /**
     * 功能：验证 `givenNotMyPartition_whenUpdateInactivityTimeoutIfExpired_thenShouldCleanupDevice` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNotMyPartition_whenUpdateInactivityTimeoutIfExpired_thenShouldCleanupDevice() {
        // GIVEN
        long currentTime = System.currentTimeMillis();

        DeviceState deviceState = DeviceState.builder()
                .active(true)
                .lastConnectTime(currentTime - 8000)
                .lastActivityTime(currentTime - 4000)
                .lastDisconnectTime(0)
                .lastInactivityAlarmTime(0)
                .inactivityTimeout(3000)
                .build();

        DeviceStateData stateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .deviceCreationTime(currentTime - 10000)
                .state(deviceState)
                .build();

        service.deviceStates.put(deviceId, stateData);

        var notMyTpi = TopicPartitionInfo.builder().myPartition(false).build();
        given(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).willReturn(notMyTpi);

        // WHEN
        service.updateInactivityStateIfExpired(System.currentTimeMillis(), deviceId, stateData);

        // THEN
        assertThat(service.deviceStates.get(deviceId)).isNull();
        assertThat(service.deviceStates.size()).isEqualTo(0);
        assertThat(service.deviceStates.isEmpty()).isTrue();
    }

    /**
     * 功能：验证 `givenTestParameters_whenUpdateInactivityStateIfExpired_thenShouldBeInTheExpectedStateAndPerformExpectedActions` 描述的测试场景。
     * 参数：
     * - `activityState`：`activityState` 参数。
     * - `ts`：时间戳。
     * - `lastActivityTime`：`lastActivityTime` 参数。
     * - `lastInactivityAlarmTime`：`lastInactivityAlarmTime` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideParametersForUpdateInactivityStateIfExpired")
    public void givenTestParameters_whenUpdateInactivityStateIfExpired_thenShouldBeInTheExpectedStateAndPerformExpectedActions(
            boolean activityState, long ts, long lastActivityTime, long lastInactivityAlarmTime, long inactivityTimeout, long deviceCreationTime,
            boolean expectedActivityState, long expectedLastInactivityAlarmTime, boolean shouldUpdateActivityStateToInactive
    ) {
        // GIVEN
        var state = DeviceState.builder()
                .active(activityState)
                .lastActivityTime(lastActivityTime)
                .lastInactivityAlarmTime(lastInactivityAlarmTime)
                .inactivityTimeout(inactivityTimeout)
                .build();

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .deviceCreationTime(deviceCreationTime)
                .metaData(new TbMsgMetaData())
                .state(state)
                .build();

        if (shouldUpdateActivityStateToInactive) {
            given(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).willReturn(tpi);
        }

        // WHEN
        service.updateInactivityStateIfExpired(ts, deviceId, deviceStateData);

        // THEN
        assertThat(state.isActive()).isEqualTo(expectedActivityState);
        assertThat(state.getLastInactivityAlarmTime()).isEqualTo(expectedLastInactivityAlarmTime);

        if (shouldUpdateActivityStateToInactive) {
            then(telemetrySubscriptionService).should().saveAttrAndNotify(
                    eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(SERVER_SCOPE), eq(ACTIVITY_STATE), eq(false), any()
            );

            var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
            var actualMsg = msgCaptor.getValue();
            assertThat(actualMsg.getType()).isEqualTo(TbMsgType.INACTIVITY_EVENT.name());
            assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

            var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
            then(notificationRuleProcessor).should().process(notificationCaptor.capture());
            var actualNotification = notificationCaptor.getValue();
            assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
            assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
            assertThat(actualNotification.isActive()).isFalse();

            then(telemetrySubscriptionService).should().saveAttrAndNotify(
                    eq(TenantId.SYS_TENANT_ID), eq(deviceId), eq(SERVER_SCOPE),
                    eq(INACTIVITY_ALARM_TIME), eq(expectedLastInactivityAlarmTime), any()
            );
        }
    }

    /**
     * 功能：执行 `provideParametersForUpdateInactivityStateIfExpired` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> provideParametersForUpdateInactivityStateIfExpired() {
        return Stream.of(
                Arguments.of(false, 100, 70,  90,  70,  60,  false, 90,  false),

                Arguments.of(false, 100, 40,  50,  70,  10,  false, 50,  false),

                Arguments.of(false, 100, 25,  60,  75,  25,  false, 60,  false),

                Arguments.of(false, 100, 60,  70,  10,  50,  false, 70,  false),

                Arguments.of(false, 100, 10,  15,  90,  10,  false, 15,  false),

                Arguments.of(false, 100, 0,   40,  75,  0,   false, 40,  false),

                Arguments.of(true,  100, 90,  80,  80,  50,  true,  80,  false),

                Arguments.of(true,  100, 95,  90,  10,  50,  true,  90,  false),

                Arguments.of(true,  100, 10,  10,  90,  10,  false, 100, true),

                Arguments.of(true,  100, 10,  10,  90,  11,  true,  10,  false),

                Arguments.of(true,  100, 15,  10,  85,  5,   false, 100, true),

                Arguments.of(true,  100, 15,  10,  75,  5,   false, 100, true),

                Arguments.of(true,  100, 95,  90,  5,   50,  false, 100, true),

                Arguments.of(true,  100, 0,   0,   101, 0,   true,  0,   false),

                Arguments.of(true,  100, 0,   0,   100, 0,   false, 100, true),

                Arguments.of(true,  100, 0,   0,   99,  0,   false, 100, true),

                Arguments.of(true,  100, 0,   0,   120, 10,  true,  0,   false),

                Arguments.of(true,  100, 50,  0,   100, 0,   true,  0,   false),

                Arguments.of(true,  100, 10,  0,   91,  0,   true,  0,   false),

                Arguments.of(true,  100, 90,  0,   10,  0,   false, 100, true),

                Arguments.of(true,  100, 100, 100, 1,   0,   true,  100, false),

                Arguments.of(true,  100, 100, 100, 100, 100, true,  100, false),

                Arguments.of(false, 100, 59,  60,  30,  10,  false, 60,  false),

                Arguments.of(true,  100, 60,  60,  30,  10,  false, 100, true),

                Arguments.of(true,  100, 61,  60,  30,  10,  false, 100, true),

                Arguments.of(true,  0,   0,   0,   1,   0,   true,  0,   false),

                Arguments.of(true,  0,   0,   0,   0,   0,   false, 0,   true),

                Arguments.of(true,  100, 90,  80,  20,  70,  true,  80,  false),

                Arguments.of(true,  100, 80,  90,  30,  70,  true,  90,  false)
        );
    }

    /**
     * 功能：验证 `givenConcurrentAccess_whenGetOrFetchDeviceStateData_thenFetchDeviceStateDataInvokedOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenConcurrentAccess_whenGetOrFetchDeviceStateData_thenFetchDeviceStateDataInvokedOnce() {
        doAnswer(invocation -> {
            Thread.sleep(100);
            return deviceStateDataMock;
        }).when(service).fetchDeviceStateDataUsingSeparateRequests(deviceId);

        int numberOfThreads = 10;
        var allThreadsReadyLatch = new CountDownLatch(numberOfThreads);

        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(numberOfThreads);
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    allThreadsReadyLatch.countDown();
                    try {
                        allThreadsReadyLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    service.getOrFetchDeviceStateData(deviceId);
                });
            }

            executor.shutdown();
            await().atMost(10, TimeUnit.SECONDS).until(executor::isTerminated);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }

        then(service).should().fetchDeviceStateDataUsingSeparateRequests(deviceId);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultDeviceStateServiceTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
