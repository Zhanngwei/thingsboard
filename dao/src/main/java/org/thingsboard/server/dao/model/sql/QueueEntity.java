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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `QueueEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.QUEUE_TABLE_NAME)
public class QueueEntity extends BaseSqlEntity<Queue> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.QUEUE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.QUEUE_NAME_PROPERTY)
    private String name;

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Column(name = ModelConstants.QUEUE_TOPIC_PROPERTY)
    private String topic;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Column(name = ModelConstants.QUEUE_POLL_INTERVAL_PROPERTY)
    private int pollInterval;

    /**
     * `partitions` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.QUEUE_PARTITIONS_PROPERTY)
    private int partitions;

    /**
     * 是否满足分区条件。
     */
    @Column(name = ModelConstants.QUEUE_CONSUMER_PER_PARTITION)
    private boolean consumerPerPartition;

    /**
     * 版本包处理超时时间，用于控制时间范围或等待时长。
     */
    @Column(name = ModelConstants.QUEUE_PACK_PROCESSING_TIMEOUT_PROPERTY)
    private long packProcessingTimeout;

    /**
     * 策略对象，封装可复用的处理规则。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.QUEUE_SUBMIT_STRATEGY_PROPERTY)
    private JsonNode submitStrategy;

    /**
     * 策略对象，封装可复用的处理规则。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.QUEUE_PROCESSING_STRATEGY_PROPERTY)
    private JsonNode processingStrategy;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.QUEUE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `QueueEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public QueueEntity() {
    }

    /**
     * 功能：创建 `QueueEntity` 实例，并初始化必要字段。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public QueueEntity(Queue queue) {
        if (queue.getId() != null) {
            this.setId(queue.getId().getId());
        }
        this.createdTime = queue.getCreatedTime();
        this.tenantId = DaoUtil.getId(queue.getTenantId());
        this.name = queue.getName();
        this.topic = queue.getTopic();
        this.pollInterval = queue.getPollInterval();
        this.partitions = queue.getPartitions();
        this.consumerPerPartition = queue.isConsumerPerPartition();
        this.packProcessingTimeout = queue.getPackProcessingTimeout();
        this.submitStrategy = JacksonUtil.valueToTree(queue.getSubmitStrategy());
        this.processingStrategy = JacksonUtil.valueToTree(queue.getProcessingStrategy());
        this.additionalInfo = queue.getAdditionalInfo();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Queue toData() {
        Queue queue = new Queue(new QueueId(getUuid()));
        queue.setCreatedTime(createdTime);
        queue.setTenantId(new TenantId(tenantId));
        queue.setName(name);
        queue.setTopic(topic);
        queue.setPollInterval(pollInterval);
        queue.setPartitions(partitions);
        queue.setConsumerPerPartition(consumerPerPartition);
        queue.setPackProcessingTimeout(packProcessingTimeout);
        queue.setSubmitStrategy(JacksonUtil.convertValue(submitStrategy, SubmitStrategy.class));
        queue.setProcessingStrategy(JacksonUtil.convertValue(processingStrategy, ProcessingStrategy.class));
        queue.setAdditionalInfo(additionalInfo);
        return queue;
    }
}