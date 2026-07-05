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
 * 1. 类目的：`PageData` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`PageData` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
