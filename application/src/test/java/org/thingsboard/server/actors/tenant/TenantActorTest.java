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
package org.thingsboard.server.actors.tenant;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`TenantActorTest` 是ThingsBoard Application 测试模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
public class TenantActorTest {

    /**
     * 租户对象，用于描述当前业务场景。
     */
    TenantActor tenantActor;
    TbActorCtx ctx;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    ActorSystemContext systemContext;
    TenantId tenantId = TenantId.SYS_TENANT_ID;
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws Exception {
        systemContext = mock(ActorSystemContext.class);
        ctx = mock(TbActorCtx.class);
        tenantActor = (TenantActor) new TenantActor.ActorCreator(systemContext, tenantId).createActor();
        when(systemContext.getTenantService()).thenReturn(mock(TenantService.class));
        tenantActor.init(ctx);
        tenantActor.cantFindTenant = false;
    }

    /**
     * 功能：删除或清理设备。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void deleteDeviceTest() {
        TbActorRef deviceActorRef = mock(TbActorRef.class);
        when(systemContext.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(new TopicPartitionInfo("Main", tenantId, 0,true));
        when(ctx.getOrCreateChildActor(any(), any(), any(), any())).thenReturn(deviceActorRef);
        ComponentLifecycleMsg componentLifecycleMsg = new ComponentLifecycleMsg(tenantId, deviceId, ComponentLifecycleEvent.DELETED);
        tenantActor.doProcess(componentLifecycleMsg);
        verify(deviceActorRef).tellWithHighPriority(eq(new DeviceDeleteMsg(tenantId, deviceId)));

        reset(ctx, deviceActorRef);
        when(systemContext.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(new TopicPartitionInfo("Main", tenantId, 1,false));
        tenantActor.doProcess(componentLifecycleMsg);
        verify(ctx, never()).getOrCreateChildActor(any(), any(), any(), any());
        verify(deviceActorRef, never()).tellWithHighPriority(any());
    }


/*
 * 本类总结：
 * 1. 核心职责：`TenantActorTest` 在 ThingsBoard Application 测试模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}