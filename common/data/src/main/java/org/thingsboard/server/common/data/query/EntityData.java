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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

/**
 * 中文说明：
 * 1. `EntityData` 是 ThingsBoard Common Data 中承载实体信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@RequiredArgsConstructor
public class EntityData {

    /**
     * 实体ID，用于定位对应业务对象。
     */
    private final EntityId entityId;
    private final Map<EntityKeyType, Map<String, TsValue>> latest;
    /**
     * 时序数据列表，用于保存一组待处理对象。
     */
    private final Map<String, TsValue[]> timeseries;
    private final Map<Integer, ComparisonTsValue> aggLatest;

    /**
     * 功能：创建 `EntityData` 实例，并初始化必要字段。
     * 参数：
     * - `entityId`：实体IDID。
     * - `latest`：键值映射。
     * - `timeseries`：键值映射。
     * 返回：新创建的对象实例。
     */
    public EntityData(EntityId entityId, Map<EntityKeyType, Map<String, TsValue>> latest, Map<String, TsValue[]> timeseries) {
        this(entityId, latest, timeseries, null);
    }

    /**
     * 功能：删除或清理时间戳。
     * 参数：无。
     * 返回：无。
     */
    @JsonIgnore
    public void clearTsAndAggData() {
        if (timeseries != null) {
            timeseries.clear();
        }
        if (aggLatest != null) {
            aggLatest.clear();
        }
    }
}
