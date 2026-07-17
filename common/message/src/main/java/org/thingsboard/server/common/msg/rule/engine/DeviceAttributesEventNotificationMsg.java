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
package org.thingsboard.server.common.msg.rule.engine;

import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `DeviceAttributesEventNotificationMsg` 是 ThingsBoard Common Message 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `ToDeviceActorNotificationMsg`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class DeviceAttributesEventNotificationMsg implements ToDeviceActorNotificationMsg {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2422071590415277039L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final DeviceId deviceId;
    /**
     * `deletedKeys`集合，用于去重保存或快速判断对象是否存在。
     */
    private final Set<AttributeKey> deletedKeys;
    private final String scope;
    /**
     * `values`列表，用于保存一组待处理对象。
     */
    private final List<AttributeKvEntry> values;
    private final boolean deleted;

    /**
     * 功能：处理`on Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `values`：值。
     * 返回：处理结果。
     */
    public static DeviceAttributesEventNotificationMsg onUpdate(TenantId tenantId, DeviceId deviceId, String scope, List<AttributeKvEntry> values) {
        return new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, scope, values, false);
    }

    /**
     * 功能：处理`on Delete`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：处理结果。
     */
    public static DeviceAttributesEventNotificationMsg onDelete(TenantId tenantId, DeviceId deviceId, String scope, List<String> keys) {
        Set<AttributeKey> keysToNotify = new HashSet<>();
        keys.forEach(key -> keysToNotify.add(new AttributeKey(scope, key)));
        return new DeviceAttributesEventNotificationMsg(tenantId, deviceId, keysToNotify, null, null, true);
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG;
    }
}
