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
package org.thingsboard.server.queue;

import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. `TbQueueConsumer` 是 ThingsBoard Common 中定义队列能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbQueueConsumer<T extends TbQueueMsg> {

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    String getTopic();

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void subscribe();

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：无。
     */
    void subscribe(Set<TopicPartitionInfo> partitions);

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void stop();

    /**
     * 功能：执行 `unsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void unsubscribe();

    /**
     * 功能：执行 `poll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    List<T> poll(long durationInMillis);

    /**
     * 功能：执行 `commit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void commit();

    /**
     * 功能：判断`Stopped`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isStopped();

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    List<String> getFullTopicNames();

}
