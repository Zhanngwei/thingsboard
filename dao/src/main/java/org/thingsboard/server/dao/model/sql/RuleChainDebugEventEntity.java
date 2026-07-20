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
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_CHAIN_DEBUG_EVENT_TABLE_NAME;

/**
 * 中文说明：
 * 1. `RuleChainDebugEventEntity` 是 ThingsBoard DAO 中表示事件持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `EventEntity`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RULE_CHAIN_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RuleChainDebugEventEntity extends EventEntity<RuleChainDebugEvent> implements BaseEntity<RuleChainDebugEvent> {

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Column(name = EVENT_MESSAGE_COLUMN_NAME)
    private String message;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    /**
     * 功能：创建 `RuleChainDebugEventEntity` 实例，并初始化必要字段。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleChainDebugEventEntity(RuleChainDebugEvent event) {
        super(event);
        this.message = event.getMessage();
        this.error = event.getError();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleChainDebugEvent toData() {
        return RuleChainDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .message(message)
                .error(error).build();
    }

}
