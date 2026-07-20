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
package org.thingsboard.server.dao.relation;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `RelationCacheKey` 是 ThingsBoard DAO 中管理实体关系缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
@Builder
public class RelationCacheKey implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 3911151843961657570L;

    /**
     * `from` 字段，保存当前对象的对应属性。
     */
    private final EntityId from;
    private final EntityId to;
    /**
     * 类型，用于区分不同处理分支。
     */
    private final String type;
    private final RelationTypeGroup typeGroup;
    /**
     * `direction` 字段，保存当前对象的对应属性。
     */
    private final EntitySearchDirection direction;

    /**
     * 功能：创建 `RelationCacheKey` 实例，并初始化必要字段。
     * 参数：
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `type`：类型。
     * - `typeGroup`：类型。
     * 返回：新创建的对象实例。
     */
    public RelationCacheKey(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        this(from, to, type, typeGroup, null);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = add(sb, true, from);
        first = add(sb, first, to);
        first = add(sb, first, type);
        first = add(sb, first, typeGroup);
        add(sb, first, direction);
        return sb.toString();
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `sb`：`sb` 参数。
     * - `first`：`first` 参数。
     * - `param`：`param` 参数。
     * 返回：判断结果。
     */
    private boolean add(StringBuilder sb, boolean first, Object param) {
        if (param != null) {
            if (!first) {
                sb.append("_");
            }
            first = false;
            sb.append(param);
        }
        return first;
    }

}
