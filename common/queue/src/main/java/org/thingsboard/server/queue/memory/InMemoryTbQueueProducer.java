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

import lombok.Data;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;

/**
 * 中文说明：
 * 1. `InMemoryTbQueueProducer` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueProducer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
public class InMemoryTbQueueProducer<T extends TbQueueMsg> implements TbQueueProducer<T> {

    /**
     * `storage` 字段，保存当前对象的对应属性。
     */
    private final InMemoryStorage storage;

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String defaultTopic;

    /**
     * 功能：创建 `InMemoryTbQueueProducer` 实例，并初始化必要字段。
     * 参数：
     * - `storage`：`storage` 参数。
     * - `defaultTopic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public InMemoryTbQueueProducer(InMemoryStorage storage, String defaultTopic) {
        this.storage = storage;
        this.defaultTopic = defaultTopic;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void init() {

    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback) {
        boolean result = storage.put(tpi.getFullTopicName(), msg);
        if (result) {
            if (callback != null) {
                callback.onSuccess(null);
            }
        } else {
            if (callback != null) {
                callback.onFailure(new RuntimeException("Failure add msg to InMemoryQueue"));
            }
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {

    }
}
