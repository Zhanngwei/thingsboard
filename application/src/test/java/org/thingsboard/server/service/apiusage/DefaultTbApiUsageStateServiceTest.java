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
package org.thingsboard.server.service.apiusage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbApiUsageStateServiceTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTbApiUsageStateServiceTest {

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Mock
    TenantService tenantService;
    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Mock
    TimeseriesService tsService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    TbClusterService clusterService;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Mock
    PartitionService partitionService;
    /**
     * 租户，表示当前对象所处状态。
     */
    @Mock
    TenantApiUsageState tenantUsageStateMock;
    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Mock
    ApiUsageStateService apiUsageStateService;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Mock
    TbTenantProfileCache tenantProfileCache;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    MailService mailService;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Mock
    DbCallbackExecutorService dbExecutor;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112"));

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Spy
    @InjectMocks
    DefaultTbApiUsageStateService service;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
    }

    /**
     * 功能：验证 `givenTenantIdFromEntityStatesMap_whenGetApiUsageState` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenTenantIdFromEntityStatesMap_whenGetApiUsageState() {
        service.myUsageStates.put(tenantId, tenantUsageStateMock);
        ApiUsageState tenantUsageState = service.getApiUsageState(tenantId);
        assertThat(tenantUsageState, is(tenantUsageStateMock.getApiUsageState()));
        Mockito.verify(service, never()).getOrFetchState(tenantId, tenantId);
    }


/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbApiUsageStateServiceTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}