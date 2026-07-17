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
 * 1. `DefaultTbApiUsageStateServiceTest` 是 ThingsBoard Application 中验证 `DefaultTbApiUsageStateService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
}