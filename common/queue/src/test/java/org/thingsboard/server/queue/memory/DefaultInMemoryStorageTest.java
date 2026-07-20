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
package org.thingsboard.server.queue.memory;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 中文说明：
 * 1. `DefaultInMemoryStorageTest` 是 ThingsBoard Common Queue 中验证 `DefaultInMemoryStorage` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public class DefaultInMemoryStorageTest {
    /**
     * `MAX_POLL_SIZE`常量，用于统一引用固定值。
     */
    static final int MAX_POLL_SIZE = 1000;
    final Gson gson = new Gson();
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    final String topic = "tb_core_notification.tb-node-0";

    InMemoryStorage storage = new DefaultInMemoryStorage();

    /**
     * 功能：验证 `givenStorage_whenGetLagTotal_thenReturnInteger` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenStorage_whenGetLagTotal_thenReturnInteger() throws InterruptedException {
        assertThat(storage.getLagTotal()).isEqualTo(0);
        storage.put("main", mock(TbQueueMsg.class));
        assertThat(storage.getLagTotal()).isEqualTo(1);
        storage.put("main", mock(TbQueueMsg.class));
        assertThat(storage.getLagTotal()).isEqualTo(2);
        storage.put("hp", mock(TbQueueMsg.class));
        assertThat(storage.getLagTotal()).isEqualTo(3);
        storage.get("main");
        assertThat(storage.getLagTotal()).isEqualTo(1);
    }

    /**
     * 功能：验证 `givenQueueWithMoreThenBatchSize_whenPoll_thenReturnFullListAndSecondList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueWithMoreThenBatchSize_whenPoll_thenReturnFullListAndSecondList() throws InterruptedException {
        List<TbQueueMsg> msgs = new ArrayList<>(MAX_POLL_SIZE + 1);
        for (int i = 0; i < MAX_POLL_SIZE + 1; i++) {
            DefaultTbQueueMsg msg = gson.fromJson("{\"key\": \"" + UUID.randomUUID() + "\"}", DefaultTbQueueMsg.class);
            msgs.add(msg);
            storage.put(topic, msg);
        }

        assertThat(storage.getLagTotal()).as("total lag is 1001").isEqualTo(MAX_POLL_SIZE + 1);
        assertThat(storage.get(topic)).as("poll exactly 1000 msgs").isEqualTo(msgs.subList(0, MAX_POLL_SIZE));
        assertThat(storage.get(topic)).as("poll last 1 message").isEqualTo(msgs.subList(MAX_POLL_SIZE, MAX_POLL_SIZE + 1));
        assertThat(storage.getLagTotal()).as("total lag is zero").isEqualTo(0);
    }

    /**
     * 功能：验证`Poll Once`相关场景。
     * 参数：
     * - `msgCount`：待处理消息。
     * 返回：无。
     */
    private void testPollOnce(final int msgCount) throws InterruptedException {
        List<TbQueueMsg> msgs = new ArrayList<>(msgCount);
        for (int i = 0; i < msgCount; i++) {
            DefaultTbQueueMsg msg = gson.fromJson("{\"key\": \"" + UUID.randomUUID() + "\"}", DefaultTbQueueMsg.class);
            msgs.add(msg);
            storage.put(topic, msg);
        }

        assertThat(storage.getLagTotal()).as("total lag before poll").isEqualTo(msgCount);
        assertThat(storage.get(topic)).as("polled exactly msgs").isEqualTo(msgs.subList(0, msgCount));
        assertThat(storage.getLagTotal()).as("final lag is zero").isEqualTo(0);
    }

    /**
     * 功能：验证 `givenQueueWithExactBatchSize_whenPoll_thenReturnExactBatchSizeList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueWithExactBatchSize_whenPoll_thenReturnExactBatchSizeList() throws InterruptedException {
        testPollOnce(MAX_POLL_SIZE);
    }

    /**
     * 功能：验证 `givenQueueWithExactBatchSizeMinusOne_whenPoll_thenReturnCorrectSizeList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueWithExactBatchSizeMinusOne_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(MAX_POLL_SIZE - 1);
    }

    /**
     * 功能：验证 `givenQueueWithExactBatchSizeMinusTen_whenPoll_thenReturnCorrectSizeList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueWithExactBatchSizeMinusTen_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(MAX_POLL_SIZE - 10);
    }

    /**
     * 功能：验证 `givenQueueEmpty_whenPoll_thenReturnEmptyList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueEmpty_whenPoll_thenReturnEmptyList() throws InterruptedException {
        testPollOnce(0);
    }

    /**
     * 功能：验证 `givenQueueWithSingleMessage_whenPoll_thenReturnSingletonList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueWithSingleMessage_whenPoll_thenReturnSingletonList() throws InterruptedException {
        testPollOnce(1);
    }

    /**
     * 功能：验证 `givenQueueWithTwoMessages_whenPoll_thenReturnCorrectSizeList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueWithTwoMessages_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(2);
    }

    /**
     * 功能：验证 `givenQueueWithTenMessages_whenPoll_thenReturnCorrectSizeList` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenQueueWithTenMessages_whenPoll_thenReturnCorrectSizeList() throws InterruptedException {
        testPollOnce(10);
    }

}
