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
 * 1. `TenantActorTest` 是 ThingsBoard Application 中验证 `TenantActor` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
}