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
package org.thingsboard.server.transport.mqtt.mqttv5.provision;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.AbstractMqttV5Test;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC;

/**
 * 中文说明：
 * 1. `MqttV5ProvisionDeviceTest` 是 ThingsBoard Application 中验证 `MqttV5ProvisionDevice` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttV5Test`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class MqttV5ProvisionDeviceTest extends AbstractMqttV5Test {

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    DeviceService deviceService;

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        processTestProvisioningCreateNewDeviceWithoutCredentials();
    }

    /**
     * 功能：处理设备凭据。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        super.processBeforeTest(configProperties);
        byte[] result = createMqttClientAndPublish();
        JsonNode response = JacksonUtil.fromBytes(result);
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    protected byte[] createMqttClientAndPublish() throws Exception {
        String provisionRequestMsg = "{\"deviceName\":\"Test Provision device\",\"provisionDeviceKey\":\"testProvisionKey\", \"provisionDeviceSecret\":\"testProvisionSecret\"}";
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait("provision");
        MqttV5TestCallback onProvisionCallback = new MqttV5TestCallback(DEVICE_PROVISION_RESPONSE_TOPIC);
        client.setCallback(onProvisionCallback);
        client.subscribe(DEVICE_PROVISION_RESPONSE_TOPIC, MqttQoS.AT_MOST_ONCE);
        client.publishAndWait(DEVICE_PROVISION_REQUEST_TOPIC, provisionRequestMsg.getBytes());
        onProvisionCallback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        client.disconnect();
        return onProvisionCallback.getPayloadBytes();
    }
}
