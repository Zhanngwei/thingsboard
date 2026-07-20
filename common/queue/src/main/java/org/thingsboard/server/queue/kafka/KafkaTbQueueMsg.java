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
package org.thingsboard.server.queue.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;
import org.thingsboard.server.queue.common.DefaultTbQueueMsgHeaders;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `KafkaTbQueueMsg` 是 ThingsBoard Common Queue 中承载队列信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class KafkaTbQueueMsg implements TbQueueMsg {
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private final UUID key;
    private final TbQueueMsgHeaders headers;
    /**
     * 数据列表，用于保存一组待处理对象。
     */
    private final byte[] data;

    /**
     * 功能：创建 `KafkaTbQueueMsg` 实例，并初始化必要字段。
     * 参数：
     * - `record`：`record` 参数。
     * 返回：新创建的对象实例。
     */
    public KafkaTbQueueMsg(ConsumerRecord<String, byte[]> record) {
        this.key = UUID.fromString(record.key());
        TbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        record.headers().forEach(header -> {
            headers.put(header.key(), header.value());
        });
        this.headers = headers;
        this.data = record.value();
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public UUID getKey() {
        return key;
    }

    /**
     * 功能：获取`Headers`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueMsgHeaders getHeaders() {
        return headers;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public byte[] getData() {
        return data;
    }
}
