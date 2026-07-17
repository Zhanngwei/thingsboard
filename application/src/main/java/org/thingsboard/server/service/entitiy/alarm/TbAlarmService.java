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
package org.thingsboard.server.service.entitiy.alarm;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;

/**
 * 中文说明：
 * 1. `TbAlarmService` 是 ThingsBoard Application 中定义告警能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbAlarmService {

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Alarm save(Alarm entity, User user) throws ThingsboardException;

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    AlarmInfo ack(Alarm alarm, User user) throws ThingsboardException;

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `ackTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    AlarmInfo ack(Alarm alarm, long ackTs, User user) throws ThingsboardException;

    /**
     * 功能：执行 `clear` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    AlarmInfo clear(Alarm alarm, User user) throws ThingsboardException;

    /**
     * 功能：执行 `clear` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `clearTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    AlarmInfo clear(Alarm alarm, long clearTs, User user) throws ThingsboardException;

    /**
     * 功能：执行 `assign` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `assigneeId`：`assigneeId`ID。
     * - `assignTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    AlarmInfo assign(Alarm alarm, UserId assigneeId, long assignTs, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassign` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `unassignTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    AlarmInfo unassign(Alarm alarm, long unassignTs, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignDeletedUserAlarms` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * - `unassignTs`：时间戳。
     * 返回：匹配的数据集合。
     */
    List<AlarmId> unassignDeletedUserAlarms(TenantId tenantId, User user, long unassignTs);

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * 返回：判断结果。
     */
    Boolean delete(Alarm alarm, User user);
}
