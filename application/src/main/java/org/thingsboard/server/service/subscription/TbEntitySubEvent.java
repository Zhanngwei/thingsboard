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
package org.thingsboard.server.service.subscription;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Set;

/**
 * Information about the local websocket subscriptions.
 */
/**
 * 中文说明：
 * 1. `TbEntitySubEvent` 是 ThingsBoard Application 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Builder
@Data
public class TbEntitySubEvent {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final EntityId entityId;
    /**
     * 类型，用于区分不同处理分支。
     */
    private final ComponentLifecycleEvent type;
    private final TbSubscriptionsInfo info;
    /**
     * 序号，用于控制处理规模或位置。
     */
    private final int seqNumber;

    /**
     * 功能：判断时间戳。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasTsOrAttrSub() {
        return info != null && (info.tsAllKeys || info.attrAllKeys || info.tsKeys != null || info.attrKeys != null);
    }
}
