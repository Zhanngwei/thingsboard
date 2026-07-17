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
package org.thingsboard.server.common.transport.activity.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 中文说明：
 * 1. `AllEventsActivityStrategyTest` 是 ThingsBoard Common Transport 中验证 `AllEventsActivityStrategy` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class AllEventsActivityStrategyTest {

    /**
     * 策略对象，封装可复用的处理规则。
     */
    private AllEventsActivityStrategy strategy;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setUp() {
        strategy = AllEventsActivityStrategy.getInstance();
    }

    /**
     * 功能：验证`On Activity`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOnActivity() {
        assertTrue(strategy.onActivity(), "onActivity() should always return true.");
    }

    /**
     * 功能：验证上报相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOnReportingPeriodEnd() {
        assertTrue(strategy.onReportingPeriodEnd(), "onReportingPeriodEnd() should always return true.");
    }

}
