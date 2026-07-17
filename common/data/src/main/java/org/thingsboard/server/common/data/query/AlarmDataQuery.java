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
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 中文说明：
 * 1. `AlarmDataQuery` 是 ThingsBoard Common Data 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AbstractDataQuery`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString
public class AlarmDataQuery extends AbstractDataQuery<AlarmDataPageLink> {

    /**
     * 告警列表，用于保存一组待处理对象。
     */
    @Getter
    protected List<EntityKey> alarmFields;

    /**
     * 功能：创建 `AlarmDataQuery` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AlarmDataQuery() {
    }

    /**
     * 功能：创建 `AlarmDataQuery` 实例，并初始化必要字段。
     * 参数：
     * - `entityFilter`：实体对象。
     * - `keyFilters`：键。
     * 返回：新创建的对象实例。
     */
    public AlarmDataQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        super(entityFilter, keyFilters);
    }

    /**
     * 功能：创建 `AlarmDataQuery` 实例，并初始化必要字段。
     * 参数：
     * - `entityFilter`：实体对象。
     * - `pageLink`：`pageLink` 参数。
     * - `entityFields`：实体对象。
     * - `latestValues`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AlarmDataQuery(EntityFilter entityFilter, AlarmDataPageLink pageLink, List<EntityKey> entityFields, List<EntityKey> latestValues, List<KeyFilter> keyFilters, List<EntityKey> alarmFields) {
        super(entityFilter, pageLink, entityFields, latestValues, keyFilters);
        this.alarmFields = alarmFields;
    }

    /**
     * 功能：执行 `next` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public AlarmDataQuery next() {
        return new AlarmDataQuery(getEntityFilter(), getPageLink().nextPageLink(), entityFields, latestValues, keyFilters, alarmFields);
    }
}
