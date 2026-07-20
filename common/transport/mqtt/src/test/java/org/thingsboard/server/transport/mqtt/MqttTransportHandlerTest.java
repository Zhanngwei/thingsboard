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
package org.thingsboard.server.transport.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `MqttTransportHandlerTest` 是 ThingsBoard Common Transport 中验证 `MqttTransportHandler` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class MqttTransportHandlerTest {

    /**
     * 队列常量，用于统一引用固定值。
     */
    public static final int MSG_QUEUE_LIMIT = 10;
    public static final InetSocketAddress IP_ADDR = new InetSocketAddress("127.0.0.1", 9876);
    /**
     * 超时时间常量，用于统一引用固定值。
     */
    public static final int TIMEOUT = 30;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    MqttTransportContext context;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Mock
    SslHandler sslHandler;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    ChannelHandlerContext ctx;

    AtomicInteger packedId = new AtomicInteger();
    /**
     * 执行器，负责处理对应任务或消息。
     */
    ExecutorService executor;
    MqttTransportHandler handler;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Spy
    TransportService transportService;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws Exception {

        willReturn(MSG_QUEUE_LIMIT).given(context).getMessageQueueSizePerDeviceLimit();
        willReturn(transportService).given(context).getTransportService();

        handler = spy(new MqttTransportHandler(context, sslHandler));
        willReturn(IP_ADDR).given(handler).getAddress(any());
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    MqttConnectMessage getMqttConnectMessage() {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, true, MqttQoS.AT_LEAST_ONCE, false, 123);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader("device", packedId.incrementAndGet(), true, true, true, 1, true, false, 60);
        MqttConnectPayload payload = new MqttConnectPayload("clientId", "topic", "message".getBytes(StandardCharsets.UTF_8), "username", "password".getBytes(StandardCharsets.UTF_8));
        return new MqttConnectMessage(mqttFixedHeader, variableHeader, payload);
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    MqttPublishMessage getMqttPublishMessage() {
        return getMqttPublishMessage("v1/gateway/telemetry");
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：处理结果。
     */
    MqttPublishMessage getDeviceMqttPublishMessage() {
        return getMqttPublishMessage("v1/devices/me/telemetry");
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `topicName`：主题名称或主题对象。
     * 返回：处理结果。
     */
    MqttPublishMessage getMqttPublishMessage(String topicName) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, true, MqttQoS.AT_LEAST_ONCE, false, 123);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topicName, packedId.incrementAndGet());
        ByteBuf payload = Unpooled.wrappedBuffer("{\"testKey\":\"testValue\"}".getBytes());
        return new MqttPublishMessage(mqttFixedHeader, variableHeader, payload);
    }

    /**
     * 功能：验证 `givenMqttConnectMessage_whenProcessMqttMsg_thenProcessConnect` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMqttConnectMessage_whenProcessMqttMsg_thenProcessConnect() {
        MqttConnectMessage msg = getMqttConnectMessage();
        willDoNothing().given(handler).processConnect(ctx, msg);

        handler.channelRead(ctx, msg);

        assertThat(handler.address, is(IP_ADDR));
        assertThat(handler.deviceSessionCtx.getChannel(), is(ctx));
        verify(handler, never()).doDisconnect();
        verify(handler, times(1)).processConnect(ctx, msg);
    }

    /**
     * 功能：验证 `givenQueueLimit_whenEnqueueRegularSessionMsgOverLimit_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueLimit_whenEnqueueRegularSessionMsgOverLimit_thenOK() {
        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(MSG_QUEUE_LIMIT).collect(Collectors.toList());
        messages.forEach(msg -> handler.enqueueRegularSessionMsg(ctx, msg));
        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(MSG_QUEUE_LIMIT));
        assertThat(handler.deviceSessionCtx.getMsgQueueSnapshot(), contains(messages.toArray()));
    }

    /**
     * 功能：验证 `givenQueueLimit_whenEnqueueRegularSessionMsgOverLimit_thenCtxClose` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueLimit_whenEnqueueRegularSessionMsgOverLimit_thenCtxClose() {
        final int limit = MSG_QUEUE_LIMIT + 1;
        willDoNothing().given(handler).processMsgQueue(ctx);
        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(limit).collect(Collectors.toList());

        messages.forEach((msg) -> handler.enqueueRegularSessionMsg(ctx, msg));

        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(MSG_QUEUE_LIMIT));
        verify(handler, times(limit)).enqueueRegularSessionMsg(any(), any());
        verify(handler, times(MSG_QUEUE_LIMIT)).processMsgQueue(any());
        verify(ctx, times(1)).close();
    }

    /**
     * 功能：验证 `givenMqttConnectMessageAndPublishImmediately_whenProcessMqttMsg_thenEnqueueRegularSessionMsg` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMqttConnectMessageAndPublishImmediately_whenProcessMqttMsg_thenEnqueueRegularSessionMsg() {
        givenMqttConnectMessage_whenProcessMqttMsg_thenProcessConnect();

        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(MSG_QUEUE_LIMIT).collect(Collectors.toList());

        messages.forEach((msg) -> handler.channelRead(ctx, msg));

        assertThat(handler.address, is(IP_ADDR));
        assertThat(handler.deviceSessionCtx.getChannel(), is(ctx));
        assertThat(handler.deviceSessionCtx.isConnected(), is(false));
        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(MSG_QUEUE_LIMIT));
        assertThat(handler.deviceSessionCtx.getMsgQueueSnapshot(), contains(messages.toArray()));
        verify(handler, never()).doDisconnect();
        verify(handler, times(1)).processConnect(any(), any());
        verify(handler, times(MSG_QUEUE_LIMIT)).enqueueRegularSessionMsg(any(), any());
        verify(handler, never()).processRegularSessionMsg(any(), any());
        messages.forEach((msg) -> verify(handler, times(1)).enqueueRegularSessionMsg(ctx, msg));
    }

    /**
     * 功能：验证 `givenMessageQueue_whenProcessMqttMsgConcurrently_thenEnqueueRegularSessionMsg` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMessageQueue_whenProcessMqttMsgConcurrently_thenEnqueueRegularSessionMsg() throws InterruptedException {
        //given
        assertThat(handler.deviceSessionCtx.isConnected(), is(false));
        assertThat(MSG_QUEUE_LIMIT, greaterThan(2));
        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(MSG_QUEUE_LIMIT).collect(Collectors.toList());
        messages.forEach((msg) -> handler.enqueueRegularSessionMsg(ctx, msg));
        willDoNothing().given(handler).processRegularSessionMsg(any(), any());
        executor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getClass().getName()));

        CountDownLatch readyLatch = new CountDownLatch(MSG_QUEUE_LIMIT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(MSG_QUEUE_LIMIT);

        Stream.iterate(0, i -> i + 1).limit(MSG_QUEUE_LIMIT).forEach(x ->
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        assertThat(startLatch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
                        handler.processMsgQueue(ctx);
                        finishLatch.countDown();
                    } catch (Exception e) {
                        log.error("Failed to run processMsgQueue", e);
                        fail("Failed to run processMsgQueue");
                    }
                }));

        //when
        assertThat(readyLatch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
        handler.deviceSessionCtx.setConnected(true);
        startLatch.countDown();
        assertThat(finishLatch.await(TIMEOUT, TimeUnit.SECONDS), is(true));

        //then
        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(0));
        assertThat(handler.deviceSessionCtx.getMsgQueueSnapshot(), empty());
        verify(handler, times(MSG_QUEUE_LIMIT)).processRegularSessionMsg(any(), any());
        messages.forEach((msg) -> verify(handler, times(1)).processRegularSessionMsg(ctx, msg));
    }

    /**
     * 功能：验证 `givenMqttMessage_whenDeviceProfileMqttTransport_thenTopicAddedToMetadata` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMqttMessage_whenDeviceProfileMqttTransport_thenTopicAddedToMetadata() {
        MqttPublishMessage message = getDeviceMqttPublishMessage();
        when(context.getJsonMqttAdaptor()).thenReturn(new JsonMqttAdaptor());
        handler.deviceSessionCtx.setConnected(true);
        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
        mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(new JsonTransportPayloadConfiguration());
        deviceProfileData.setTransportConfiguration(mqttDeviceProfileTransportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        handler.deviceSessionCtx.setDeviceProfile(deviceProfile);

        handler.processRegularSessionMsg(ctx, message);

        TbMsgMetaData expectedMd = new TbMsgMetaData();
        expectedMd.putValue(DataConstants.MQTT_TOPIC, message.variableHeader().topicName());

        verify(transportService, times(1)).process(any(), (TransportProtos.PostTelemetryMsg) any(), eq(expectedMd), any());
    }
}