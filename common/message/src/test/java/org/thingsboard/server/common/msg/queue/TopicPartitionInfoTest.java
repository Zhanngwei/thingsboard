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
package org.thingsboard.server.common.msg.queue;

import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 中文说明：
 * 1. `TopicPartitionInfoTest` 是 ThingsBoard Common Message 中验证 `TopicPartitionInfo` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class TopicPartitionInfoTest {

    /**
     * 功能：验证 `givenTopicPartitionInfo_whenEquals_thenTrue` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenTopicPartitionInfo_whenEquals_thenTrue() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()));

    }

    /**
     * 功能：验证 `givenTopicPartitionInfo_whenEquals_thenFalse` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenTopicPartitionInfo_whenEquals_thenFalse() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(TenantId.SYS_TENANT_ID)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

    }
}