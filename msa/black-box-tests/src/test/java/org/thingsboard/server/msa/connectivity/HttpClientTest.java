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
import com.google.gson.JsonObject;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

/**
 * 中文说明：
 * 1. `HttpClientTest` 是 ThingsBoard Microservices 中验证 `HttpClient` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractContainerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DisableUIListeners
public class HttpClientTest extends AbstractContainerTest {
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
     * 功能：执行 `telemetryUpload` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void telemetryUpload() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.readTree(createPayload().toString()));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    /**
     * 功能：获取`Attributes`。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void getAttributes() throws Exception {
        String accessToken = testRestClient.getDeviceCredentialsByDeviceId(device.getId()).getCredentialsId();
        assertThat(accessToken).isNotNull();

        JsonNode sharedAttribute = mapper.readTree(createPayload().toString());
        testRestClient.postTelemetryAttribute(DEVICE, device.getId(), SHARED_SCOPE, sharedAttribute);

        JsonNode clientAttribute = mapper.readTree(createPayload().toString());
        testRestClient.postAttribute(accessToken, clientAttribute);

        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        JsonNode attributes = testRestClient.getAttributes(accessToken, null, null);
        assertThat(attributes.get("shared")).isEqualTo(sharedAttribute);
        assertThat(attributes.get("client")).isEqualTo(clientAttribute);

        JsonNode attributes2 = testRestClient.getAttributes(accessToken, null, "stringKey");
        assertThat(attributes2.get("shared").get("stringKey")).isEqualTo(sharedAttribute.get("stringKey"));
        assertThat(attributes2.has("client")).isFalse();

        JsonNode attributes3 =  testRestClient.getAttributes(accessToken, "longKey,stringKey", null);

        assertThat(attributes3.has("shared")).isFalse();
        assertThat(attributes3.get("client").get("longKey")).isEqualTo(clientAttribute.get("longKey"));
        assertThat(attributes3.get("client").get("stringKey")).isEqualTo(clientAttribute.get("stringKey"));
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

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", device.getName());

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String credentialsType = provisionResponse.get("credentialsType");
        String credentialsValue = provisionResponse.get("credentialsValue");
        String status = provisionResponse.get("status");

        assertThat(credentialsType).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(credentialsValue).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(status).isEqualTo("SUCCESS");

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

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", testDeviceName);

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String credentialsType = provisionResponse.get("credentialsType");
        String credentialsValue = provisionResponse.get("credentialsValue");
        String status = provisionResponse.get("status");

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(credentialsType).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(credentialsValue).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(status).isEqualTo("SUCCESS");

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

        JsonPath provisionResponse = testRestClient.postProvisionRequest(provisionRequest.toString());

        String status = provisionResponse.get("status");

        assertThat(status).isEqualTo("NOT_FOUND");
    }

}
