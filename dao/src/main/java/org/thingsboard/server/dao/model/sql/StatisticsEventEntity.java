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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERRORS_OCCURRED_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGES_PROCESSED_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.STATS_EVENT_TABLE_NAME;

/**
 * 中文说明：
 * 1. `StatisticsEventEntity` 是 ThingsBoard DAO 中表示事件持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `EventEntity`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = STATS_EVENT_TABLE_NAME)
@NoArgsConstructor
public class StatisticsEventEntity extends EventEntity<StatisticsEvent> implements BaseEntity<StatisticsEvent> {

    /**
     * `messagesProcessed` 字段，保存当前对象的对应属性。
     */
    @Column(name = EVENT_MESSAGES_PROCESSED_COLUMN_NAME)
    private long messagesProcessed;
    /**
     * `errorsOccurred` 字段，保存当前对象的对应属性。
     */
    @Column(name = EVENT_ERRORS_OCCURRED_COLUMN_NAME)
    private long errorsOccurred;

    /**
     * 功能：创建 `StatisticsEventEntity` 实例，并初始化必要字段。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：新创建的对象实例。
     */
    public StatisticsEventEntity(StatisticsEvent event) {
        super(event);
        this.messagesProcessed = event.getMessagesProcessed();
        this.errorsOccurred = event.getErrorsOccurred();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public StatisticsEvent toData() {
        return StatisticsEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .messagesProcessed(messagesProcessed)
                .errorsOccurred(errorsOccurred)
                .build();
    }

}
