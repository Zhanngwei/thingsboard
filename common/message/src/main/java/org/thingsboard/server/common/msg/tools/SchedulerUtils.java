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
 * 1. `SchedulerUtils` 是 ThingsBoard Common Message 中处理 `Scheduler Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
