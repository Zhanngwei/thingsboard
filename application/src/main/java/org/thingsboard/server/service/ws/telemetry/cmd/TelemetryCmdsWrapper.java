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
package org.thingsboard.server.service.ws.telemetry.cmd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.service.ws.WsCommandsWrapper;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.GetHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.TimeseriesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUnsubscribeCmd;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @deprecated Use {@link WsCommandsWrapper}. This class is left for backward compatibility
 * */
/**
 * 中文说明：
 * 1. 类目的：`TelemetryCmdsWrapper` 是ThingsBoard Application 模块中的WebSocket 服务类型，用于维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 生命周期：随 WebSocket 建连创建订阅，断连或取消订阅时释放。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / Session。
 */
@Data
@Deprecated
public class TelemetryCmdsWrapper {

    /**
     * `attrSubCmds`列表，用于保存一组待处理对象。
     */
    private List<AttributesSubscriptionCmd> attrSubCmds;

    /**
     * 时间戳列表，用于保存一组待处理对象。
     */
    private List<TimeseriesSubscriptionCmd> tsSubCmds;

    /**
     * `historyCmds`列表，用于保存一组待处理对象。
     */
    private List<GetHistoryCmd> historyCmds;

    /**
     * 实体列表，用于保存一组待处理对象。
     */
    private List<EntityDataCmd> entityDataCmds;

    /**
     * 实体列表，用于保存一组待处理对象。
     */
    private List<EntityDataUnsubscribeCmd> entityDataUnsubscribeCmds;

    /**
     * 告警列表，用于保存一组待处理对象。
     */
    private List<AlarmDataCmd> alarmDataCmds;

    /**
     * 告警列表，用于保存一组待处理对象。
     */
    private List<AlarmDataUnsubscribeCmd> alarmDataUnsubscribeCmds;

    /**
     * 实体列表，用于保存一组待处理对象。
     */
    private List<EntityCountCmd> entityCountCmds;

    /**
     * 实体列表，用于保存一组待处理对象。
     */
    private List<EntityCountUnsubscribeCmd> entityCountUnsubscribeCmds;

    /**
     * 告警列表，用于保存一组待处理对象。
     */
    private List<AlarmCountCmd> alarmCountCmds;

    /**
     * 告警列表，用于保存一组待处理对象。
     */
    private List<AlarmCountUnsubscribeCmd> alarmCountUnsubscribeCmds;

    /**
     * 功能：执行 `toCommonCmdsWrapper` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public WsCommandsWrapper toCommonCmdsWrapper() {
        return new WsCommandsWrapper(null, Stream.of(
                        attrSubCmds, tsSubCmds, historyCmds, entityDataCmds,
                        entityDataUnsubscribeCmds, alarmDataCmds, alarmDataUnsubscribeCmds,
                        entityCountCmds, entityCountUnsubscribeCmds,
                        alarmCountCmds, alarmCountUnsubscribeCmds
                )
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TelemetryCmdsWrapper` 在 ThingsBoard Application 模块 中承担WebSocket 服务类型职责，核心目的是维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 核心流程：接收订阅请求后注册监听，数据变化时推送到客户端。
 * 3. 关键依赖：主要依赖或协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
