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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.TestCoapClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

/**
 * 中文说明：
 * 1. 类目的：`CoapClientTest` 是 ThingsBoard MSA 测试模块 中的协议连通性测试类型，用于验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Integration Test / Client Adapter。
 */
@DisableUIListeners
public class CoapClientTest  extends AbstractContainerTest {
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private TestCoapClient client;

    /**
     * 设备对象，用于描述当前业务场景。
     */
    private Device device;
    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    /**
     * 功能：执行 `provisionRequestForDeviceWithPreProvisionedStrategy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionRequestForDeviceWithPreProvisionedStrategy() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        JsonNode provisionResponse = JacksonUtil.fromBytes(createCoapClientAndPublish(device.getName()));

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    /**
     * 功能：执行 `provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy() throws Exception {

        String testDeviceName = "test_provision_device";

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());

        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        JsonNode provisionResponse = JacksonUtil.fromBytes(createCoapClientAndPublish(testDeviceName));

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    /**
     * 功能：执行 `provisionRequestForDeviceWithDisabledProvisioningStrategy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionRequestForDeviceWithDisabledProvisioningStrategy() throws Exception {

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);

        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish(null));

        assertThat(response.get("status").asText()).isEqualTo("NOT_FOUND");
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private byte[] createCoapClientAndPublish(String deviceName) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceName);
        client = new TestCoapClient(TestCoapClient.getFeatureTokenUrl(FeatureType.PROVISION));
        return client.postMethod(provisionRequestMsg.getBytes()).getPayload();
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String createTestProvisionMessage(String deviceName) {
        ObjectNode provisionRequest = JacksonUtil.newObjectNode();
        provisionRequest.put("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.put("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        if (deviceName != null) {
            provisionRequest.put("deviceName", deviceName);
        }
        return provisionRequest.toString();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CoapClientTest` 在 ThingsBoard MSA 测试模块 中承担协议连通性测试类型职责，核心目的是验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
