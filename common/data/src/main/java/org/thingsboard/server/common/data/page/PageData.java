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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `PageData` 是 ThingsBoard Common Data 中承载 `Page` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@EqualsAndHashCode
public class PageData<T> implements Serializable {

    public static final PageData EMPTY_PAGE_DATA = new PageData<>();

    /**
     * 数据列表，用于保存一组待处理对象。
     */
    private final List<T> data;
    private final int totalPages;
    /**
     * `totalElements` 字段，保存当前对象的对应属性。
     */
    private final long totalElements;
    private final boolean hasNext;

    /**
     * 功能：创建 `PageData` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public PageData() {
        this(Collections.emptyList(), 0, 0, false);
    }

    /**
     * 功能：创建 `PageData` 实例，并初始化必要字段。
     * 参数：
     * - `data`：待处理数据。
     * - `totalPages`：`totalPages` 参数。
     * - `totalElements`：`totalElements` 参数。
     * - `hasNext`：`hasNext` 参数。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public PageData(@JsonProperty("data") List<T> data,
                    @JsonProperty("totalPages") int totalPages,
                    @JsonProperty("totalElements") long totalElements,
                    @JsonProperty("hasNext") boolean hasNext) {
        this.data = data;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    /**
     * 功能：执行 `emptyPageData` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public static <T> PageData<T> emptyPageData() {
        return (PageData<T>) EMPTY_PAGE_DATA;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiModelProperty(position = 1, value = "Array of the entities", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public List<T> getData() {
        return data;
    }

    /**
     * 功能：获取`Total Pages`。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Total number of available pages. Calculated based on the 'pageSize' request parameter and total number of entities that match search criteria", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 功能：获取`Total Elements`。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 3, value = "Total number of elements in all available pages", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * 功能：判断`Next`。
     * 参数：无。
     * 返回：判断结果。
     */
    @ApiModelProperty(position = 4, value = "'false' value indicates the end of the result set", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * 功能：转换数据。
     * 参数：
     * - `mapper`：`mapper` 参数。
     * 返回：匹配的数据集合。
     */
    public <D> PageData<D> mapData(Function<T, D> mapper) {
        return new PageData<>(getData().stream().map(mapper).collect(Collectors.toList()), getTotalPages(), getTotalElements(), hasNext());
    }

}
