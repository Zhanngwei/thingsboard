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
package org.thingsboard.server.service.ws.telemetry.sub;

import lombok.AllArgsConstructor;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`TelemetrySubscriptionUpdate` 是ThingsBoard Application 模块中的WebSocket 服务类型，用于维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 生命周期：随 WebSocket 建连创建订阅，断连或取消订阅时释放。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / Session。
 */
@AllArgsConstructor
public class TelemetrySubscriptionUpdate {
    /**
     * 订阅ID，用于定位对应业务对象。
     */
    private final int subscriptionId;
    private int errorCode;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private String errorMsg;
    private Map<String, List<Object>> data;

    /**
     * 功能：创建 `TelemetrySubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `subscriptionId`：订阅ID。
     * - `data`：待处理数据。
     * 返回：新创建的对象实例。
     */
    public TelemetrySubscriptionUpdate(int subscriptionId, List<TsKvEntry> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = new TreeMap<>();
        if (data != null) {
            for (TsKvEntry tsEntry : data) {
                List<Object> values = this.data.computeIfAbsent(tsEntry.getKey(), k -> new ArrayList<>());
                Object[] value = new Object[2];
                value[0] = tsEntry.getTs();
                value[1] = tsEntry.getValueAsString();
                values.add(value);
            }
        }
    }

    /**
     * 功能：创建 `TelemetrySubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `subscriptionId`：订阅ID。
     * - `data`：待处理数据。
     * 返回：新创建的对象实例。
     */
    public TelemetrySubscriptionUpdate(int subscriptionId, Map<String, List<Object>> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = data;
    }

    /**
     * 功能：创建 `TelemetrySubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `subscriptionId`：订阅ID。
     * - `errorCode`：错误信息。
     * 返回：新创建的对象实例。
     */
    public TelemetrySubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode) {
        this(subscriptionId, errorCode, null);
    }

    /**
     * 功能：创建 `TelemetrySubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `subscriptionId`：订阅ID。
     * - `errorCode`：错误信息。
     * - `errorMsg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public TelemetrySubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode, String errorMsg) {
        super();
        this.subscriptionId = subscriptionId;
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }

    /**
     * 功能：获取订阅。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Map<String, List<Object>> getData() {
        return data;
    }

    /**
     * 功能：获取`Latest Values`。
     * 参数：无。
     * 返回：处理结果。
     */
    public Map<String, Long> getLatestValues() {
        if (data == null) {
            return Collections.emptyMap();
        } else {
            return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                List<Object> data = e.getValue();
                Object[] latest = (Object[]) data.get(data.size() - 1);
                return (long) latest[0];
            }));
        }
    }

    /**
     * 功能：获取错误码。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * 功能：执行 `copyWithNewSubscriptionId` 对应的处理。
     * 参数：
     * - `subscriptionId`：订阅ID。
     * 返回：处理结果。
     */
    public TelemetrySubscriptionUpdate copyWithNewSubscriptionId(int subscriptionId){
        return new TelemetrySubscriptionUpdate(subscriptionId, errorCode, errorMsg, data);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("TelemetrySubscriptionUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", data=");
        data.forEach((k, v) -> {
            result.append(k).append("=[");
            for(Object a : v){
                result.append(Arrays.toString((Object[])a)).append("|");
            }
            result.append("]");
        });
        return result.toString();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TelemetrySubscriptionUpdate` 在 ThingsBoard Application 模块 中承担WebSocket 服务类型职责，核心目的是维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 核心流程：接收订阅请求后注册监听，数据变化时推送到客户端。
 * 3. 关键依赖：主要依赖或协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
