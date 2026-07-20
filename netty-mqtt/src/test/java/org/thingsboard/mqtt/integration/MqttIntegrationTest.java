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
package org.thingsboard.mqtt.integration;

import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttConnectResult;
import org.thingsboard.mqtt.integration.server.MqttServer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 中文说明：
 * 1. `MqttIntegrationTest` 是 ThingsBoard Netty MQTT Client 中验证 `MqttIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public class MqttIntegrationTest {

    /**
     * 主机地址常量，用于统一引用固定值。
     */
    static final String MQTT_HOST = "localhost";
    static final int KEEPALIVE_TIMEOUT_SECONDS = 2;
    /**
     * 延迟时间常量，用于统一引用固定值。
     */
    static final long RECONNECT_DELAY_SECONDS = 10L;

    /**
     * 事件循环，用于支撑当前网络或外部服务交互。
     */
    EventLoopGroup eventLoopGroup;
    MqttServer mqttServer;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    MqttClient mqttClient;

    /**
     * 处理器列表，用于保存一组待处理对象。
     */
    AbstractListeningExecutor handlerExecutor;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void init() throws Exception {
        this.handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 4;
            }
        };
        handlerExecutor.init();

        this.eventLoopGroup = new NioEventLoopGroup();

        this.mqttServer = new MqttServer();
        this.mqttServer.init();
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void destroy() throws InterruptedException {
        if (this.mqttClient != null) {
            this.mqttClient.disconnect();
        }
        if (this.mqttServer != null) {
            this.mqttServer.shutdown();
        }
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
        }
        if (this.handlerExecutor != null) {
            this.handlerExecutor.destroy();
        }
    }

    /**
     * 功能：验证 `givenActiveMqttClient_whenNoActivityForKeepAliveTimeout_thenDisconnectClient` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenActiveMqttClient_whenNoActivityForKeepAliveTimeout_thenDisconnectClient() throws Throwable {
        //given
        this.mqttClient = initClient();

        log.warn("Sending publish messages...");
        CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            Thread.sleep(30);
            Future<Void> pubFuture = publishMsg();
            pubFuture.addListener(future -> latch.countDown());
        }

        log.warn("Waiting for messages acknowledgments...");
        boolean awaitResult = latch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(awaitResult);
        log.warn("Messages are delivered successfully...");

        //when
        log.warn("Starting idle period...");
        Thread.sleep(5000);

        //then
        List<MqttMessageType> allReceivedEvents = this.mqttServer.getEventsFromClient();
        long disconnectCount = allReceivedEvents.stream().filter(type -> type == MqttMessageType.DISCONNECT).count();

        Assert.assertEquals(1, disconnectCount);
    }

    /**
     * 功能：发送或提交消息。
     * 参数：无。
     * 返回：异步处理结果。
     */
    private Future<Void> publishMsg() {
        return this.mqttClient.publish(
                "test/topic",
                Unpooled.wrappedBuffer("payload".getBytes(StandardCharsets.UTF_8)),
                MqttQoS.AT_MOST_ONCE);
    }

    /**
     * 功能：初始化或启动客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    private MqttClient initClient() throws Exception {
        MqttClientConfig config = new MqttClientConfig();
        config.setOwnerId("MqttIntegrationTest");
        config.setTimeoutSeconds(KEEPALIVE_TIMEOUT_SECONDS);
        config.setReconnectDelay(RECONNECT_DELAY_SECONDS);
        MqttClient client = MqttClient.create(config, null, handlerExecutor);
        client.setEventLoop(this.eventLoopGroup);
        Promise<MqttConnectResult> connectFuture = client.connect(MQTT_HOST, this.mqttServer.getMqttPort());

        String hostPort = MQTT_HOST + ":" + this.mqttServer.getMqttPort();
        MqttConnectResult result;
        try {
            result = connectFuture.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            connectFuture.cancel(true);
            client.disconnect();
            throw new RuntimeException(String.format("Failed to connect to MQTT server at %s.", hostPort));
        }
        if (!result.isSuccess()) {
            connectFuture.cancel(true);
            client.disconnect();
            throw new RuntimeException(String.format("Failed to connect to MQTT server at %s. Result code is: %s", hostPort, result.getReturnCode()));
        }
        return client;
    }
}