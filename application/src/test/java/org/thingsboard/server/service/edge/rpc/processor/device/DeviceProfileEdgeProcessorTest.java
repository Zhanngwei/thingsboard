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
package org.thingsboard.server.service.edge.rpc.processor.device;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessorV1;


/**
 * 中文说明：
 * 1. `DeviceProfileEdgeProcessorTest` 是 ThingsBoard Application 中验证 `DeviceProfileEdgeProcessor` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractDeviceProcessorTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@SpringBootTest(classes = {DeviceProfileEdgeProcessorV1.class})
class DeviceProfileEdgeProcessorTest extends AbstractDeviceProcessorTest {

    /**
     * 功能：验证设备配置相关场景。
     * 参数：
     * - `edgeVersion`：`edgeVersion` 参数。
     * - `expectedDashboardIdMSB`：`expectedDashboardIdMSB` 参数。
     * - `expectedDashboardIdLSB`：`expectedDashboardIdLSB` 参数。
     * - `expectedRuleChainIdMSB`：`expectedRuleChainIdMSB` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideParameters")
    public void testDeviceProfileDefaultFields_notSendToEdgeOlder3_6_0IfNotAssigned(EdgeVersion edgeVersion, long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                                                                                    long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        updateDeviceProfileDefaultFields(expectedDashboardIdMSB, expectedDashboardIdLSB, expectedRuleChainIdMSB, expectedRuleChainIdLSB);

        edgeEvent.setEntityId(deviceProfileId.getId());

        DownlinkMsg downlinkMsg = deviceProfileProcessorV1.convertDeviceProfileEventToDownlink(edgeEvent, edgeId, edgeVersion);

        verify(downlinkMsg, expectedDashboardIdMSB, expectedDashboardIdLSB, expectedRuleChainIdMSB, expectedRuleChainIdLSB);
    }
}
