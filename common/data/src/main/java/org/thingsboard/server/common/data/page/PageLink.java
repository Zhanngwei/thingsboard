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
 * 1. 类目的：`PageLink` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`PageLink` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
