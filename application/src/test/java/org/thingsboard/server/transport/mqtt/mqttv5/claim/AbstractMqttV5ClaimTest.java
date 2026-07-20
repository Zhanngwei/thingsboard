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
package org.thingsboard.server.transport.mqtt.mqttv5.claim;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.transport.mqtt.mqttv5.AbstractMqttV5Test;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_CLAIM_TOPIC;

/**
 * 中文说明：
 * 1. `AbstractMqttV5ClaimTest` 是 ThingsBoard Application 中验证 `AbstractMqttV5Claim` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttV5Test`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractMqttV5ClaimTest extends AbstractMqttV5Test {
    /**
     * 密码常量，用于统一引用固定值。
     */
    protected static final String CUSTOMER_USER_PASSWORD = "customerUser123!";

    /**
     * 客户对象，用于描述当前业务场景。
     */
    protected User customerAdmin;
    protected Customer savedCustomer;

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processTestClaimingDevice() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        byte[] payloadBytes;
        byte[] failurePayloadBytes;
        payloadBytes = "{\"secretKey\":\"value\", \"durationMs\":60000}".getBytes();
        failurePayloadBytes = "{\"secretKey\":\"value\", \"durationMs\":1}".getBytes();
        validateClaimResponse(client, payloadBytes, failurePayloadBytes);
    }

    /**
     * 功能：校验响应。
     * 参数：
     * - `client`：客户端对象。
     * - `payloadBytes`：`payloadBytes` 参数。
     * - `failurePayloadBytes`：`failurePayloadBytes` 参数。
     * 返回：无。
     */
    protected void validateClaimResponse(MqttV5TestClient client, byte[] payloadBytes, byte[] failurePayloadBytes) throws Exception {
        client.publishAndWait(DEVICE_CLAIM_TOPIC, failurePayloadBytes);

        loginUser(customerAdmin.getName(), CUSTOMER_USER_PASSWORD);
        ClaimRequest claimRequest = new ClaimRequest("value");

        ClaimResponse claimResponse = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest()),
                20,
                100
        );

        assertEquals(claimResponse, ClaimResponse.FAILURE);

        client.publishAndWait(DEVICE_CLAIM_TOPIC, payloadBytes);
        client.disconnect();

        ClaimResult claimResult = doExecuteWithRetriesAndInterval(
                () -> doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResult.class, status().isOk()),
                20,
                100
        );
        assertEquals(claimResult.getResponse(), ClaimResponse.SUCCESS);
        Device claimedDevice = claimResult.getDevice();
        assertNotNull(claimedDevice);
        assertNotNull(claimedDevice.getCustomerId());
        assertEquals(customerAdmin.getCustomerId(), claimedDevice.getCustomerId());

        claimResponse = doPostClaimAsync("/api/customer/device/" + savedDevice.getName() + "/claim", claimRequest, ClaimResponse.class, status().isBadRequest());
        assertEquals(claimResponse, ClaimResponse.CLAIMED);
    }

    /**
     * 功能：保存或创建客户。
     * 参数：无。
     * 返回：无。
     */
    protected void createCustomerAndUser() throws Exception {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Test Claiming Customer");
        savedCustomer = doPost("/api/customer", customer, Customer.class);
        assertNotNull(savedCustomer);
        assertEquals(tenantId, savedCustomer.getTenantId());

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(savedCustomer.getId());
        user.setEmail("customer@thingsboard.org");

        customerAdmin = createUser(user, CUSTOMER_USER_PASSWORD);
        assertNotNull(customerAdmin);
        assertEquals(customerAdmin.getCustomerId(), savedCustomer.getId());
    }
}
