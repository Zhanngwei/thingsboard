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
package org.thingsboard.server.transport.mqtt.mqttv3.provision;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportProtos.CredentialsDataProto;
import org.thingsboard.server.gen.transport.TransportProtos.CredentialsType;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceCredentialsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateBasicMqttCredRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestSubscribeOnTopicCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC;

/**
 * 中文说明：
 * 1. `MqttProvisionProtoDeviceTest` 是 ThingsBoard Application 中验证 `MqttProvisionProtoDevice` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class MqttProvisionProtoDeviceTest extends AbstractMqttIntegrationTest {

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
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testProvisioningDisabledDevice() throws Exception {
        processTestProvisioningDisabledDevice();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testProvisioningCheckPreProvisionedDevice() throws Exception {
        processTestProvisioningCheckPreProvisionedDevice();
    }

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
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        processTestProvisioningCreateNewDeviceWithAccessToken();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testProvisioningCreateNewDeviceWithCert() throws Exception {
        processTestProvisioningCreateNewDeviceWithCert();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        processTestProvisioningCreateNewDeviceWithMqttBasic();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testProvisioningWithBadKeyDevice() throws Exception {
        processTestProvisioningWithBadKeyDevice();
    }


    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningDisabledDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.DISABLED)
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg result = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());
        Assert.assertNotNull(result);
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), result.getStatus().name());
    }

    /**
     * 功能：处理设备凭据。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().name());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().name());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder()
                .setValidateDeviceTokenRequestMsg(ValidateDeviceTokenRequestMsg.newBuilder().setToken("test_token").build())
                .build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(
                createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.ACCESS_TOKEN, requestCredentials)));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.ACCESS_TOKEN);
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder()
                .setValidateDeviceX509CertRequestMsg(
                        ValidateDeviceX509CertRequestMsg.newBuilder().setHash("testHash").build())
                .build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(
                createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.X509_CERTIFICATE, requestCredentials)));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.X509_CERTIFICATE);

        String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);

        Assert.assertEquals(deviceCredentials.getCredentialsId(), sha3Hash);

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), "testHash");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningCreateNewDeviceWithMqttBasic() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        CredentialsDataProto requestCredentials = CredentialsDataProto.newBuilder().setValidateBasicMqttCredRequestMsg(
                ValidateBasicMqttCredRequestMsg.newBuilder()
                        .setClientId("test_clientId")
                        .setUserName("test_username")
                        .setPassword("test_password")
                    .build()
        ).build();

        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(
                createMqttClientAndPublish(createTestsProvisionMessage(CredentialsType.MQTT_BASIC, requestCredentials)));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().toString());
        Assert.assertEquals(deviceCredentials.getCredentialsType(), DeviceCredentialsType.MQTT_BASIC);
        Assert.assertEquals(deviceCredentials.getCredentialsId(), EncryptionUtil.getSha3Hash("|", "test_clientId", "test_username"));

        BasicMqttCredentials mqttCredentials = new BasicMqttCredentials();
        mqttCredentials.setClientId("test_clientId");
        mqttCredentials.setUserName("test_username");
        mqttCredentials.setPassword("test_password");

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), JacksonUtil.toString(mqttCredentials));
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().toString());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.getCredentialsType().name());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.getStatus().name());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestProvisioningWithBadKeyDevice() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKeyOrig")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        ProvisionDeviceResponseMsg response = ProvisionDeviceResponseMsg.parseFrom(createMqttClientAndPublish());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.getStatus().name());
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    protected byte[] createMqttClientAndPublish() throws Exception {
        byte[] provisionRequestMsg = createTestProvisionMessage();
        return createMqttClientAndPublish(provisionRequestMsg);
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `provisionRequestMsg`：请求对象。
     * 返回：处理结果。
     */
    protected byte[] createMqttClientAndPublish(byte[] provisionRequestMsg) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait("provision");
        MqttTestCallback onProvisionCallback = new MqttTestSubscribeOnTopicCallback(DEVICE_PROVISION_RESPONSE_TOPIC);
        client.setCallback(onProvisionCallback);
        client.subscribe(DEVICE_PROVISION_RESPONSE_TOPIC, MqttQoS.AT_MOST_ONCE);
        client.publishAndWait(DEVICE_PROVISION_REQUEST_TOPIC, provisionRequestMsg);
        onProvisionCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        client.disconnect();
        return onProvisionCallback.getPayloadBytes();
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `credentialsType`：类型。
     * - `credentialsData`：待处理数据。
     * 返回：处理结果。
     */
    protected byte[] createTestsProvisionMessage(CredentialsType credentialsType, CredentialsDataProto credentialsData) throws Exception {
        return ProvisionDeviceRequestMsg.newBuilder()
                .setDeviceName("Test Provision device")
                .setCredentialsType(credentialsType != null ? credentialsType : CredentialsType.ACCESS_TOKEN)
                .setCredentialsDataProto(credentialsData != null ? credentialsData: CredentialsDataProto.newBuilder().build())
                .setProvisionDeviceCredentialsMsg(
                        ProvisionDeviceCredentialsMsg.newBuilder()
                                .setProvisionDeviceKey("testProvisionKey")
                                .setProvisionDeviceSecret("testProvisionSecret")
                ).build()
                .toByteArray();
    }


    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    protected byte[] createTestProvisionMessage() throws Exception {
        return createTestsProvisionMessage(null, null);
    }

}
