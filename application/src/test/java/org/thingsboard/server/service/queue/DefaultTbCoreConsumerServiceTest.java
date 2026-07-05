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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbCoreConsumerServiceTest` 是ThingsBoard Application 测试模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@ExtendWith(MockitoExtension.class)
public class DefaultTbCoreConsumerServiceTest {

    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Mock
    private DeviceStateService stateServiceMock;
    /**
     * `statsMock` 字段，保存当前对象的对应属性。
     */
    @Mock
    private TbCoreConsumerStats statsMock;

    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    @Mock
    private TbCallback tbCallbackMock;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private final long time = System.currentTimeMillis();

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    private ListeningExecutorService executor;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    private DefaultTbCoreConsumerService defaultTbCoreConsumerServiceMock;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setup() {
        executor = MoreExecutors.newDirectExecutorService();
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stateService", stateServiceMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "deviceActivityEventsExecutor", executor);
    }

    /**
     * 功能：执行 `cleanup` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    public void cleanup() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10L, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 功能：验证 `givenProcessingSuccess_whenForwardingDeviceStateMsgToStateService_thenOnSuccessCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingSuccess_whenForwardingDeviceStateMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var stateMsg = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(stateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(stateMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onQueueMsg(stateMsg, tbCallbackMock);
    }

    /**
     * 功能：验证 `givenStatsEnabled_whenForwardingDeviceStateMsgToStateService_thenStatsAreRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsEnabled_whenForwardingDeviceStateMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var stateMsg = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(stateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(stateMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(stateMsg);
    }

    /**
     * 功能：验证 `givenStatsDisabled_whenForwardingDeviceStateMsgToStateService_thenStatsAreNotRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsDisabled_whenForwardingDeviceStateMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var stateMsg = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(stateMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(stateMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(stateMsg);
    }

    /**
     * 功能：验证 `givenProcessingSuccess_whenForwardingConnectMsgToStateService_thenOnSuccessCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingSuccess_whenForwardingConnectMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceConnect(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    /**
     * 功能：验证 `givenProcessingFailure_whenForwardingConnectMsgToStateService_thenOnFailureCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingFailure_whenForwardingConnectMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceConnect(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    /**
     * 功能：验证 `givenStatsEnabled_whenForwardingConnectMsgToStateService_thenStatsAreRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsEnabled_whenForwardingConnectMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(connectMsg);
    }

    /**
     * 功能：验证 `givenStatsDisabled_whenForwardingConnectMsgToStateService_thenStatsAreNotRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsDisabled_whenForwardingConnectMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var connectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastConnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(connectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(connectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(connectMsg);
    }

    /**
     * 功能：验证 `givenProcessingSuccess_whenForwardingActivityMsgToStateService_thenOnSuccessCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingSuccess_whenForwardingActivityMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceActivity(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    /**
     * 功能：验证 `givenProcessingFailure_whenForwardingActivityMsgToStateService_thenOnFailureCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingFailure_whenForwardingActivityMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceActivity(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();

        var exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(tbCallbackMock).should().onFailure(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to update device activity for device [" + deviceId.getId() + "]!")
                .hasCause(runtimeException);
    }

    /**
     * 功能：验证 `givenStatsEnabled_whenForwardingActivityMsgToStateService_thenStatsAreRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsEnabled_whenForwardingActivityMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(activityMsg);
    }

    /**
     * 功能：验证 `givenStatsDisabled_whenForwardingActivityMsgToStateService_thenStatsAreNotRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsDisabled_whenForwardingActivityMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var activityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(activityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(activityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(activityMsg);
    }

    /**
     * 功能：验证 `givenProcessingSuccess_whenForwardingDisconnectMsgToStateService_thenOnSuccessCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingSuccess_whenForwardingDisconnectMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceDisconnect(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    /**
     * 功能：验证 `givenProcessingFailure_whenForwardingDisconnectMsgToStateService_thenOnFailureCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingFailure_whenForwardingDisconnectMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceDisconnect(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    /**
     * 功能：验证 `givenStatsEnabled_whenForwardingDisconnectMsgToStateService_thenStatsAreRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsEnabled_whenForwardingDisconnectMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(disconnectMsg);
    }

    /**
     * 功能：验证 `givenStatsDisabled_whenForwardingDisconnectMsgToStateService_thenStatsAreNotRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsDisabled_whenForwardingDisconnectMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var disconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastDisconnectTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(disconnectMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(disconnectMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(disconnectMsg);
    }

    /**
     * 功能：验证 `givenProcessingSuccess_whenForwardingInactivityMsgToStateService_thenOnSuccessCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingSuccess_whenForwardingInactivityMsgToStateService_thenOnSuccessCallbackIsCalled() {
        // GIVEN
        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(stateServiceMock).should().onDeviceInactivity(tenantId, deviceId, time);
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    /**
     * 功能：验证 `givenProcessingFailure_whenForwardingInactivityMsgToStateService_thenOnFailureCallbackIsCalled` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenProcessingFailure_whenForwardingInactivityMsgToStateService_thenOnFailureCallbackIsCalled() {
        // GIVEN
        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        var runtimeException = new RuntimeException("Something bad happened!");
        doThrow(runtimeException).when(stateServiceMock).onDeviceInactivity(tenantId, deviceId, time);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(runtimeException);
    }

    /**
     * 功能：验证 `givenStatsEnabled_whenForwardingInactivityMsgToStateService_thenStatsAreRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsEnabled_whenForwardingInactivityMsgToStateService_thenStatsAreRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", true);

        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should().log(inactivityMsg);
    }

    /**
     * 功能：验证 `givenStatsDisabled_whenForwardingInactivityMsgToStateService_thenStatsAreNotRecorded` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStatsDisabled_whenForwardingInactivityMsgToStateService_thenStatsAreNotRecorded() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "stats", statsMock);
        ReflectionTestUtils.setField(defaultTbCoreConsumerServiceMock, "statsEnabled", false);

        var inactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastInactivityTime(time)
                .build();

        doCallRealMethod().when(defaultTbCoreConsumerServiceMock).forwardToStateService(inactivityMsg, tbCallbackMock);

        // WHEN
        defaultTbCoreConsumerServiceMock.forwardToStateService(inactivityMsg, tbCallbackMock);

        // THEN
        then(statsMock).should(never()).log(inactivityMsg);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbCoreConsumerServiceTest` 在 ThingsBoard Application 测试模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
