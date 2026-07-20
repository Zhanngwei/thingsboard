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

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `TopicPartitionInfo` 是 ThingsBoard Common Message 中承载分区信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString
public class TopicPartitionInfo {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String topic;
    private final TenantId tenantId;
    /**
     * 分区，用于定位消息或数据所属分区。
     */
    private final Integer partition;
    /**
     * 主题名称，用于标识或展示当前对象。
     */
    @Getter
    private final String fullTopicName;
    /**
     * 是否满足分区条件。
     */
    @Getter
    private final boolean myPartition;

    /**
     * 功能：创建 `TopicPartitionInfo` 实例，并初始化必要字段。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `tenantId`：租户IDID。
     * - `partition`：分区标识或分区信息。
     * - `myPartition`：分区标识或分区信息。
     * 返回：新创建的对象实例。
     */
    @Builder
    public TopicPartitionInfo(String topic, TenantId tenantId, Integer partition, boolean myPartition) {
        this.topic = topic;
        this.tenantId = tenantId;
        this.partition = partition;
        this.myPartition = myPartition;
        String tmp = topic;
        if (tenantId != null && !tenantId.isNullUid()) {
            tmp += ".isolated." + tenantId.getId().toString();
        }
        if (partition != null) {
            tmp += "." + partition;
        }
        this.fullTopicName = tmp;
    }

    /**
     * 功能：执行 `newByTopic` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    public TopicPartitionInfo newByTopic(String topic) {
        return new TopicPartitionInfo(topic, this.tenantId, this.partition, this.myPartition);
    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getTopic() {
        return topic;
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public Optional<TenantId> getTenantId() {
        return Optional.ofNullable(tenantId);
    }

    /**
     * 功能：获取分区。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public Optional<Integer> getPartition() {
        return Optional.ofNullable(partition);
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicPartitionInfo that = (TopicPartitionInfo) o;
        return topic.equals(that.topic) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(partition, that.partition) &&
                fullTopicName.equals(that.fullTopicName);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return Objects.hash(fullTopicName);
    }
}
