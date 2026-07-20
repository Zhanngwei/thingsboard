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
package org.thingsboard.server.queue.common;

import lombok.Data;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `DefaultTbQueueMsg` 是 ThingsBoard Common Queue 中承载队列信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class DefaultTbQueueMsg implements TbQueueMsg {
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private final UUID key;
    private final byte[] data;
    /**
     * `headers` 字段，保存当前对象的对应属性。
     */
    private final DefaultTbQueueMsgHeaders headers;

    /**
     * 功能：创建 `DefaultTbQueueMsg` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public DefaultTbQueueMsg(TbQueueMsg msg) {
        this.key = msg.getKey();
        this.data = msg.getData();
        DefaultTbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        msg.getHeaders().getData().forEach(headers::put);
        this.headers = headers;
    }

}
