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
 * 1. 类目的：`GetTsCmd` 是ThingsBoard Application 模块中的WebSocket 服务类型，用于维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 生命周期：随 WebSocket 建连创建订阅，断连或取消订阅时释放。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / Session。
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

/*
 * 本类总结：
 * 1. 核心职责：`GetTsCmd` 在 ThingsBoard Application 模块 中承担WebSocket 服务类型职责，核心目的是维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 核心流程：接收订阅请求后注册监听，数据变化时推送到客户端。
 * 3. 关键依赖：主要依赖或协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
