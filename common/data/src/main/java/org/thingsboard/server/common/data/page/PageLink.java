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
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `PageLink` 是 ThingsBoard Common Data 中承载 `Page Link` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class PageLink {

    /**
     * `DEFAULT_SORT_PROPERTY`常量，用于统一引用固定值。
     */
    protected static final String DEFAULT_SORT_PROPERTY = "id";
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, DEFAULT_SORT_PROPERTY);

    /**
     * 搜索文本，表示当前对象的对应属性。
     */
    private final String textSearch;
    private final int pageSize;
    /**
     * `page` 字段，保存当前对象的对应属性。
     */
    private final int page;
    private final SortOrder sortOrder;

    /**
     * 功能：创建 `PageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：新创建的对象实例。
     */
    public PageLink(PageLink pageLink) {
        this.pageSize = pageLink.getPageSize();
        this.page = pageLink.getPage();
        this.textSearch = pageLink.getTextSearch();
        this.sortOrder = pageLink.getSortOrder();
    }

    /**
     * 功能：创建 `PageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * 返回：新创建的对象实例。
     */
    public PageLink(int pageSize) {
        this(pageSize, 0);
    }

    /**
     * 功能：创建 `PageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * 返回：新创建的对象实例。
     */
    public PageLink(int pageSize, int page) {
        this(pageSize, page, null, null);
    }

    /**
     * 功能：创建 `PageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * 返回：新创建的对象实例。
     */
    public PageLink(int pageSize, int page, String textSearch) {
        this(pageSize, page, textSearch, null);
    }

    /**
     * 功能：创建 `PageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `sortOrder`：`sortOrder` 参数。
     * 返回：新创建的对象实例。
     */
    public PageLink(int pageSize, int page, String textSearch, SortOrder sortOrder) {
        this.pageSize = pageSize;
        this.page = page;
        this.textSearch = textSearch;
        this.sortOrder = sortOrder;
    }

    /**
     * 功能：执行 `nextPageLink` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @JsonIgnore
    public PageLink nextPageLink() {
        return new PageLink(this.pageSize, this.page+1, this.textSearch, this.sortOrder);
    }

    /**
     * 功能：执行 `toSort` 对应的处理。
     * 参数：
     * - `sortOrder`：`sortOrder` 参数。
     * - `columnMap`：键值映射。
     * 返回：处理结果。
     */
    public Sort toSort(SortOrder sortOrder, Map<String,String> columnMap) {
        if (sortOrder == null) {
            return DEFAULT_SORT;
        } else {
            String property = sortOrder.getProperty();
            if (columnMap.containsKey(property)) {
                property = columnMap.get(property);
            }
            return Sort.by(Sort.Direction.fromString(sortOrder.getDirection().name()), property);
        }
    }

    /**
     * 功能：执行 `toSort` 对应的处理。
     * 参数：
     * - `sortOrders`：数据列表。
     * - `columnMap`：键值映射。
     * 返回：处理结果。
     */
    public Sort toSort(List<SortOrder> sortOrders, Map<String,String> columnMap) {
        return Sort.by(sortOrders.stream().map(s -> toSortOrder(s, columnMap)).collect(Collectors.toList()));
    }

    /**
     * 功能：执行 `toSortOrder` 对应的处理。
     * 参数：
     * - `sortOrder`：`sortOrder` 参数。
     * - `columnMap`：键值映射。
     * 返回：处理结果。
     */
    private Sort.Order toSortOrder(SortOrder sortOrder, Map<String,String> columnMap) {
        String property = sortOrder.getProperty();
        if (columnMap.containsKey(property)) {
            property = columnMap.get(property);
        }
        return new Sort.Order(Sort.Direction.fromString(sortOrder.getDirection().name()), property, Sort.NullHandling.NULLS_LAST);
    }

}
