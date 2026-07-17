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
package org.thingsboard.rule.engine.data;

import lombok.Data;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.List;

/**
 * `RelationsQuery` 类，封装当前模块中的一组相关职责。
 */
@Data
public class RelationsQuery {

    /**
     * `direction` 字段，保存当前对象的对应属性。
     */
    private EntitySearchDirection direction;
    /**
     * `maxLevel` 字段，保存当前对象的对应属性。
     */
    private int maxLevel = 1;
    /**
     * `filters`列表，用于保存一组待处理对象。
     */
    private List<RelationEntityTypeFilter> filters;
    /**
     * 是否满足`fetchLastLevelOnly`条件。
     */
    private boolean fetchLastLevelOnly = false;
}
