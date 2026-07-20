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

import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;

import java.util.List;

/**
 * 中文说明：
 * 1. `TbLocalSubscriptionService` 是 ThingsBoard Application 中定义 `Tb Local Subscription` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbLocalSubscriptionService {

    /**
     * 功能：保存或创建订阅。
     * 参数：
     * - `subscription`：`subscription` 参数。
     * 返回：无。
     */
    void addSubscription(TbSubscription<?> subscription);

    /**
     * 功能：处理事件。
     * 参数：
     * - `subEventCallback`：处理完成后的回调。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onSubEventCallback(TransportProtos.TbEntitySubEventCallbackProto subEventCallback, TbCallback callback);

    /**
     * 功能：处理事件。
     * 参数：
     * - `entityId`：实体IDID。
     * - `seqNumber`：`seqNumber` 参数。
     * - `entityUpdatesInfo`：实体对象。
     * - `empty`：`empty` 参数。
     * 返回：无。
     */
    void onSubEventCallback(EntityId entityId, int seqNumber, TbEntityUpdatesInfo entityUpdatesInfo, TbCallback empty);

    /**
     * 功能：执行 `cancelSubscription` 对应的处理。
     * 参数：
     * - `sessionId`：会话ID。
     * - `subscriptionId`：订阅ID。
     * 返回：无。
     */
    void cancelSubscription(String sessionId, int subscriptionId);

    /**
     * 功能：执行 `cancelAllSessionSubscriptions` 对应的处理。
     * 参数：
     * - `sessionId`：会话ID。
     * 返回：无。
     */
    void cancelAllSessionSubscriptions(String sessionId);

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tsUpdate`：`tsUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTimeSeriesUpdate(TransportProtos.TbSubUpdateProto tsUpdate, TbCallback callback);

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `update`：数据列表。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTimeSeriesUpdate(EntityId entityId, List<TsKvEntry> update, TbCallback callback);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `attrUpdate`：`attrUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAttributesUpdate(TransportProtos.TbSubUpdateProto attrUpdate, TbCallback callback);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `update`：数据列表。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAttributesUpdate(EntityId entityId, String scope, List<TsKvEntry> update, TbCallback callback);

    /**
     * 功能：处理告警。
     * 参数：
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `deleted`：`deleted` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAlarmUpdate(EntityId entityId, AlarmInfo alarm, boolean deleted, TbCallback callback);

    /**
     * 功能：处理告警。
     * 参数：
     * - `update`：`update` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onAlarmUpdate(TransportProtos.TbAlarmSubUpdateProto update, TbCallback callback);

    /**
     * 功能：处理通知。
     * 参数：
     * - `entityId`：实体IDID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onNotificationUpdate(EntityId entityId, NotificationsSubscriptionUpdate subscriptionUpdate, TbCallback callback);

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    void onApplicationEvent(ClusterTopologyChangeEvent event);

    /**
     * 功能：处理消息。
     * 参数：
     * - `coreStartupMsg`：待处理消息。
     * 返回：无。
     */
    void onCoreStartupMsg(TransportProtos.CoreStartupMsg coreStartupMsg);

    /**
     * 功能：处理请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `update`：`update` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onNotificationRequestUpdate(TenantId tenantId, NotificationRequestUpdate update, TbCallback callback);

    /**
     * 功能：处理通知。
     * 参数：
     * - `notificationsUpdate`：`notificationsUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onNotificationUpdate(TransportProtos.NotificationsSubUpdateProto notificationsUpdate, TbCallback callback);

}
