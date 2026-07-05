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
package org.thingsboard.server.common.data.alarm;

import java.util.Collection;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`AlarmStatusFilter` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public class AlarmStatusFilter {

    private static final AlarmStatusFilter EMPTY = new AlarmStatusFilter(Optional.empty(), Optional.empty());

    /**
     * 是否满足`clearFilter`条件。
     */
    private final Optional<Boolean> clearFilter;
    private final Optional<Boolean> ackFilter;

    /**
     * 功能：创建 `AlarmStatusFilter` 实例，并初始化必要字段。
     * 参数：
     * - `clearFilter`：`clearFilter` 参数。
     * - `ackFilter`：`ackFilter` 参数。
     * 返回：新创建的对象实例。
     */
    private AlarmStatusFilter(Optional<Boolean> clearFilter, Optional<Boolean> ackFilter) {
        this.clearFilter = clearFilter;
        this.ackFilter = ackFilter;
    }

    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(AlarmQuery query) {
        if (query.getSearchStatus() != null) {
            return AlarmStatusFilter.from(query.getSearchStatus());
        } else if (query.getStatus() != null) {
            return AlarmStatusFilter.from(query.getStatus());
        }
        return AlarmStatusFilter.empty();
    }

    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `alarmSearchStatus`：`alarmSearchStatus` 参数。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(AlarmSearchStatus alarmSearchStatus) {
        switch (alarmSearchStatus) {
            case ACK:
                return new AlarmStatusFilter(Optional.empty(), Optional.of(true));
            case UNACK:
                return new AlarmStatusFilter(Optional.empty(), Optional.of(false));
            case ACTIVE:
                return new AlarmStatusFilter(Optional.of(false), Optional.empty());
            case CLEARED:
                return new AlarmStatusFilter(Optional.of(true), Optional.empty());
            default:
                return EMPTY;
        }
    }

    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `alarmStatus`：`alarmStatus` 参数。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(AlarmStatus alarmStatus) {
        switch (alarmStatus) {
            case ACTIVE_UNACK:
                return new AlarmStatusFilter(Optional.of(false), Optional.of(false));
            case ACTIVE_ACK:
                return new AlarmStatusFilter(Optional.of(false), Optional.of(true));
            case CLEARED_UNACK:
                return new AlarmStatusFilter(Optional.of(true), Optional.of(false));
            case CLEARED_ACK:
                return new AlarmStatusFilter(Optional.of(true), Optional.of(true));
            default:
                return EMPTY;
        }
    }

    /**
     * 功能：执行 `empty` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter empty() {
        return EMPTY;
    }

    /**
     * 功能：判断`Any Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasAnyFilter() {
        return clearFilter.isPresent() || ackFilter.isPresent();
    }

    /**
     * 功能：判断`Clear Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasClearFilter() {
        return clearFilter.isPresent();
    }

    /**
     * 功能：判断`Ack Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasAckFilter() {
        return ackFilter.isPresent();
    }

    /**
     * 功能：获取`Clear Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean getClearFilter() {
        return clearFilter.orElseThrow(() -> new RuntimeException("Clear filter is not set! Use `hasClearFilter` to check."));
    }

    /**
     * 功能：获取`Ack Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean getAckFilter() {
        return ackFilter.orElseThrow(() -> new RuntimeException("Ack filter is not set! Use `hasAckFilter` to check."));
    }


    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `statuses`：数据列表。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(Collection<AlarmSearchStatus> statuses) {
        if (statuses == null || statuses.isEmpty() || statuses.contains(AlarmSearchStatus.ANY)) {
            return EMPTY;
        }
        boolean clearFilter = statuses.contains(AlarmSearchStatus.CLEARED);
        boolean activeFilter = statuses.contains(AlarmSearchStatus.ACTIVE);
        Optional<Boolean> clear = Optional.empty();
        if (clearFilter && !activeFilter || !clearFilter && activeFilter) {
            clear = Optional.of(clearFilter);
        }

        boolean ackFilter = statuses.contains(AlarmSearchStatus.ACK);
        boolean unackFilter = statuses.contains(AlarmSearchStatus.UNACK);
        Optional<Boolean> ack = Optional.empty();
        if (ackFilter && !unackFilter || !ackFilter && unackFilter) {
            ack = Optional.of(ackFilter);
        }
        return new AlarmStatusFilter(clear, ack);
    }

    /**
     * 功能：执行 `matches` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：判断结果。
     */
    public boolean matches(Alarm alarm) {
        return ackFilter.map(ackFilter -> ackFilter.equals(alarm.isAcknowledged())).orElse(true) &&
                clearFilter.map(clearedFilter -> clearedFilter.equals(alarm.isCleared())).orElse(true);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmStatusFilter` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
