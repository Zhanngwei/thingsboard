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
package org.thingsboard.server.common.data.alarm;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;
import java.util.List;


/**
 * 中文说明：
 * 1. `AlarmApiCallResult` 是 ThingsBoard Common Data 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class AlarmApiCallResult implements Serializable {

    /**
     * 当前操作是否成功。
     */
    private final boolean successful;
    private final boolean created;
    /**
     * 当前对象是否发生变更。
     */
    private final boolean modified;
    private final boolean cleared;
    /**
     * 当前对象是否已经删除。
     */
    private final boolean deleted;
    private final AlarmInfo alarm;
    /**
     * `old` 字段，保存当前对象的对应属性。
     */
    private final Alarm old;
    private final List<EntityId> propagatedEntitiesList;

    /**
     * 功能：创建 `AlarmApiCallResult` 实例，并初始化必要字段。
     * 参数：
     * - `successful`：`successful` 参数。
     * - `created`：`created` 参数。
     * - `modified`：`modified` 参数。
     * - `cleared`：`cleared` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    private AlarmApiCallResult(boolean successful, boolean created, boolean modified, boolean cleared, boolean deleted, AlarmInfo alarm, Alarm old, List<EntityId> propagatedEntitiesList) {
        this.successful = successful;
        this.created = created;
        this.modified = modified;
        this.cleared = cleared;
        this.deleted = deleted;
        this.alarm = alarm;
        this.old = old;
        this.propagatedEntitiesList = propagatedEntitiesList;
    }

    /**
     * 功能：创建 `AlarmApiCallResult` 实例，并初始化必要字段。
     * 参数：
     * - `other`：键值映射。
     * - `propagatedEntitiesList`：数据列表。
     * 返回：新创建的对象实例。
     */
    public AlarmApiCallResult(AlarmApiCallResult other, List<EntityId> propagatedEntitiesList) {
        this.successful = other.successful;
        this.created = other.created;
        this.modified = other.modified;
        this.cleared = other.cleared;
        this.deleted = other.deleted;
        this.alarm = other.alarm;
        this.old = other.old;
        this.propagatedEntitiesList = propagatedEntitiesList;
    }

    /**
     * 功能：判断`Severity Changed`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSeverityChanged() {
        if (alarm == null || old == null) {
            return false;
        } else {
            return !alarm.getSeverity().equals(old.getSeverity());
        }
    }

    /**
     * 功能：判断`Acknowledged`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isAcknowledged() {
        if (alarm == null || old == null) {
            return false;
        } else {
            return alarm.isAcknowledged() != old.isAcknowledged();
        }
    }

    /**
     * 功能：获取`Old Severity`。
     * 参数：无。
     * 返回：处理结果。
     */
    public AlarmSeverity getOldSeverity() {
        return isSeverityChanged() ? old.getSeverity() : null;
    }

    /**
     * 功能：判断`Propagation Changed`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isPropagationChanged() {
        if (created) {
            return true;
        }
        if (alarm == null || old == null) {
            return false;
        }
        return (alarm.isPropagate() != old.isPropagate()) ||
                (alarm.isPropagateToOwner() != old.isPropagateToOwner()) ||
                (alarm.isPropagateToTenant() != old.isPropagateToTenant()) ||
                (!alarm.getPropagateRelationTypes().equals(old.getPropagateRelationTypes()));
    }

}
