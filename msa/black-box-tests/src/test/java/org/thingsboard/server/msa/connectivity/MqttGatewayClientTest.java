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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultGatewayPrototype;

@DisableUIListeners
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`MqttGatewayClientTest` 是 ThingsBoard MSA 测试模块 中的协议连通性测试类型，用于验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Integration Test / Client Adapter。
 */
public class MqttGatewayClientTest extends AbstractContainerTest {
    /**
     * 字段说明：
     * 1. 保存 `gatewayDevice` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private Device gatewayDevice;
    private MqttClient mqttClient;
    /**
     * 字段说明：
     * 1. 保存 `createdDevice` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    private Device createdDevice;
    private MqttMessageListener listener;
    private JsonParser jsonParser = new JsonParser();

    /**
     * 字段说明：
     * 1. 保存 `handlerExecutor` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
     * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
     * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
     */
    AbstractListeningExecutor handlerExecutor;

    @BeforeMethod
    /**
     * 方法说明：
     * 1. 职责：执行 `createGateway` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void createGateway() throws Exception {
        this.handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 4;
            }
        };
        handlerExecutor.init();

        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        testRestClient.login("tenant@thingsboard.org", "tenant");
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        gatewayDevice = testRestClient.postDevice("", defaultGatewayPrototype());
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        DeviceCredentials gatewayDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(gatewayDevice.getId());

        // MQTT 状态会影响连接、订阅、发布确认或重传流程，需要与协议时序保持一致。
        this.listener = new MqttMessageListener();
        // MQTT 状态会影响连接、订阅、发布确认或重传流程，需要与协议时序保持一致。
        this.mqttClient = getMqttClient(gatewayDeviceCredentials, listener);
        // MQTT 状态会影响连接、订阅、发布确认或重传流程，需要与协议时序保持一致。
        this.createdDevice = createDeviceThroughGateway(mqttClient, gatewayDevice);
    }

    @AfterMethod
    /**
     * 方法说明：
     * 1. 职责：执行 `removeGateway` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void removeGateway()  {
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        testRestClient.deleteDeviceIfExists(this.gatewayDevice.getId());
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        testRestClient.deleteDeviceIfExists(this.createdDevice.getId());
        // 异步结果通过回调继续处理，调用线程不能假设这里已经完成完整链路。
        this.listener = null;
        // MQTT 状态会影响连接、订阅、发布确认或重传流程，需要与协议时序保持一致。
        this.mqttClient = null;
        this.createdDevice = null;
        // 条件分支用于保护配置、连接状态、测试前置条件或协议状态机边界。
        if (handlerExecutor != null) {
            handlerExecutor.destroy();
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `telemetryUpload` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void telemetryUpload() throws Exception {
        // MQTT 状态会影响连接、订阅、发布确认或重传流程，需要与协议时序保持一致。
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        // MQTT 状态会影响连接、订阅、发布确认或重传流程，需要与协议时序保持一致。
        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), -1).toString().getBytes())).get();
        // 网络调用用于验证服务端可达性或订阅链路，失败时需要区分连接问题和业务断言问题。
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `telemetryUploadWithTs` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void telemetryUploadWithTs() throws Exception {
        long ts = 1451649600512L;

        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), ts).toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `publishAttributeUpdateToServer` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void publishAttributeUpdateToServer() throws Exception {
        testRestClient.getDeviceCredentialsByDeviceId(createdDevice.getId());

        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        JsonObject clientAttributes = new JsonObject();
        clientAttributes.addProperty("attr1", "value1");
        clientAttributes.addProperty("attr2", true);
        clientAttributes.addProperty("attr3", 42.0);
        clientAttributes.addProperty("attr4", 73);
        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received attributes: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("attr1", "attr2", "attr3", "attr4"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("attr1").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr2").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr3").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr4").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `responseDataOnAttributesRequestCheck` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void responseDataOnAttributesRequestCheck() throws Exception {
        testRestClient.getDeviceCredentialsByDeviceId(createdDevice.getId());
        JsonObject sharedAttributes = new JsonObject();
        sharedAttributes.addProperty("attr1", "value1");
        sharedAttributes.addProperty("attr2", true);
        sharedAttributes.addProperty("attr3", 42.0);
        sharedAttributes.addProperty("attr4", 73);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();

        testRestClient.postTelemetryAttribute(DataConstants.DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));
        var event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        JsonObject requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        requestData.addProperty("key", "attr1");

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        JsonObject responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();
        assertThat(responseData.has("value")).isTrue();
        assertThat(responseData.get("value").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());

        requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        JsonArray keys = new JsonArray();
        keys.add("attr1");
        keys.add("attr2");
        requestData.add("keys", keys);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();

        assertThat(responseData.has("values")).isTrue();
        assertThat(responseData.get("values").getAsJsonObject().get("attr1").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());
        assertThat(responseData.get("values").getAsJsonObject().get("attr2").getAsString()).isEqualTo(sharedAttributes.get("attr2").getAsString());

        requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        keys = new JsonArray();
        keys.add("attr1");
        keys.add("undefined");
        requestData.add("keys", keys);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();

        assertThat(responseData.has("values")).isTrue();
        assertThat(responseData.get("values").getAsJsonObject().get("attr1").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());
        assertThat(responseData.get("values").getAsJsonObject().entrySet()).hasSize(1);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `requestAttributeValuesFromServer` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void requestAttributeValuesFromServer() throws Exception {
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        // Add a new client attribute
        JsonObject clientAttributes = new JsonObject();
        String clientAttributeValue = StringUtils.randomAlphanumeric(8);
        clientAttributes.addProperty("clientAttr", clientAttributeValue);

        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received ws telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(1);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnly("clientAttr");
        assertThat(actualLatestTelemetry.getDataValuesByKey("clientAttr").get(1)).isEqualTo(clientAttributeValue);

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty("sharedAttr", sharedAttributeValue);

        // Subscribe for attribute update event
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));
        MqttEvent sharedAttributeEvent = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        // Catch attribute update event
        assertThat(sharedAttributeEvent).isNotNull();
        assertThat(sharedAttributeEvent.getTopic()).isEqualTo("v1/gateway/attributes");

        // Subscribe to attributes response
        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        checkAttribute(true, clientAttributeValue);
        checkAttribute(false, sharedAttributeValue);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `subscribeToAttributeUpdatesFromServer` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void subscribeToAttributeUpdatesFromServer() throws Exception {
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();
        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);
        String sharedAttributeName = "sharedAttr";
        // Add a new shared attribute

        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty(sharedAttributeName, sharedAttributeValue);

        JsonObject gatewaySharedAttributeValue = new JsonObject();
        gatewaySharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewaySharedAttributeValue.add("data", sharedAttributes);

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));

        MqttEvent event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText())
                .isEqualTo(sharedAttributeValue);

        // Update the shared attribute value
        JsonObject updatedSharedAttributes = new JsonObject();
        String updatedSharedAttributeValue = StringUtils.randomAlphanumeric(8);
        updatedSharedAttributes.addProperty(sharedAttributeName, updatedSharedAttributeValue);

        JsonObject gatewayUpdatedSharedAttributeValue = new JsonObject();
        gatewayUpdatedSharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewayUpdatedSharedAttributeValue.add("data", updatedSharedAttributes);

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(updatedSharedAttributes.toString()));
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText())
                .isEqualTo(updatedSharedAttributeValue);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `serverSideRpc` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void serverSideRpc() throws Exception {
        String gatewayRpcTopic = "v1/gateway/rpc";
        mqttClient.on(gatewayRpcTopic, listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Send an RPC from the server
        JsonObject serverRpcPayload = new JsonObject();
        serverRpcPayload.addProperty("method", "getValue");
        serverRpcPayload.addProperty("params", true);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName())));
        ListenableFuture<JsonNode> future = service.submit(() -> {
            try {
                return testRestClient.postServerSideRpc(createdDevice.getId(), mapper.readTree(serverRpcPayload.toString()));
            } catch (IOException e) {
                return null;
            }
        });

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        service.shutdownNow();

        assertThat(requestFromServer).isNotNull();
        assertThat(requestFromServer.getMessage()).isNotNull();
        JsonNode requestFromServerJson = JacksonUtil.toJsonNode(requestFromServer.getMessage());
        assertThat(requestFromServerJson.get("device").asText()).isEqualTo(createdDevice.getName());
        assertThat(requestFromServerJson.get("data").get("method").asText()).isEqualTo("getValue");
        assertThat(requestFromServerJson.get("data").get("params").asText()).isEqualTo("true");
        int requestId = requestFromServerJson.get("data").get("id").asInt();

        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        JsonObject gatewayResponse = new JsonObject();
        gatewayResponse.addProperty("device", createdDevice.getName());
        gatewayResponse.addProperty("id", requestId);
        gatewayResponse.add("data", clientResponse);
        // Send a response to the server's RPC request

        mqttClient.publish(gatewayRpcTopic, Unpooled.wrappedBuffer(gatewayResponse.toString().getBytes())).get();
        JsonNode serverResponse = future.get(5 * timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(serverResponse).isEqualTo(mapper.readTree(clientResponse.toString()));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `deviceCreationAfterDeleted` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    public void deviceCreationAfterDeleted() throws Exception {
        testRestClient.deleteDevice(this.createdDevice.getId());
        testRestClient.getDeviceById(this.createdDevice.getId(), HttpStatus.NOT_FOUND.value());
        this.createdDevice = createDeviceThroughGateway(mqttClient, gatewayDevice);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkAttribute` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    private void checkAttribute(boolean client, String expectedValue) throws Exception {
        JsonObject gatewayAttributesRequest = new JsonObject();
        int messageId = new Random().nextInt(100);
        gatewayAttributesRequest.addProperty("id", messageId);
        gatewayAttributesRequest.addProperty("device", createdDevice.getName());
        gatewayAttributesRequest.addProperty("client", client);
        String attributeName;
        if (client)
            attributeName = "clientAttr";
        else
            attributeName = "sharedAttr";
        gatewayAttributesRequest.addProperty("key", attributeName);
        log.info(gatewayAttributesRequest.toString());
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(gatewayAttributesRequest.toString().getBytes())).get();
        MqttEvent clientAttributeEvent = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(clientAttributeEvent).isNotNull();
        JsonObject responseMessage = new JsonParser().parse(Objects.requireNonNull(clientAttributeEvent).getMessage()).getAsJsonObject();

        assertThat(responseMessage.get("id").getAsInt()).isEqualTo(messageId);
        assertThat(responseMessage.get("device").getAsString()).isEqualTo(createdDevice.getName());
        assertThat(responseMessage.entrySet()).hasSize(3);
        assertThat(responseMessage.get("value").getAsString()).isEqualTo(expectedValue);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDeviceThroughGateway` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    private Device createDeviceThroughGateway(MqttClient mqttClient, Device gatewayDevice) throws Exception {
        if (timeoutMultiplier > 1) {
            TimeUnit.SECONDS.sleep(30);
        }

        String deviceName = "mqtt_device" + RandomStringUtils.randomAlphabetic(5);
        mqttClient.publish("v1/gateway/connect", Unpooled.wrappedBuffer(createGatewayConnectPayload(deviceName).toString().getBytes()), MqttQoS.AT_LEAST_ONCE).get();

        if (timeoutMultiplier > 1) {
            TimeUnit.SECONDS.sleep(30);
        }

        List<EntityRelation> relations = testRestClient.findRelationByFrom(gatewayDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).hasSize(1);

        EntityId createdEntityId = relations.get(0).getTo();
        DeviceId createdDeviceId = new DeviceId(createdEntityId.getId());
        return testRestClient.getDeviceById(createdDeviceId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getOwnerId` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    private String getOwnerId() {
        return "Tenant[" + gatewayDevice.getTenantId().getId() + "]MqttGatewayClientTestDevice[" + gatewayDevice.getId().getId() + "]";
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getMqttClient` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
     * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
     * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
     * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
     * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
     * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
     * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
     * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
     */
    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId(getOwnerId());
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername(deviceCredentials.getCredentialsId());
        MqttClient mqttClient = MqttClient.create(clientConfig, listener, handlerExecutor);
        mqttClient.connect("localhost", 1883).get();
        return mqttClient;
    }

    @Data
    /**
     * 中文说明：
     * 1. 类目的：`MqttMessageListener` 是 ThingsBoard MSA 测试模块 中的协议连通性测试类型，用于验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
     * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
     * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 Integration Test / Client Adapter。
     */
    private class MqttMessageListener implements MqttHandler {
        /**
         * 字段说明：
         * 1. 保存 `events` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
         * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
         * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
         */
        private final BlockingQueue<MqttEvent> events;

        /**
         * 方法说明：
         * 1. 职责：执行 `MqttMessageListener` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
         * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
         * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
         * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
         * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
         * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
         * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
         * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
         */
        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onMessage` 对应的协议连通性测试类型流程，完成配置读取、连接管理、协议处理、页面操作、健康探测或测试断言。
         * 2. 参数：输入参数通常代表配置项、目标地址、设备凭据、MQTT 消息、Web 元素、测试夹具、回调或异步结果。
         * 3. 返回值：返回客户端状态、协议响应、通知结果、测试对象、Future/回调句柄或 `void`；`void` 通常通过副作用、断言或回调表达结果。
         * 4. 调用时机：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁时，由 Spring Boot、Netty pipeline、测试框架、Selenium 页面对象、监控调度器或上层客户端调用。
         * 5. 使用流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
         * 6. 线程安全：方法本身不额外声明线程安全；Netty 事件循环、Selenium 驱动、测试框架并发和 Spring Bean 生命周期决定并发边界。
         * 7. 事务/缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理；若测试通过 REST 或协议入口触发服务端写入，事务由目标服务端模块控制。
         * 8. MQTT/Actor/数据库/Rule Engine：方法可能直接处理 MQTT 或通过 HTTP/WebSocket/CoAP 间接影响 Transport、Actor、Rule Engine 和 DAO 流程。
         */
        public ListenableFuture<Void> onMessage(String topic, ByteBuf message) {
            log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
            events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
            return Futures.immediateVoidFuture();
        }

    }

    @Data
    /**
     * 中文说明：
     * 1. 类目的：`MqttEvent` 是 ThingsBoard MSA 测试模块 中的协议连通性测试类型，用于验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
     * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
     * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 Integration Test / Client Adapter。
     */
    private class MqttEvent {
        /**
         * 字段说明：
         * 1. 保存 `topic` 对应的配置、客户端、通道、测试夹具、页面元素、回调或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、协议事件、Selenium 定位、Docker 环境或测试数据。
         * 3. 生命周期与持有对象一致；单例服务字段随应用存在，连接/测试字段随单次会话或测试用例存在。
         * 4. 设计为字段是为了复用连接、配置、页面对象或异步状态，减少重复初始化和跨方法参数传递。
         * 5. 线程安全取决于字段类型；Netty 通道、异步 Future、WebDriver 和集合状态需要遵守各自的并发模型。
         */
        private final String topic;
        private final String message;
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`MqttGatewayClientTest` 在 ThingsBoard MSA 测试模块 中承担协议连通性测试类型职责，核心目的是验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
