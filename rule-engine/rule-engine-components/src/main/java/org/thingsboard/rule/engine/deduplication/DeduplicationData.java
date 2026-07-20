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
package org.thingsboard.rule.engine.deduplication;

import lombok.Data;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.LinkedList;
import java.util.List;

/**
 * 中文说明：
 * 1. `DeduplicationData` 是 ThingsBoard Rule Engine Components 中承载 `Deduplication` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class DeduplicationData {

    /**
     * 消息列表，用于保存一组待处理对象。
     */
    private final List<TbMsg> msgList;
    /**
     * 是否满足`tickScheduled`条件。
     */
    private boolean tickScheduled;

    /**
     * 功能：创建 `DeduplicationData` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public DeduplicationData() {
        msgList = new LinkedList<>();
    }

    /**
     * 功能：执行 `size` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int size() {
        return msgList.size();
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void add(TbMsg msg) {
        msgList.add(msg);
    }

    /**
     * 功能：判断`Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isEmpty() {
        return msgList.isEmpty();
    }
}
