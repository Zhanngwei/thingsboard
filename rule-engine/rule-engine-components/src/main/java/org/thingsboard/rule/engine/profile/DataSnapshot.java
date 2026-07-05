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
package org.thingsboard.rule.engine.profile;

import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：`DataSnapshot` 是数据快照辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
class DataSnapshot {

    /**
     * 当前对象是否已准备就绪。
     */
    private volatile boolean ready;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Getter
    @Setter
    private long ts;
    /**
     * `keys`集合，用于去重保存或快速判断对象是否存在。
     */
    private final Set<AlarmConditionFilterKey> keys;
    private final Map<AlarmConditionFilterKey, EntityKeyValue> values = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `DataSnapshot` 实例，并初始化必要字段。
     * 参数：
     * - `entityKeysToFetch`：实体对象。
     * 返回：新创建的对象实例。
     */
    DataSnapshot(Set<AlarmConditionFilterKey> entityKeysToFetch) {
        this.keys = entityKeysToFetch;
    }

    /**
     * 功能：执行 `toConditionKey` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    static AlarmConditionFilterKey toConditionKey(EntityKey key) {
        return new AlarmConditionFilterKey(toConditionKeyType(key.getType()), key.getKey());
    }

    /**
     * 功能：执行 `toConditionKeyType` 对应的处理。
     * 参数：
     * - `keyType`：类型。
     * 返回：处理结果。
     */
    static AlarmConditionKeyType toConditionKeyType(EntityKeyType keyType) {
        switch (keyType) {
            case ATTRIBUTE:
            case SERVER_ATTRIBUTE:
            case SHARED_ATTRIBUTE:
            case CLIENT_ATTRIBUTE:
                return AlarmConditionKeyType.ATTRIBUTE;
            case TIME_SERIES:
                return AlarmConditionKeyType.TIME_SERIES;
            case ENTITY_FIELD:
                return AlarmConditionKeyType.ENTITY_FIELD;
            default:
                throw new RuntimeException("Not supported entity key: " + keyType.name());
        }
    }

    /**
     * 功能：删除或清理值。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    void removeValue(EntityKey key) {
        values.remove(toConditionKey(key));
    }

    /**
     * 功能：执行 `putValue` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `newTs`：时间戳。
     * - `value`：值。
     * 返回：判断结果。
     */
    boolean putValue(AlarmConditionFilterKey key, long newTs, EntityKeyValue value) {
        return putIfKeyExists(key, value, ts != newTs);
    }

    /**
     * 功能：执行 `putIfKeyExists` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * - `updateOfTs`：时间戳。
     * 返回：判断结果。
     */
    private boolean putIfKeyExists(AlarmConditionFilterKey key, EntityKeyValue value, boolean updateOfTs) {
        if (keys.contains(key)) {
            EntityKeyValue oldValue = values.put(key, value);
            if (updateOfTs) {
                return true;
            } else {
                return oldValue == null || !oldValue.equals(value);
            }
        } else {
            return false;
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    EntityKeyValue getValue(AlarmConditionFilterKey key) {
        return values.get(key);
    }
    /*
     * 本类总结：`DataSnapshot` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
