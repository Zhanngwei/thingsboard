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
package org.thingsboard.server.service.edge.rpc.processor.telemetry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;

/**
 * 中文说明：
 * 1. `TelemetryEdgeProcessorTest` 是 ThingsBoard Application 中验证 `TelemetryEdgeProcessor` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TelemetryEdgeProcessorTest {

    /**
     * 功能：验证数量限制相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConvert_maxSizeLimit() throws Exception {
        EdgeEvent edgeEvent = new EdgeEvent();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("value", StringUtils.randomAlphanumeric(10000));
        edgeEvent.setBody(body);
        DownlinkMsg downlinkMsg = new TelemetryEdgeProcessor().convertTelemetryEventToDownlink(edgeEvent);
        Assert.assertNull(downlinkMsg);
    }
}
