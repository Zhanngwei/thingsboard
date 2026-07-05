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
 * 中文说明：`DeduplicationData` 是去重数据辅助类，用于按配置聚合、去重、延迟和输出消息。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
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
    /*
     * 本类总结：`DeduplicationData` 负责按配置聚合、去重、延迟和输出消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
