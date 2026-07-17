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
package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：
 * 1. `PartitionChangeEvent` 是 ThingsBoard Common Queue 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbApplicationEvent`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString(callSuper = true)
public class PartitionChangeEvent extends TbApplicationEvent {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -8731788167026510559L;

    /**
     * 类型，提供当前类调用的业务操作。
     */
    @Getter
    private final ServiceType serviceType;
    /**
     * `partitionsMap`集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap;

    /**
     * 功能：创建 `PartitionChangeEvent` 实例，并初始化必要字段。
     * 参数：
     * - `source`：`source` 参数。
     * - `serviceType`：服务对象。
     * - `partitionsMap`：分区标识或分区信息。
     * 返回：新创建的对象实例。
     */
    public PartitionChangeEvent(Object source, ServiceType serviceType, Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap) {
        super(source);
        this.serviceType = serviceType;
        this.partitionsMap = partitionsMap;
    }

    // only for service types that have single QueueKey
    /**
     * 功能：获取`Partitions`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Set<TopicPartitionInfo> getPartitions() {
        return partitionsMap.values().stream().findAny().orElse(Collections.emptySet());
    }

}
