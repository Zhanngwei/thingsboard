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
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `TenantEdgeTest` 是 ThingsBoard Application 中验证 `TenantEdge` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractEdgeTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class TenantEdgeTest extends AbstractEdgeTest {

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateTenant() throws Exception {
        loginSysAdmin();

        // save current value into tmp to revert after test
        Tenant savedTenant = doGet("/api/tenant/" + tenantId, Tenant.class);

        // updated edge tenant
        savedTenant.setTitle("Updated Title for Tenant Edge Test");
        edgeImitator.expectMessageAmount(2); // expect tenant and tenant profile update msg
        savedTenant = doPost("/api/tenant", savedTenant, Tenant.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<TenantUpdateMsg> tenantUpdateMsgOpt = edgeImitator.findMessageByType(TenantUpdateMsg.class);
        Assert.assertTrue(tenantUpdateMsgOpt.isPresent());
        TenantUpdateMsg tenantUpdateMsg = tenantUpdateMsgOpt.get();
        Optional<TenantProfileUpdateMsg> tenantProfileUpdateMsgOpt = edgeImitator.findMessageByType(TenantProfileUpdateMsg.class);
        Assert.assertTrue(tenantProfileUpdateMsgOpt.isPresent());
        TenantProfileUpdateMsg tenantProfileUpdateMsg = tenantProfileUpdateMsgOpt.get();
        Tenant tenantMsg = JacksonUtil.fromString(tenantUpdateMsg.getEntity(), Tenant.class, true);
        Assert.assertNotNull(tenantMsg);
        Assert.assertEquals(savedTenant, tenantMsg);
        TenantProfile tenantProfileMsg = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
        Assert.assertNotNull(tenantProfileMsg);
        Assert.assertEquals(tenantMsg.getTenantProfileId(), tenantProfileMsg.getId());
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantUpdateMsg.getMsgType());
        Assert.assertEquals("Updated Title for Tenant Edge Test", tenantMsg.getTitle());

        //change tenant profile for tenant
        TenantProfile tenantProfile = createTenantProfile();
        savedTenant.setTenantProfileId(tenantProfile.getId());
        edgeImitator.expectMessageAmount(2); // expect tenant and tenant profile update msg
        savedTenant = doPost("/api/tenant", savedTenant, Tenant.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        tenantUpdateMsgOpt = edgeImitator.findMessageByType(TenantUpdateMsg.class);
        Assert.assertTrue(tenantUpdateMsgOpt.isPresent());
        tenantUpdateMsg = tenantUpdateMsgOpt.get();
        tenantMsg = JacksonUtil.fromString(tenantUpdateMsg.getEntity(), Tenant.class, true);
        Assert.assertNotNull(tenantMsg);
        tenantProfileUpdateMsgOpt = edgeImitator.findMessageByType(TenantProfileUpdateMsg.class);
        Assert.assertTrue(tenantProfileUpdateMsgOpt.isPresent());
        tenantProfileUpdateMsg = tenantProfileUpdateMsgOpt.get();
        tenantProfileMsg = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
        Assert.assertNotNull(tenantProfileMsg);
        // tenant update
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenant, tenantMsg);
        Assert.assertEquals(savedTenant.getTenantProfileId(), tenantProfileMsg.getId());
    }

    /**
     * 功能：保存或创建租户。
     * 参数：无。
     * 返回：处理结果。
     */
    private TenantProfile createTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("TestEdge tenant profile");
        return doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
    }
}
