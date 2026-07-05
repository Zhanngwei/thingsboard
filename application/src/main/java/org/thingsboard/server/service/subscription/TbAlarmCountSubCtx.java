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
package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;

/**
 * 中文说明：
 * 1. 类目的：`TbAlarmCountSubCtx` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@ToString(callSuper = true)
public class TbAlarmCountSubCtx extends TbAbstractSubCtx<AlarmCountQuery> {

    /**
     * 告警，提供当前类调用的业务操作。
     */
    private final AlarmService alarmService;

    /**
     * `result` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Setter
    private volatile int result;

    /**
     * 功能：创建 `TbAlarmCountSubCtx` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `wsService`：服务对象。
     * - `entityService`：服务对象。
     * - `localSubscriptionService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbAlarmCountSubCtx(String serviceId, WebSocketService wsService,
                              EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                              AttributesService attributesService, SubscriptionServiceStatistics stats, AlarmService alarmService,
                              WebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.alarmService = alarmService;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void fetchData() {
        result = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), query);
        sendWsMsg(new AlarmCountUpdate(cmdId, result));
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void update() {
        int newCount = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), query);
        if (newCount != result) {
            result = newCount;
            sendWsMsg(new AlarmCountUpdate(cmdId, result));
        }
    }

    /**
     * 功能：判断`Dynamic`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isDynamic() {
        return true;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbAlarmCountSubCtx` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
