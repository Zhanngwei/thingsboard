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
 * 1. `StatsActorTest` 是 ThingsBoard Application 中验证 `StatsActor` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
