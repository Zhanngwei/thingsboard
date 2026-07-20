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

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. `EntityCountQuery` 是 ThingsBoard Common Data 中承载实体信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@ToString
public class EntityCountQuery {

    /**
     * 实体对象，用于描述当前业务场景。
     */
    @Getter
    private EntityFilter entityFilter;

    /**
     * 键列表，用于保存一组待处理对象。
     */
    @Getter
    protected List<KeyFilter> keyFilters;

    /**
     * 功能：创建 `EntityCountQuery` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EntityCountQuery() {
    }

    /**
     * 功能：创建 `EntityCountQuery` 实例，并初始化必要字段。
     * 参数：
     * - `entityFilter`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityCountQuery(EntityFilter entityFilter) {
        this(entityFilter, Collections.emptyList());
    }

    /**
     * 功能：创建 `EntityCountQuery` 实例，并初始化必要字段。
     * 参数：
     * - `entityFilter`：实体对象。
     * - `keyFilters`：键。
     * 返回：新创建的对象实例。
     */
    public EntityCountQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        this.entityFilter = entityFilter;
        this.keyFilters = keyFilters;
    }
}
