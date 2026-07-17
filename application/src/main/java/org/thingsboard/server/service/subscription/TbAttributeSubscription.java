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
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 中文说明：
 * 1. `TbAttributeSubscription` 是 ThingsBoard Application 中围绕属性提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbSubscription`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class TbAttributeSubscription extends TbSubscription<TelemetrySubscriptionUpdate> {

    /**
     * 功能：创建 `TbAttributeSubscription` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `sessionId`：会话ID。
     * - `subscriptionId`：订阅ID。
     * - `tenantId`：租户IDID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Getter private final long queryTs;
    @Getter private final boolean allKeys;
    @Getter private final Map<String, Long> keyStates;
    @Getter private final TbAttributeSubscriptionScope scope;

    @Builder
    public TbAttributeSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                   BiConsumer<TbSubscription<TelemetrySubscriptionUpdate>, TelemetrySubscriptionUpdate> updateProcessor,
                                   long queryTs, boolean allKeys, Map<String, Long> keyStates, TbAttributeSubscriptionScope scope) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.ATTRIBUTES, updateProcessor);
        this.queryTs = queryTs;
        this.allKeys = allKeys;
        this.keyStates = keyStates;
        this.scope = scope;
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
