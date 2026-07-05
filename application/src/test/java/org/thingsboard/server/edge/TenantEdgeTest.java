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
 * 1. 类目的：`TenantEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
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

/*
 * 本类总结：
 * 1. 核心职责：`TenantEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
