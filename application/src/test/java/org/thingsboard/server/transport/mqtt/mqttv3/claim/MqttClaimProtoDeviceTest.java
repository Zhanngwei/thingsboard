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
package org.thingsboard.server.transport.mqtt.mqttv3.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

/**
 * 中文说明：
 * 1. `MqttClaimProtoDeviceTest` 是 ThingsBoard Application 中验证 `MqttClaimProtoDevice` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `MqttClaimDeviceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class MqttClaimProtoDeviceTest extends MqttClaimDeviceTest {

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Claim device")
                .gatewayName("Test Claim gateway")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
        createCustomerAndUser();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice(false);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice(true);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGatewayClaimingDevice() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device Proto", false);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGatewayClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload Proto", true);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `emptyPayload`：`emptyPayload` 参数。
     * 返回：无。
     */
    protected void processTestClaimingDevice(boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        byte[] payloadBytes;
        if (emptyPayload) {
            payloadBytes = getClaimDevice(0, emptyPayload).toByteArray();
        } else {
            payloadBytes = getClaimDevice(60000, emptyPayload).toByteArray();
        }
        byte[] failurePayloadBytes = getClaimDevice(1, emptyPayload).toByteArray();
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `emptyPayload`：`emptyPayload` 参数。
     * 返回：无。
     */
    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        processProtoTestGatewayClaimDevice(deviceName, emptyPayload);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `duration`：`duration` 参数。
     * - `emptyPayload`：`emptyPayload` 参数。
     * 返回：处理结果。
     */
    private TransportApiProtos.ClaimDevice getClaimDevice(long duration, boolean emptyPayload) {
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (!emptyPayload) {
            claimDeviceBuilder.setSecretKey("value");
        }
        if (duration > 0) {
            claimDeviceBuilder.setSecretKey("value");
            claimDeviceBuilder.setDurationMs(duration);
        }
        return claimDeviceBuilder.build();
    }


}
