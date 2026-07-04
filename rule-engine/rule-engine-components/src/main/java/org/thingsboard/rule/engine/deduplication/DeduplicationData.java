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

@Data
/**
 * 中文说明：`DeduplicationData` 是去重数据辅助类，用于按配置聚合、去重、延迟和输出消息。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class DeduplicationData {

    /**
     * 字段说明：保存 `msgList`，表示当前规则链消息，供本类方法在规则节点处理流程中使用。
     */
    private final List<TbMsg> msgList;
    /**
     * 字段说明：保存 `tickScheduled`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private boolean tickScheduled;

    /**
     * 方法说明：构造 `DeduplicationData` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    public DeduplicationData() {
        msgList = new LinkedList<>();
    }

    /**
     * 方法说明：执行 `size` 对应的辅助逻辑，供 `DeduplicationData` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public int size() {
        return msgList.size();
    }

    /**
     * 方法说明：向消息、元数据、集合或缓存追加数据，供 `DeduplicationData` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void add(TbMsg msg) {
        msgList.add(msg);
    }

    /**
     * 方法说明：执行 `isEmpty` 对应的辅助逻辑，供 `DeduplicationData` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public boolean isEmpty() {
        return msgList.isEmpty();
    }
    /*
     * 本类总结：`DeduplicationData` 负责按配置聚合、去重、延迟和输出消息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
