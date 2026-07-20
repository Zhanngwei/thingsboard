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
package org.thingsboard.server.transport.coap.provision;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

/**
 * 中文说明：
 * 1. `CoapProvisionJsonDeviceTest` 是 ThingsBoard Application 中验证 `CoapProvisionJsonDevice` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractCoapIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class CoapProvisionJsonDeviceTest extends AbstractCoapIntegrationTest {

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
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

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
    public void testProvisioningWithBadKeyDevice() throws Exception {
        processTestProvisioningWithBadKeyDevice();
    }


    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    private void processTestProvisioningDisabledDevice() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("errorMsg"));
        Assert.assertTrue(response.hasNonNull("status"));
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").asText());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").asText());
    }


    /**
     * 功能：处理设备凭据。
     * 参数：无。
     * 返回：无。
     */
    private void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }


    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    private void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        String requestCredentials = ",\"credentialsType\": \"ACCESS_TOKEN\",\"token\": \"test_token\"";
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish(requestCredentials));
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "ACCESS_TOKEN");
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }


    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    private void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        String requestCredentials = ",\"credentialsType\": \"X509_CERTIFICATE\",\"hash\": \"testHash\"";
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish(requestCredentials));
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "X509_CERTIFICATE");

        String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);

        Assert.assertEquals(deviceCredentials.getCredentialsId(), sha3Hash);

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), "testHash");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    private void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    private void processTestProvisioningWithBadKeyDevice() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKeyOrig")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("errorMsg"));
        Assert.assertTrue(response.hasNonNull("status"));
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").asText());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").asText());
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：无。
     * 返回：处理结果。
     */
    private byte[] createCoapClientAndPublish() throws Exception {
        return createCoapClientAndPublish("");
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private byte[] createCoapClientAndPublish(String deviceCredentials) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceCredentials);
        client = new CoapTestClient(accessToken, FeatureType.PROVISION);
        return client.postMethod(provisionRequestMsg.getBytes()).getPayload();
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String createTestProvisionMessage(String deviceCredentials) {
        return "{\"deviceName\":\"Test Provision device\",\"provisionDeviceKey\":\"testProvisionKey\", \"provisionDeviceSecret\":\"testProvisionSecret\"" + deviceCredentials + "}";
    }
}
