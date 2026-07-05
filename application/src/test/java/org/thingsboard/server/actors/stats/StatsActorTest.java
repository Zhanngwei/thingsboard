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
package org.thingsboard.server.actors.stats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. 类目的：`StatsActorTest` 是ThingsBoard Application 测试模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
class StatsActorTest {

    /**
     * Actor 实例，表示当前对象的对应属性。
     */
    StatsActor statsActor;
    ActorSystemContext actorSystemContext;
    /**
     * 事件，提供当前类调用的业务操作。
     */
    EventService eventService;
    TbServiceInfoProvider serviceInfoProvider;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        actorSystemContext = mock(ActorSystemContext.class);

        eventService = mock(EventService.class);
        willReturn(eventService).given(actorSystemContext).getEventService();
        serviceInfoProvider = mock(TbServiceInfoProvider.class);
        willReturn(serviceInfoProvider).given(actorSystemContext).getServiceInfoProvider();

        statsActor = new StatsActor(actorSystemContext);
    }

    /**
     * 功能：验证 `givenEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction() {
        StatsPersistMsg emptyStats = new StatsPersistMsg(0, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
        statsActor.onStatsPersistMsg(emptyStats);
        verify(actorSystemContext, never()).getEventService();
    }

    /**
     * 功能：验证 `givenNonEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNonEmptyStatMessage_whenOnStatsPersistMsg_thenNoAction() {
        statsActor.onStatsPersistMsg(new StatsPersistMsg(0, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(1)).saveAsync(any(Event.class));
        statsActor.onStatsPersistMsg(new StatsPersistMsg(1, 0, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(2)).saveAsync(any(Event.class));
        statsActor.onStatsPersistMsg(new StatsPersistMsg(1, 1, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID));
        verify(eventService, times(3)).saveAsync(any(Event.class));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`StatsActorTest` 在 ThingsBoard Application 测试模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
