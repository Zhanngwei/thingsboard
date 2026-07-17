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
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.pskBootstrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/**
 * 中文说明：
 * 1. `PskLwm2mIntegrationTest` 是 ThingsBoard Application 中验证 `PskLwm2mIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractSecurityLwM2MIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class PskLwm2mIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    /**
     * 功能：验证`With Psk Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithPskConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
        String identity = CLIENT_PSK_IDENTITY;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security security = psk(SECURE_URI,
                shortServerId,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        this.basicTestConnection(security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                "await on client state (Psk_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithPskConnectLwm2mBadPskKeyByLength_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
        String identity = CLIENT_PSK_IDENTITY + "_BadLength";
        String keyPsk = CLIENT_PSK_KEY + "05AC";
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "Key must be HexDec format: 32, 64, 128 characters!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }


    // Bootstrap + Lwm2m
    /**
     * 功能：验证`With Psk Connect Bs Success Update Two Sections Bootstrap And Lm2m Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithPskConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK_BS;
        String identity = CLIENT_PSK_IDENTITY_BS;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security securityBs = pskBootstrap(SECURE_URI_BS,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        this.basicTestConnection(securityBs,
                deviceCredentials,
                COAP_CONFIG_BS,
                clientEndpoint,
                transportConfiguration,
                "await on client state (PskBS two section)",
                expectedStatusesRegistrationBsSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }
}
