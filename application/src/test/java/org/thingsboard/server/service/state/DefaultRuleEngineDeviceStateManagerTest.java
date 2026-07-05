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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

/**
 * 中文说明：
 * 1. 类目的：`DefaultRuleEngineDeviceStateManagerTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@ExtendWith(MockitoExtension.class)
public class DefaultRuleEngineDeviceStateManagerTest {

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Mock
    private static DeviceStateService deviceStateServiceMock;
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    @Mock
    private static TbCallback tbCallbackMock;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    private static TbClusterService clusterServiceMock;
    /**
     * `metadataMock` 字段，保存当前对象的对应属性。
     */
    @Mock
    private static TbQueueMsgMetadata metadataMock;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    private TbServiceInfoProvider serviceInfoProviderMock;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Mock
    private PartitionService partitionServiceMock;

    /**
     * 队列，用于接收异步处理完成后的结果。
     */
    @Captor
    private static ArgumentCaptor<TbQueueCallback> queueCallbackCaptor;

    /**
     * 设备状态管理器，负责处理对应任务或消息。
     */
    private static DefaultRuleEngineDeviceStateManager deviceStateManager;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("57ab2e6c-bc4c-11ee-a506-0242ac120002"));
    private static final DeviceId DEVICE_ID = DeviceId.fromString("74a9053e-bc4c-11ee-a506-0242ac120002");
    private static final long EVENT_TS = System.currentTimeMillis();
    private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException("Something bad happened!");
    private static final TopicPartitionInfo MY_TPI = TopicPartitionInfo.builder().myPartition(true).build();
    private static final TopicPartitionInfo EXTERNAL_TPI = TopicPartitionInfo.builder().myPartition(false).build();

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setup() {
        deviceStateManager = new DefaultRuleEngineDeviceStateManager(serviceInfoProviderMock, partitionServiceMock, Optional.of(deviceStateServiceMock), clusterServiceMock);
    }

    /**
     * 功能：验证 `givenRoutedToLocalAndProcessingSuccess_whenOnDeviceAction_thenShouldCallLocalServiceAndSuccessCallback` 描述的测试场景。
     * 参数：
     * - `onDeviceAction`：设备信息或设备标识。
     * - `actionVerification`：`actionVerification` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @DisplayName("Given event should be routed to local service and event processed has succeeded, " +
            "when onDeviceX() is called, then should route event to local service and call onSuccess() callback.")
    @MethodSource
    public void givenRoutedToLocalAndProcessingSuccess_whenOnDeviceAction_thenShouldCallLocalServiceAndSuccessCallback(Runnable onDeviceAction, Runnable actionVerification) {
        // GIVEN
        given(serviceInfoProviderMock.isService(ServiceType.TB_CORE)).willReturn(true);
        given(partitionServiceMock.resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID)).willReturn(MY_TPI);

        onDeviceAction.run();

        // THEN
        actionVerification.run();

        then(clusterServiceMock).shouldHaveNoInteractions();
        then(tbCallbackMock).should().onSuccess();
        then(tbCallbackMock).should(never()).onFailure(any());
    }

    /**
     * 功能：验证 `givenRoutedToLocalAndProcessingSuccess_whenOnDeviceAction_thenShouldCallLocalServiceAndSuccessCallback` 描述的测试场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> givenRoutedToLocalAndProcessingSuccess_whenOnDeviceAction_thenShouldCallLocalServiceAndSuccessCallback() {
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                )
        );
    }

    /**
     * 功能：验证 `givenRoutedToLocalAndProcessingFailure_whenOnDeviceAction_thenShouldCallLocalServiceAndFailureCallback` 描述的测试场景。
     * 参数：
     * - `exceptionThrowSetup`：`exceptionThrowSetup` 参数。
     * - `onDeviceAction`：设备信息或设备标识。
     * - `actionVerification`：`actionVerification` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @DisplayName("Given event should be routed to local service and event processed has failed, " +
            "when onDeviceX() is called, then should route event to local service and call onFailure() callback.")
    @MethodSource
    public void givenRoutedToLocalAndProcessingFailure_whenOnDeviceAction_thenShouldCallLocalServiceAndFailureCallback(
            Runnable exceptionThrowSetup, Runnable onDeviceAction, Runnable actionVerification
    ) {
        // GIVEN
        given(serviceInfoProviderMock.isService(ServiceType.TB_CORE)).willReturn(true);
        given(partitionServiceMock.resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID)).willReturn(MY_TPI);

        exceptionThrowSetup.run();

        // WHEN
        onDeviceAction.run();

        // THEN
        actionVerification.run();

        then(clusterServiceMock).shouldHaveNoInteractions();
        then(tbCallbackMock).should(never()).onSuccess();
        then(tbCallbackMock).should().onFailure(RUNTIME_EXCEPTION);
    }

    /**
     * 功能：验证 `givenRoutedToLocalAndProcessingFailure_whenOnDeviceAction_thenShouldCallLocalServiceAndFailureCallback` 描述的测试场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> givenRoutedToLocalAndProcessingFailure_whenOnDeviceAction_thenShouldCallLocalServiceAndFailureCallback() {
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS)
                ),
                Arguments.of(
                        (Runnable) () -> doThrow(RUNTIME_EXCEPTION).when(deviceStateServiceMock).onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS),
                        (Runnable) () -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> then(deviceStateServiceMock).should().onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS)
                )
        );
    }

    /**
     * 功能：验证 `givenRoutedToExternal_whenOnDeviceAction_thenShouldSendQueueMsgToExternalServiceWithCorrectCallback` 描述的测试场景。
     * 参数：
     * - `onDeviceAction`：设备信息或设备标识。
     * - `actionVerification`：`actionVerification` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @DisplayName("Given event should be routed to external service, " +
            "when onDeviceX() is called, then should send correct queue message to external service with correct callback object.")
    @MethodSource
    public void givenRoutedToExternal_whenOnDeviceAction_thenShouldSendQueueMsgToExternalServiceWithCorrectCallback(Runnable onDeviceAction, Runnable actionVerification) {
        // WHEN
        ReflectionTestUtils.setField(deviceStateManager, "deviceStateService", Optional.empty());
        given(serviceInfoProviderMock.isService(ServiceType.TB_CORE)).willReturn(false);
        given(partitionServiceMock.resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID)).willReturn(EXTERNAL_TPI);

        onDeviceAction.run();

        // THEN
        actionVerification.run();

        TbQueueCallback callback = queueCallbackCaptor.getValue();
        callback.onSuccess(metadataMock);
        then(tbCallbackMock).should().onSuccess();
        callback.onFailure(RUNTIME_EXCEPTION);
        then(tbCallbackMock).should().onFailure(RUNTIME_EXCEPTION);
    }

    /**
     * 功能：验证 `givenRoutedToExternal_whenOnDeviceAction_thenShouldSendQueueMsgToExternalServiceWithCorrectCallback` 描述的测试场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> givenRoutedToExternal_whenOnDeviceAction_thenShouldSendQueueMsgToExternalServiceWithCorrectCallback() {
        return Stream.of(
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceConnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastConnectTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceConnectMsg(deviceConnectMsg)
                                    .build();
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceActivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastActivityTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceActivityMsg(deviceActivityMsg)
                                    .build();
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceDisconnect(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastDisconnectTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceDisconnectMsg(deviceDisconnectMsg)
                                    .build();
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                ),
                Arguments.of(
                        (Runnable) () -> deviceStateManager.onDeviceInactivity(TENANT_ID, DEVICE_ID, EVENT_TS, tbCallbackMock),
                        (Runnable) () -> {
                            var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                                    .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                                    .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                                    .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                                    .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                                    .setLastInactivityTime(EVENT_TS)
                                    .build();
                            var toCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                                    .setDeviceInactivityMsg(deviceInactivityMsg)
                                    .build();
                            then(clusterServiceMock).should().pushMsgToCore(eq(EXTERNAL_TPI), any(UUID.class), eq(toCoreMsg), queueCallbackCaptor.capture());
                        }
                )
        );
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultRuleEngineDeviceStateManagerTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
