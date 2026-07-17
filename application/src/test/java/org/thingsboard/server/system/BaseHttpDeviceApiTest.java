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
package org.thingsboard.server.system;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `BaseHttpDeviceApiTest` 是 ThingsBoard Application 中验证 `BaseHttpDeviceApi` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@TestPropertySource(properties = {
        "transport.http.enabled=true",
})
public abstract class BaseHttpDeviceApiTest extends AbstractControllerTest {

    private static final AtomicInteger idSeq = new AtomicInteger(new Random(System.currentTimeMillis()).nextInt());

    /**
     * 设备对象，用于描述当前业务场景。
     */
    protected Device device;
    protected DeviceCredentials deviceCredentials;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() throws Exception {
        loginTenantAdmin();
        device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    /**
     * 功能：验证`Get Attributes`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetAttributes() throws Exception {
        doGetAsync("/api/v1/" + "WRONG_TOKEN" + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isUnauthorized());
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isOk());

        Map<String, String> attrMap = new HashMap<>();
        attrMap.put("keyA", "valueA");
        mockMvc.perform(
                asyncDispatch(doPost("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes", attrMap, new String[]{}).andReturn()))
                .andExpect(status().isOk());
        Thread.sleep(2000);
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isOk());
    }

    /**
     * 功能：执行 `doGetAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doGetAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest;
        getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

    /**
     * 功能：执行 `doPostAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doPostAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = post(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

}
