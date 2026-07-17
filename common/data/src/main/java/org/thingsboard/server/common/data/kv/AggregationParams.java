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
 * 1. `AggregationParams` 是 ThingsBoard Common Data 中承载 `Aggregation Params` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
