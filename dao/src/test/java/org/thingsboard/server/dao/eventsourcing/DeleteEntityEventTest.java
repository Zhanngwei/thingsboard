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
package org.thingsboard.server.dao.eventsourcing;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.byLessThan;

/**
 * 中文说明：
 * 1. `DeleteEntityEventTest` 是 ThingsBoard DAO 中验证 `DeleteEntityEvent` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
class DeleteEntityEventTest {

    /**
     * 功能：验证时间戳相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testBuilderDefaultTs() {
        assertThat(DeleteEntityEvent.builder().build().getTs())
                .isCloseTo(System.currentTimeMillis(), byLessThan(TimeUnit.MINUTES.toMillis(1)));

        assertThat(DeleteEntityEvent.builder().ts(Long.MIN_VALUE).build().getTs())
                .isEqualTo(Long.MIN_VALUE);
        assertThat(DeleteEntityEvent.builder().ts(Long.MAX_VALUE).build().getTs())
                .isEqualTo(Long.MAX_VALUE);
        assertThat(DeleteEntityEvent.builder().ts(-1L).build().getTs())
                .isEqualTo(-1L);
        assertThat(DeleteEntityEvent.builder().ts(0L).build().getTs())
                .isEqualTo(0L);

        assertThat(DeleteEntityEvent.builder().ts(1692175215000L).build().getTs())
                .isEqualTo(1692175215000L);
    }
}