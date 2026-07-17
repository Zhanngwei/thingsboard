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
package org.thingsboard.server.common.data.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `Queue` 是 ThingsBoard Common Data 中承载队列信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseDataWithAdditionalInfo`、`HasName`、`HasTenantId`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class Queue extends BaseDataWithAdditionalInfo<QueueId> implements HasName, HasTenantId {
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    private String name;
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @NoXss
    @Length(fieldName = "topic")
    private String topic;
    private int pollInterval;
    /**
     * `partitions` 字段，保存当前对象的对应属性。
     */
    private int partitions;
    private boolean consumerPerPartition;
    /**
     * 版本包处理超时时间，用于控制时间范围或等待时长。
     */
    private long packProcessingTimeout;
    private SubmitStrategy submitStrategy;
    /**
     * 策略对象，封装可复用的处理规则。
     */
    private ProcessingStrategy processingStrategy;

    /**
     * 功能：创建 `Queue` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Queue() {
    }

    /**
     * 功能：创建 `Queue` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Queue(QueueId id) {
        super(id);
    }

    /**
     * 功能：创建 `Queue` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueConfiguration`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public Queue(TenantId tenantId, TenantProfileQueueConfiguration queueConfiguration) {
        this.tenantId = tenantId;
        this.name = queueConfiguration.getName();
        this.topic = queueConfiguration.getTopic();
        this.pollInterval = queueConfiguration.getPollInterval();
        this.partitions = queueConfiguration.getPartitions();
        this.consumerPerPartition = queueConfiguration.isConsumerPerPartition();
        this.packProcessingTimeout = queueConfiguration.getPackProcessingTimeout();
        this.submitStrategy = queueConfiguration.getSubmitStrategy();
        this.processingStrategy = queueConfiguration.getProcessingStrategy();
        setAdditionalInfo(queueConfiguration.getAdditionalInfo());
    }


    /**
     * 功能：获取`Custom Properties`。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    public String getCustomProperties() {
        return Optional.ofNullable(getAdditionalInfo())
                .map(info -> info.get("customProperties"))
                .filter(JsonNode::isTextual).map(JsonNode::asText).orElse(null);
    }

}
