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
package org.thingsboard.server.dao.event;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventFilter;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.UUID;

/**
 * The Interface EventDao.
 */
/**
 * 中文说明：
 * 1. `EventDao` 是 ThingsBoard DAO 中定义事件能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EventDao {

    /**
     * Save or update event object async
     *
     * @param event the event object
     * @return saved event object future
     */
    /**
     * 功能：保存或创建`Async`。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAsync(Event event);

    /**
     * Find events by tenantId, entityId, eventType and pageLink.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param pageLink the pageLink
     * @return the event list
     */
    /**
     * 功能：获取`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, TimePageLink pageLink);

    /**
     * 功能：获取事件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventFilter`：`eventFilter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, EventFilter eventFilter, TimePageLink pageLink);

    /**
     * Find latest events by tenantId, entityId and eventType.
     *
     * @param tenantId the tenantId
     * @param entityId the entityId
     * @param eventType the eventType
     * @param limit the limit
     * @return the event list
     */
    /**
     * 功能：获取`Latest Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventType`：类型。
     * - `limit`：数量限制。
     * 返回：处理结果。
     */
    List<? extends Event> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit);

    /**
     * Executes stored procedure to cleanup old events. Uses separate ttl for debug and other events.
     * @param regularEventExpTs the expiration time of the regular events
     * @param debugEventExpTs the expiration time of the debug events
     * @param cleanupDb
     */
    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `regularEventExpTs`：时间戳。
     * - `debugEventExpTs`：时间戳。
     * - `cleanupDb`：`cleanupDb` 参数。
     * 返回：无。
     */
    void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb);

    /**
     * Removes all events for the specified entity and time interval
     *
     * @param tenantId
     * @param entityId
     * @param startTime
     * @param endTime
     */
    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * 返回：无。
     */
    void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime);

    /**
     *
     * Removes all events for the specified entity, event filter and time interval
     *
     * @param tenantId
     * @param entityId
     * @param eventFilter
     * @param startTime
     * @param endTime
     */
    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `eventFilter`：`eventFilter` 参数。
     * - `startTime`：开始时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void removeEvents(UUID tenantId, UUID entityId, EventFilter eventFilter, Long startTime, Long endTime);

    /**
     * 功能：执行 `migrateEvents` 对应的处理。
     * 参数：
     * - `regularEventTs`：时间戳。
     * - `debugEventTs`：时间戳。
     * 返回：无。
     */
    void migrateEvents(long regularEventTs, long debugEventTs);
}
