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
package org.thingsboard.server.common.msg.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoUnit.MONTHS;

/**
 * 中文说明：
 * 1. 类目的：`SchedulerUtils` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SchedulerUtils {

    private static final ConcurrentMap<String, ZoneId> tzMap = new ConcurrentHashMap<>();

    /**
     * 功能：获取`Zone Id`。
     * 参数：
     * - `tz`：`tz` 参数。
     * 返回：处理结果。
     */
    public static ZoneId getZoneId(String tz) {
        return tzMap.computeIfAbsent(tz == null || tz.isEmpty() ? "UTC" : tz, ZoneId::of);
    }

    /**
     * 功能：获取`Start Of Current Hour`。
     * 参数：无。
     * 返回：数值结果。
     */
    public static long getStartOfCurrentHour() {
        return getStartOfCurrentHour(UTC);
    }

    /**
     * 功能：获取`Start Of Current Hour`。
     * 参数：
     * - `zoneId`：`zoneId`ID。
     * 返回：数值结果。
     */
    public static long getStartOfCurrentHour(ZoneId zoneId) {
        return LocalDateTime.now(UTC).atZone(zoneId).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
    }

    /**
     * 功能：获取`Start Of Current Month`。
     * 参数：无。
     * 返回：数值结果。
     */
    public static long getStartOfCurrentMonth() {
        return getStartOfCurrentMonth(UTC);
    }

    /**
     * 功能：获取`Start Of Current Month`。
     * 参数：
     * - `zoneId`：`zoneId`ID。
     * 返回：数值结果。
     */
    public static long getStartOfCurrentMonth(ZoneId zoneId) {
        return LocalDate.now(UTC).withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 功能：获取`Start Of Next Month`。
     * 参数：无。
     * 返回：数值结果。
     */
    public static long getStartOfNextMonth() {
        return getStartOfNextMonth(UTC);
    }

    /**
     * 功能：获取`Start Of Next Month`。
     * 参数：
     * - `zoneId`：`zoneId`ID。
     * 返回：数值结果。
     */
    public static long getStartOfNextMonth(ZoneId zoneId) {
        return LocalDate.now(UTC).with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 功能：获取`Start Of Next Next Month`。
     * 参数：无。
     * 返回：数值结果。
     */
    public static long getStartOfNextNextMonth() {
        return getStartOfNextNextMonth(UTC);
    }

    /**
     * 功能：获取`Start Of Next Next Month`。
     * 参数：
     * - `zoneId`：`zoneId`ID。
     * 返回：数值结果。
     */
    public static long getStartOfNextNextMonth(ZoneId zoneId) {
        return LocalDate.now(UTC).with(firstDayOfNextNextMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 功能：执行 `firstDayOfNextNextMonth` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static TemporalAdjuster firstDayOfNextNextMonth() {
        return (temporal) -> temporal.with(DAY_OF_MONTH, 1).plus(2, MONTHS);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SchedulerUtils` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
