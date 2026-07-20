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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information about the local websocket subscriptions.
 */
/**
 * 中文说明：
 * 1. `TbEntityRemoteSubsInfo` 是 ThingsBoard Application 中承载实体信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@RequiredArgsConstructor
@Slf4j
public class TbEntityRemoteSubsInfo {
    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    private final TenantId tenantId;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Getter
    private final EntityId entityId;
    @Getter
    private final Map<String, TbSubscriptionsInfo> subs = new ConcurrentHashMap<>(); // By service ID

    /**
     * 功能：更新`And Check Is Empty`。
     * 参数：
     * - `serviceId`：服务ID。
     * - `event`：`event` 参数。
     * 返回：判断结果。
     */
    public boolean updateAndCheckIsEmpty(String serviceId, TbEntitySubEvent event) {
        var current = subs.get(serviceId);
        if (current != null && current.seqNumber > event.getSeqNumber()) {
            log.warn("[{}][{}] Duplicate subscription event received. Current: {}, Event: {}",
                    tenantId, entityId, current, event.getInfo());
            return false;
        }
        switch (event.getType()) {
            case CREATED:
                subs.put(serviceId, event.getInfo());
                break;
            case UPDATED:
                var newSubInfo = event.getInfo();
                if (newSubInfo.isEmpty()) {
                    subs.remove(serviceId);
                    return isEmpty();
                } else {
                    subs.put(serviceId, newSubInfo);
                }
                break;
            case DELETED:
                subs.remove(serviceId);
                return isEmpty();
        }
        return false;
    }

    /**
     * 功能：删除或清理`And Check Is Empty`。
     * 参数：
     * - `serviceId`：服务ID。
     * 返回：判断结果。
     */
    public boolean removeAndCheckIsEmpty(String serviceId) {
        if (subs.remove(serviceId) != null) {
            return subs.isEmpty();
        } else {
            return false;
        }
    }

    /**
     * 功能：判断`Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isEmpty() {
        return subs.isEmpty();
    }
}
