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
package org.thingsboard.server.transport.mqtt.mqttv3.credentials;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_TELEMETRY_TOPIC;

/**
 * 中文说明：
 * 1. `BasicMqttCredentialsTest` 是 ThingsBoard Application 中验证 `BasicMqttCredentials` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class BasicMqttCredentialsTest extends AbstractMqttIntegrationTest {

    /**
     * 客户端常量，用于统一引用固定值。
     */
    public static final String CLIENT_ID = "ClientId";
    public static final String USER_NAME1 = "UserName1";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_NAME2 = "UserName2";
    public static final String USER_NAME3 = "UserName3";
    /**
     * 密码常量，用于统一引用固定值。
     */
    public static final String PASSWORD = "secret";

    /**
     * 设备ID，用于发起外部调用或协议交互。
     */
    private Device clientIdDevice;
    private Device clientIdAndUserNameDevice1;
    /**
     * 密码，用于发起外部调用或协议交互。
     */
    private Device clientIdAndUserNameAndPasswordDevice2;
    private Device clientIdAndUserNameAndPasswordDevice3;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    private Device accessTokenDevice;
    private Device accessToken2Device;


    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() throws Exception {
        loginTenantAdmin();

        BasicMqttCredentials credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        clientIdDevice = createDevice("clientIdDevice", credValue);

        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME1);
        clientIdAndUserNameDevice1 = createDevice("clientIdAndUserNameDevice", credValue);

        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME2);
        credValue.setPassword(PASSWORD);
        clientIdAndUserNameAndPasswordDevice2 = createDevice("clientIdAndUserNameAndPasswordDevice", credValue);

        credValue = new BasicMqttCredentials();
        credValue.setClientId(CLIENT_ID);
        credValue.setUserName(USER_NAME3);
        credValue.setPassword(PASSWORD);
        clientIdAndUserNameAndPasswordDevice3 = createDevice("clientIdAndUserNameAndPasswordDevice2", credValue);

        accessTokenDevice = createDevice("accessTokenDevice", USER_NAME1);
        accessToken2Device = createDevice("accessToken2Device", USER_NAME2);
    }

    /**
     * 功能：验证凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCorrectCredentials() throws Exception {
        // Check that correct devices receive telemetry
        MqttTestClient mqttTestClient1 = new MqttTestClient();
        mqttTestClient1.connectAndWait(USER_NAME1);

        MqttTestClient mqttTestClient2 = new MqttTestClient(CLIENT_ID);
        mqttTestClient2.connectAndWait();

        MqttTestClient mqttTestClient3 = new MqttTestClient(CLIENT_ID);
        mqttTestClient3.connectAndWait(USER_NAME1);

        MqttTestClient mqttTestClient4 = new MqttTestClient(CLIENT_ID);
        mqttTestClient4.connectAndWait(USER_NAME2, PASSWORD);

        // Also correct. Random clientId and password, but matches access token
        MqttTestClient mqttTestClient5 = new MqttTestClient(StringUtils.randomAlphanumeric(10));
        mqttTestClient5.connectAndWait(USER_NAME2, StringUtils.randomAlphanumeric(10));

        testTelemetryIsDelivered(accessTokenDevice, mqttTestClient1);
        testTelemetryIsDelivered(clientIdDevice, mqttTestClient2);
        testTelemetryIsDelivered(clientIdAndUserNameDevice1, mqttTestClient3);
        testTelemetryIsDelivered(clientIdAndUserNameAndPasswordDevice2, mqttTestClient4);

        // Also correct. Random clientId and password, but matches access token
        testTelemetryIsDelivered(accessToken2Device, mqttTestClient5);
    }

    /**
     * 功能：验证密码相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCorrectClientIdAndUserNameButWrongPassword() throws Exception {
        // Not correct. Correct clientId and username, but wrong password
        MqttTestClient mqttTestClient = new MqttTestClient(CLIENT_ID);
        try {
            mqttTestClient.connectAndWait(USER_NAME3, "WRONG PASSWORD");
            Assert.fail(); // This should not happens, because we have a wrong password
        } catch (MqttException e) {
            Assert.assertEquals(5, e.getReasonCode()); // 4 - Reason code not authorized in MQTT v3
        }
        Assertions.assertThrows(MqttException.class, () -> {
            testTelemetryIsNotDelivered(clientIdAndUserNameAndPasswordDevice3, mqttTestClient);
        });
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `client`：客户端对象。
     * 返回：无。
     */
    private void testTelemetryIsDelivered(Device device, MqttTestClient client) throws Exception {
        testTelemetryIsDelivered(device, client, true);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `client`：客户端对象。
     * 返回：无。
     */
    private void testTelemetryIsNotDelivered(Device device, MqttTestClient client) throws Exception {
        testTelemetryIsDelivered(device, client, false);
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `client`：客户端对象。
     * - `ok`：`ok` 参数。
     * 返回：无。
     */
    private void testTelemetryIsDelivered(Device device, MqttTestClient client, boolean ok) throws Exception {
        String randomKey = StringUtils.randomAlphanumeric(10);
        List<String> expectedKeys = Arrays.asList(randomKey);
        client.publishAndWait(DEVICE_TELEMETRY_TOPIC, JacksonUtil.toString(JacksonUtil.newObjectNode().put(randomKey, true)).getBytes());

        String deviceId = device.getId().getId().toString();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", new TypeReference<>() {
            });
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        if (ok) {
            assertNotNull(actualKeys);

            Set<String> actualKeySet = new HashSet<>(actualKeys);
            Set<String> expectedKeySet = new HashSet<>(expectedKeys);

            assertEquals(expectedKeySet, actualKeySet);
        } else {
            assertNull(actualKeys);
        }
        client.disconnect();
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `clientIdCredValue`：客户端对象。
     * 返回：处理结果。
     */
    private Device createDevice(String deviceName, BasicMqttCredentials clientIdCredValue) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        device = doPost("/api/device", device, Device.class);

        DeviceCredentials clientIdCred =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        clientIdCred.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);


        clientIdCred.setCredentialsValue(JacksonUtil.toString(clientIdCredValue));
        doPost("/api/device/credentials", clientIdCred).andExpect(status().isOk());
        return device;
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    protected Device createDevice(String deviceName, String accessToken) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        device = doPost("/api/device", device, Device.class);

        DeviceCredentials clientIdCred =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        clientIdCred.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        clientIdCred.setCredentialsId(accessToken);
        doPost("/api/device/credentials", clientIdCred).andExpect(status().isOk());
        return device;
    }
}
