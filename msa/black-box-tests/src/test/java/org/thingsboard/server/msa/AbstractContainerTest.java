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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;

import java.net.URI;
import java.util.Map;
import java.util.Random;


/**
 * 中文说明：
 * 1. 类目的：`AbstractContainerTest` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
@Slf4j
@Listeners(TestListener.class)
public abstract class AbstractContainerTest {

    /**
     * 设备常量，用于统一引用固定值。
     */
    protected final static String TEST_PROVISION_DEVICE_KEY = "test_provision_key";
    protected final static String TEST_PROVISION_DEVICE_SECRET = "test_provision_secret";
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    protected static long timeoutMultiplier = 1;
    protected ObjectMapper mapper = new ObjectMapper();
    private static final ContainerTestSuite containerTestSuite = ContainerTestSuite.getInstance();
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    protected static TestRestClient testRestClient;

    /**
     * 功能：执行 `beforeSuite` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeSuite
    public void beforeSuite() {
        if ("false".equals(System.getProperty("runLocal", "false"))) {
            containerTestSuite.start();
        }
        testRestClient = new TestRestClient(TestProperties.getBaseUrl());
        if (!"kafka".equals(System.getProperty("blackBoxTests.queue", "kafka"))) {
            timeoutMultiplier = 10;
        }
    }

    /**
     * 功能：执行 `afterSuite` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterSuite()
    public void afterSuite() {
        if (containerTestSuite.isActive()) {
            containerTestSuite.stop();
        }
    }

    /**
     * 功能：订阅`To Web Socket`。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `property`：`property` 参数。
     * 返回：处理结果。
     */
    protected WsClient subscribeToWebSocket(DeviceId deviceId, String scope, CmdsType property) throws Exception {
        String webSocketUrl = TestProperties.getWebSocketUrl();
        WsClient wsClient = new WsClient(new URI(webSocketUrl + "/api/ws/plugins/telemetry?token=" + testRestClient.getToken()), timeoutMultiplier);
        if (webSocketUrl.matches("^(wss)://.*$")) {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
        }
        wsClient.connectBlocking();

        JsonObject cmdsObject = new JsonObject();
        cmdsObject.addProperty("entityType", EntityType.DEVICE.name());
        cmdsObject.addProperty("entityId", deviceId.toString());
        cmdsObject.addProperty("scope", scope);
        cmdsObject.addProperty("cmdId", new Random().nextInt(100));

        JsonArray cmd = new JsonArray();
        cmd.add(cmdsObject);
        JsonObject wsRequest = new JsonObject();
        wsRequest.add(property.toString(), cmd);
        wsClient.send(wsRequest.toString());
        wsClient.waitForFirstReply();
        return wsClient;
    }

    /**
     * 功能：获取`Expected Latest Values`。
     * 参数：
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    protected Map<String, Long> getExpectedLatestValues(long ts) {
        return ImmutableMap.<String, Long>builder()
                .put("booleanKey", ts)
                .put("stringKey", ts)
                .put("doubleKey", ts)
                .put("longKey", ts)
                .build();
    }

    /**
     * 功能：保存或创建消息载荷。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected JsonObject createGatewayConnectPayload(String deviceName){
        JsonObject payload = new JsonObject();
        payload.addProperty("device", deviceName);
        return payload;
    }

    /**
     * 功能：保存或创建消息载荷。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    protected JsonObject createGatewayPayload(String deviceName, long ts){
        JsonObject payload = new JsonObject();
        payload.add(deviceName, createGatewayTelemetryArray(ts));
        return payload;
    }

    /**
     * 功能：保存或创建遥测。
     * 参数：
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    protected JsonArray createGatewayTelemetryArray(long ts){
        JsonArray telemetryArray = new JsonArray();
        if (ts > 0)
            telemetryArray.add(createPayload(ts));
        else
            telemetryArray.add(createPayload());
        return telemetryArray;
    }

    /**
     * 功能：保存或创建消息载荷。
     * 参数：
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    protected JsonObject createPayload(long ts) {
        JsonObject values = createPayload();
        JsonObject payload = new JsonObject();
        payload.addProperty("ts", ts);
        payload.add("values", values);
        return payload;
    }

    /**
     * 功能：保存或创建消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    protected JsonObject createPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringKey", "value1");
        values.addProperty("booleanKey", true);
        values.addProperty("doubleKey", 42.0);
        values.addProperty("longKey", 73L);

        return values;
    }

    /**
     * 中文说明：
     * 1. 类目的：`CmdsType` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
     * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
     * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
     */
    protected enum CmdsType {
        TS_SUB_CMDS("tsSubCmds"),
        HISTORY_CMDS("historyCmds"),
        ATTR_SUB_CMDS("attrSubCmds");

        /**
         * `text` 字段，保存当前对象的对应属性。
         */
        private final String text;

        /**
         * 功能：创建 `AbstractContainerTest` 实例，并初始化必要字段。
         * 参数：
         * - `text`：`text` 参数。
         * 返回：新创建的对象实例。
         */
        CmdsType(final String text) {
            this.text = text;
        }

        /**
         * 功能：生成当前对象的文本表示。
         * 参数：无。
         * 返回：文本结果。
         */
        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `provisionType`：类型。
     * 返回：处理结果。
     */
    protected DeviceProfile updateDeviceProfileWithProvisioningStrategy(DeviceProfile deviceProfile, DeviceProfileProvisionType provisionType) {
        DeviceProfileProvisionConfiguration provisionConfiguration;
        String testProvisionDeviceKey = TEST_PROVISION_DEVICE_KEY;
        deviceProfile.setProvisionType(provisionType);
        switch(provisionType) {
            case ALLOW_CREATE_NEW_DEVICES:
                provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(TEST_PROVISION_DEVICE_SECRET);
                break;
            case CHECK_PRE_PROVISIONED_DEVICES:
                provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(TEST_PROVISION_DEVICE_SECRET);
                break;
            default:
            case DISABLED:
                testProvisionDeviceKey = null;
                provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
                break;
        }
        DeviceProfileData deviceProfileData = deviceProfile.getProfileData();
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(testProvisionDeviceKey);
        return testRestClient.postDeviceProfile(deviceProfile);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractContainerTest` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
