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
package org.thingsboard.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：
 * 1. `TimePageLink` 是 ThingsBoard Common Data 中承载 `Time Page Link` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `PageLink`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TimePageLink extends PageLink {

    /**
     * 开始时间戳，用于限定查询或统计的起点。
     */
    private final Long startTime;
    private final Long endTime;

    /**
     * 功能：创建 `TimePageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * 返回：新创建的对象实例。
     */
    public TimePageLink(PageLink pageLink, Long startTime, Long endTime) {
        super(pageLink);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 功能：创建 `TimePageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * 返回：新创建的对象实例。
     */
    public TimePageLink(int pageSize) {
        this(pageSize, 0);
    }

    /**
     * 功能：创建 `TimePageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * 返回：新创建的对象实例。
     */
    public TimePageLink(int pageSize, int page) {
        this(pageSize, page, null);
    }

    /**
     * 功能：创建 `TimePageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * 返回：新创建的对象实例。
     */
    public TimePageLink(int pageSize, int page, String textSearch) {
        this(pageSize, page, textSearch, null, null, null);
    }

    /**
     * 功能：创建 `TimePageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `sortOrder`：`sortOrder` 参数。
     * 返回：新创建的对象实例。
     */
    public TimePageLink(int pageSize, int page, String textSearch, SortOrder sortOrder) {
        this(pageSize, page, textSearch, sortOrder, null, null);
    }

    /**
     * 功能：创建 `TimePageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `sortOrder`：`sortOrder` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TimePageLink(int pageSize, int page, String textSearch, SortOrder sortOrder, Long startTime, Long endTime) {
        super(pageSize, page, textSearch, sortOrder);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 功能：执行 `nextPageLink` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @JsonIgnore
    public TimePageLink nextPageLink() {
        return new TimePageLink(this.getPageSize(), this.getPage()+1, this.getTextSearch(), this.getSortOrder(),
                this.startTime, this.endTime);
    }

    /**
     * 功能：执行 `toSort` 对应的处理。
     * 参数：
     * - `sortOrder`：`sortOrder` 参数。
     * - `columnMap`：键值映射。
     * 返回：处理结果。
     */
    @Override
    public Sort toSort(SortOrder sortOrder, Map<String,String> columnMap) {
        if (sortOrder == null) {
            return super.toSort(sortOrder, columnMap);
        } else {
            return toSort(new ArrayList<>(List.of(sortOrder)), columnMap);
        }
    }

    /**
     * 功能：执行 `toSort` 对应的处理。
     * 参数：
     * - `sortOrders`：数据列表。
     * - `columnMap`：键值映射。
     * 返回：处理结果。
     */
    @Override
    public Sort toSort(List<SortOrder> sortOrders, Map<String,String> columnMap) {
        if (!isDefaultSortOrderAvailable(sortOrders)) {
            sortOrders.add(new SortOrder(DEFAULT_SORT_PROPERTY, SortOrder.Direction.ASC));
        }
        return super.toSort(sortOrders, columnMap);
    }

    /**
     * 功能：判断排序规则。
     * 参数：
     * - `sortOrders`：数据列表。
     * 返回：判断结果。
     */
    private boolean isDefaultSortOrderAvailable(List<SortOrder> sortOrders) {
        for (SortOrder sortOrder : sortOrders) {
            if (DEFAULT_SORT_PROPERTY.equals(sortOrder.getProperty())) {
                return true;
            }
        }
        return false;
    }
}
