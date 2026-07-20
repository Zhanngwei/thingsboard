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
package org.thingsboard.server.dao.util;

import org.thingsboard.server.common.data.kv.IntervalType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;

/**
 * 中文说明：
 * 1. `TimeUtils` 是 ThingsBoard DAO 中处理 `Time Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class TimeUtils {

    /**
     * 功能：执行 `calculateIntervalEnd` 对应的处理。
     * 参数：
     * - `startTs`：时间戳。
     * - `intervalType`：类型。
     * - `tzId`：`tzId`ID。
     * 返回：数值结果。
     */
    public static long calculateIntervalEnd(long startTs, IntervalType intervalType, ZoneId tzId) {
        var startTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTs), tzId);
        switch (intervalType) {
            case WEEK:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(WeekFields.SUNDAY_START.dayOfWeek(), 1).plusDays(7).toInstant().toEpochMilli();
            case WEEK_ISO:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(WeekFields.ISO.dayOfWeek(), 1).plusDays(7).toInstant().toEpochMilli();
            case MONTH:
                return startTime.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).plusMonths(1).toInstant().toEpochMilli();
            case QUARTER:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(3).toInstant().toEpochMilli();
            default:
                throw new RuntimeException("Not supported!");
        }
    }

}
