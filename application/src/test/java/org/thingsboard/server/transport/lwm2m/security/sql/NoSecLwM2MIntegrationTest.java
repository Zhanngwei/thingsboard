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

import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOOTSTRAP_ONLY;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.LWM2M_ONLY;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/**
 * 中文说明：
 * 1. `NoSecLwM2MIntegrationTest` 是 ThingsBoard Application 中验证 `NoSecLwM2MIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractSecurityLwM2MIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class NoSecLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectLwm2mSuccessAndObserveTelemetry() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC;
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        super.basicTestConnectionObserveTelemetry(SECURITY_NO_SEC, clientCredentials, COAP_CONFIG, clientEndpoint);
    }

    // Bootstrap + Lwm2m
    /**
     * 功能：验证`With No Sec Connect Bs Success Update Two Sections Bootstrap And Lm2m Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS;
        String awaitAlias = "await on client state (NoSecBS two section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, BOTH, expectedStatusesRegistrationBsSuccess, ON_REGISTRATION_SUCCESS);
    }

    /**
     * 功能：验证`With No Sec Connect Bs Success Update Lwm2m Section And Lm2m Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectBsSuccess_UpdateLwm2mSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + LWM2M_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Lwm2m section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, LWM2M_ONLY, expectedStatusesRegistrationBsSuccess, ON_REGISTRATION_SUCCESS);
    }

    /**
     * 功能：验证`With No Sec Connect Bs Success Update Bootstrap Section And Lm2m Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectBsSuccess_UpdateBootstrapSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + BOOTSTRAP_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Bootstrap section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, BOOTSTRAP_ONLY, expectedStatusesBsSuccess, ON_BOOTSTRAP_SUCCESS);
    }

    /**
     * 功能：验证`With No Sec Connect Bs Success Update None Section And Lm2m Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectBsSuccess_UpdateNoneSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + NONE.name();
        String awaitAlias = "await on client state (NoSecBS None section)";
        basicTestConnectionBefore(clientEndpoint, awaitAlias, NONE, expectedStatusesBsSuccess, ON_BOOTSTRAP_SUCCESS);
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateTwoSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + BOTH.name();
        String awaitAlias = "await on client state (NoSecBS Trigger Two section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, BOTH);
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateBootstrapSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + BOOTSTRAP_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Trigger Bootstrap section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, BOOTSTRAP_ONLY);
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateLwm2mSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + LWM2M_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Trigger Lwm2m section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, LWM2M_ONLY);
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateNoneSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + NONE.name();
        String awaitAlias = "await on client state (NoSecBS Trigger None  section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, NONE);
    }
}
