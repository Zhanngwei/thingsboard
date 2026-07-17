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
package org.thingsboard.server.transport.coap.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.server.resources.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.coapserver.DefaultCoapServerService;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;
import org.thingsboard.server.transport.coap.CoapTransportResource;
import org.thingsboard.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;

import static org.mockito.Mockito.spy;

/**
 * 中文说明：
 * 1. `CoapAttributesUpdatesIntegrationTest` 是 ThingsBoard Application 中验证 `CoapAttributesUpdatesIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractCoapAttributesIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class CoapAttributesUpdatesIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    /**
     * 传输层，表示当前对象的对应属性。
     */
    CoapTransportResource coapTransportResource;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    DefaultCoapServerService defaultCoapServerService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    DefaultTransportService defaultTransportService;

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        Resource api = defaultCoapServerService.getCoapServer().getRoot().getChild("api");
        coapTransportResource = spy( (CoapTransportResource) api.getChild("v1") );
        api.delete(api.getChild("v1") );
        api.add(coapTransportResource);
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Subscribe to attribute updates")
                .build();
        processBeforeTest(configProperties);
    }

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
     * 功能：验证服务端相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(false);
    }

    /**
     * 功能：验证状态相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerWithEmptyCurrentStateNotification() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(true);
    }
}
