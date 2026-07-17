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
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AggregationParams;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.util.List;

/**
 * 中文说明：
 * 1. `GetTsCmd` 是 ThingsBoard Application 中定义 `Get Ts Cmd` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface GetTsCmd {

    /**
     * 功能：获取开始时间戳。
     * 参数：无。
     * 返回：数值结果。
     */
    long getStartTs();

    /**
     * 功能：获取结束时间戳。
     * 参数：无。
     * 返回：数值结果。
     */
    long getEndTs();

    /**
     * 功能：获取`Keys`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    List<String> getKeys();

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    IntervalType getIntervalType();

    /**
     * 功能：获取时间间隔。
     * 参数：无。
     * 返回：数值结果。
     */
    long getInterval();

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：文本结果。
     */
    String getTimeZoneId();

    /**
     * 功能：获取数量限制。
     * 参数：无。
     * 返回：数值结果。
     */
    int getLimit();

    /**
     * 功能：获取`Agg`。
     * 参数：无。
     * 返回：处理结果。
     */
    Aggregation getAgg();

    /**
     * 功能：判断`Fetch Latest Previous Point`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isFetchLatestPreviousPoint();

    /**
     * 功能：执行 `toAggregationParams` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    default AggregationParams toAggregationParams() {
        var agg = getAgg();
        var intervalType = getIntervalType();
        if (agg == null || Aggregation.NONE.equals(agg)) {
            return AggregationParams.none();
        } else if (intervalType == null || IntervalType.MILLISECONDS.equals(intervalType)) {
            return AggregationParams.milliseconds(agg, getInterval());
        } else {
            return AggregationParams.calendar(agg, intervalType, getTimeZoneId());
        }
    }

}
