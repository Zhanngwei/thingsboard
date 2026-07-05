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
package org.thingsboard.server.common.data.kv;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`AggregationParams` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@AllArgsConstructor
@EqualsAndHashCode
@Slf4j
public class AggregationParams {
    private static final Map<String, String> TZ_LINKS = Map.of("EST", "America/New_York", "GMT+0", "GMT", "GMT-0", "GMT", "HST", "US/Hawaii", "MST", "America/Phoenix", "ROC", "Asia/Taipei");
    /**
     * `aggregation` 字段，保存当前对象的对应属性。
     */
    @Getter
    private final Aggregation aggregation;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private final IntervalType intervalType;
    /**
     * `tzId`ID，用于定位对应业务对象。
     */
    @Getter
    private final ZoneId tzId;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private final long interval;

    /**
     * 功能：执行 `none` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static AggregationParams none() {
        return new AggregationParams(Aggregation.NONE, null, null, 0L);
    }

    /**
     * 功能：执行 `milliseconds` 对应的处理。
     * 参数：
     * - `aggregationType`：类型。
     * - `aggregationIntervalMs`：`aggregationIntervalMs` 参数。
     * 返回：处理结果。
     */
    public static AggregationParams milliseconds(Aggregation aggregationType, long aggregationIntervalMs) {
        return new AggregationParams(aggregationType, IntervalType.MILLISECONDS, null, aggregationIntervalMs);
    }

    /**
     * 功能：执行 `calendar` 对应的处理。
     * 参数：
     * - `aggregationType`：类型。
     * - `intervalType`：类型。
     * - `tzIdStr`：`tzIdStr` 参数。
     * 返回：处理结果。
     */
    public static AggregationParams calendar(Aggregation aggregationType, IntervalType intervalType, String tzIdStr) {
        return calendar(aggregationType, intervalType, getZoneId(tzIdStr));
    }

    /**
     * 功能：执行 `calendar` 对应的处理。
     * 参数：
     * - `aggregationType`：类型。
     * - `intervalType`：类型。
     * - `tzId`：`tzId`ID。
     * 返回：处理结果。
     */
    public static AggregationParams calendar(Aggregation aggregationType, IntervalType intervalType, ZoneId tzId) {
        return new AggregationParams(aggregationType, intervalType, tzId, 0L);
    }

    /**
     * 功能：执行 `of` 对应的处理。
     * 参数：
     * - `aggregation`：`aggregation` 参数。
     * - `intervalType`：类型。
     * - `tzId`：`tzId`ID。
     * - `interval`：`interval` 参数。
     * 返回：处理结果。
     */
    public static AggregationParams of(Aggregation aggregation, IntervalType intervalType, ZoneId tzId, long interval) {
        return new AggregationParams(aggregation, intervalType, tzId, interval);
    }

    /**
     * 功能：获取时间间隔。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getInterval() {
        if (intervalType == null) {
            return 0L;
        } else {
            switch (intervalType) {
                case WEEK:
                case WEEK_ISO:
                    return TimeUnit.DAYS.toMillis(7);
                case MONTH:
                    return TimeUnit.DAYS.toMillis(30);
                case QUARTER:
                    return TimeUnit.DAYS.toMillis(90);
                default:
                    return interval;
            }
        }
    }

    /**
     * 功能：获取`Zone Id`。
     * 参数：
     * - `tzIdStr`：`tzIdStr` 参数。
     * 返回：处理结果。
     */
    private static ZoneId getZoneId(String tzIdStr) {
        if (StringUtils.isEmpty(tzIdStr)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(tzIdStr, TZ_LINKS);
        } catch (DateTimeException e) {
            log.warn("[{}] Failed to convert the time zone. Fallback to default.", tzIdStr);
            return ZoneId.systemDefault();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AggregationParams` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
