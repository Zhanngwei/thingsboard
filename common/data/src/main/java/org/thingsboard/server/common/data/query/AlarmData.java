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
package org.thingsboard.server.common.data.query;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `AlarmData` 是 ThingsBoard Common Data 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AlarmInfo`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
public class AlarmData extends AlarmInfo {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -7042457913823369638L;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Getter
    private final EntityId entityId;
    /**
     * `latest`映射关系，用于按键查找对应值。
     */
    @Getter
    private final Map<EntityKeyType, Map<String, TsValue>> latest;

    /**
     * 功能：创建 `AlarmData` 实例，并初始化必要字段。
     * 参数：
     * - `main`：`main` 参数。
     * - `prototype`：类型。
     * 返回：新创建的对象实例。
     */
    public AlarmData(AlarmInfo main, AlarmData prototype) {
        super(main);
        this.entityId = prototype.entityId;
        this.latest = new HashMap<>();
        this.latest.putAll(prototype.getLatest());
    }

    /**
     * 功能：创建 `AlarmData` 实例，并初始化必要字段。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `entityId`：实体IDID。
     * 返回：新创建的对象实例。
     */
    public AlarmData(Alarm alarm, EntityId entityId) {
        super(alarm);
        this.entityId = entityId;
        this.latest = new HashMap<>();
    }
}
