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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`AlarmDataPageLink` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@AllArgsConstructor
public class AlarmDataPageLink extends EntityDataPageLink {

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long startTs;
    private long endTs;
    //TODO: handle this;
    /**
     * 时间窗口，用于控制时间范围或等待时长。
     */
    private long timeWindow;
    private List<String> typeList;
    /**
     * 状态列表，用于保存一组待处理对象。
     */
    private List<AlarmSearchStatus> statusList;
    private List<AlarmSeverity> severityList;
    /**
     * 是否满足`searchPropagatedAlarms`条件。
     */
    private boolean searchPropagatedAlarms;
    private UserId assigneeId;

    /**
     * 功能：创建 `AlarmDataPageLink` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AlarmDataPageLink() {
        super();
    }

    /**
     * 功能：创建 `AlarmDataPageLink` 实例，并初始化必要字段。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `sortOrder`：`sortOrder` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AlarmDataPageLink(int pageSize, int page, String textSearch, EntityDataSortOrder sortOrder, boolean dynamic,
                             boolean searchPropagatedAlarms,
                             long startTs, long endTs, long timeWindow,
                             List<String> typeList, List<AlarmSearchStatus> statusList, List<AlarmSeverity> severityList,
                             UserId assigneeId) {
        super(pageSize, page, textSearch, sortOrder, dynamic);
        this.searchPropagatedAlarms = searchPropagatedAlarms;
        this.startTs = startTs;
        this.endTs = endTs;
        this.timeWindow = timeWindow;
        this.typeList = typeList;
        this.statusList = statusList;
        this.severityList = severityList;
        this.assigneeId = assigneeId;
    }

    /**
     * 功能：执行 `nextPageLink` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @JsonIgnore
    public AlarmDataPageLink nextPageLink() {
        return new AlarmDataPageLink(this.getPageSize(), this.getPage() + 1, this.getTextSearch(), this.getSortOrder(), this.isDynamic(),
                this.searchPropagatedAlarms,
                this.startTs, this.endTs, this.timeWindow,
                this.typeList, this.statusList, this.severityList,
                this.assigneeId
        );
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmDataPageLink` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
