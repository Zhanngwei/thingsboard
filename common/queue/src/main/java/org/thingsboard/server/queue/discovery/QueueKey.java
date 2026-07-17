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
package org.thingsboard.server.queue.discovery;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;

/**
 * 中文说明：
 * 1. `QueueKey` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
@AllArgsConstructor
public class QueueKey {

    /**
     * 类型，用于区分不同处理分支。
     */
    private final ServiceType type;
    private final String queueName;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;

    /**
     * 功能：创建 `QueueKey` 实例，并初始化必要字段。
     * 参数：
     * - `type`：类型。
     * - `queue`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public QueueKey(ServiceType type, Queue queue) {
        this.type = type;
        this.queueName = queue.getName();
        this.tenantId = queue.getTenantId();
    }

    /**
     * 功能：创建 `QueueKey` 实例，并初始化必要字段。
     * 参数：
     * - `type`：类型。
     * - `queueRoutingInfo`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public QueueKey(ServiceType type, QueueRoutingInfo queueRoutingInfo) {
        this.type = type;
        this.queueName = queueRoutingInfo.getQueueName();
        this.tenantId = queueRoutingInfo.getTenantId();
    }

    /**
     * 功能：创建 `QueueKey` 实例，并初始化必要字段。
     * 参数：
     * - `type`：类型。
     * - `tenantId`：租户IDID。
     * 返回：新创建的对象实例。
     */
    public QueueKey(ServiceType type, TenantId tenantId) {
        this.type = type;
        this.queueName = DataConstants.MAIN_QUEUE_NAME;
        this.tenantId = tenantId != null ? tenantId : TenantId.SYS_TENANT_ID;
    }

    /**
     * 功能：创建 `QueueKey` 实例，并初始化必要字段。
     * 参数：
     * - `type`：类型。
     * 返回：新创建的对象实例。
     */
    public QueueKey(ServiceType type) {
        this.type = type;
        this.queueName = DataConstants.MAIN_QUEUE_NAME;
        this.tenantId = TenantId.SYS_TENANT_ID;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "QK(" + queueName + "," + type + "," +
                (TenantId.SYS_TENANT_ID.equals(tenantId) ? "system" : tenantId) +
                ')';
    }
}
