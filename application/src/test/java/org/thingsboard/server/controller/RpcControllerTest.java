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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`RpcControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@DaoSqlTest
public class RpcControllerTest extends AbstractControllerTest {

    /**
     * 租户对象，用于描述当前业务场景。
     */
    private Tenant savedTenant;
    private User tenantAdmin;

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    /**
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：处理结果。
     */
    private Device createDefaultDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");

        return device;
    }

    /**
     * 功能：保存或创建RPC。
     * 参数：无。
     * 返回：处理结果。
     */
    private ObjectNode createDefaultRpc() {
        ObjectNode rpc = JacksonUtil.newObjectNode();
        rpc.put("method", "setGpio");

        ObjectNode params = JacksonUtil.newObjectNode();

        params.put("pin", 7);
        params.put("value", 1);

        rpc.set("params", params);
        rpc.put("persistent", true);
        rpc.put("timeout", 5000);

        return rpc;
    }

    /**
     * 功能：获取RPC。
     * 参数：
     * - `rpcId`：RPCID。
     * 返回：处理结果。
     */
    private Rpc getRpcById(String rpcId) throws Exception {
        return doGet("/api/rpc/persistent/" + rpcId, Rpc.class);
    }

    /**
     * 功能：删除或清理RPC。
     * 参数：
     * - `rpcId`：RPCID。
     * 返回：处理结果。
     */
    private MvcResult removeRpcById(String rpcId) throws Exception {
        return doDelete("/api/rpc/persistent/" + rpcId).andReturn();
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveRpc() throws Exception {
        Device device = createDefaultDevice();
        Device savedDevice = doPost("/api/device", device, Device.class);

        ObjectNode rpc = createDefaultRpc();
        String result = doPostAsync(
                "/api/rpc/oneway/" + savedDevice.getId().getId().toString(),
                JacksonUtil.toString(rpc),
                String.class,
                status().isOk()
        );
        String rpcId = JacksonUtil.fromString(result, JsonNode.class)
                .get("rpcId")
                .asText();
        Rpc savedRpc = getRpcById(rpcId);

        Assert.assertNotNull(savedRpc);
        Assert.assertEquals(savedDevice.getId(), savedRpc.getDeviceId());
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRpc() throws Exception {
        Device device = createDefaultDevice();
        Device savedDevice = doPost("/api/device", device, Device.class);

        ObjectNode rpc = createDefaultRpc();
        String result = doPostAsync(
                "/api/rpc/oneway/" + savedDevice.getId().getId().toString(),
                JacksonUtil.toString(rpc),
                String.class,
                status().isOk()
        );
        String rpcId = JacksonUtil.fromString(result, JsonNode.class)
                .get("rpcId")
                .asText();
        Rpc savedRpc = getRpcById(rpcId);

        MvcResult mvcResult = removeRpcById(savedRpc.getId().getId().toString());
        MvcResult res = doGet("/api/rpc/persistent/" + rpcId)
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode deleteResponse = JacksonUtil.fromString(res.getResponse().getContentAsString(), JsonNode.class);
        Assert.assertEquals(404, deleteResponse.get("status").asInt());

        String url = "/api/rpc/persistent/device/" + savedDevice.getUuidId().toString()
                + "?" + "page=0" + "&" +
                "pageSize=" + Integer.MAX_VALUE + "&" +
                "rpcStatus=" + RpcStatus.DELETED.name();
        MvcResult byDeviceResult = doGet(url).andReturn();
        JsonNode byDeviceResponse = JacksonUtil.fromString(byDeviceResult.getResponse().getContentAsString(), JsonNode.class);

        Assert.assertEquals(500, byDeviceResponse.get("status").asInt());
    }

    /**
     * 功能：验证设备ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetRpcsByDeviceId() throws Exception {
        Device device = createDefaultDevice();
        Device savedDevice = doPost("/api/device", device, Device.class);

        ObjectNode rpc = createDefaultRpc();

        String result = doPostAsync(
                "/api/rpc/oneway/" + savedDevice.getId().getId().toString(),
                JacksonUtil.toString(rpc),
                String.class,
                status().isOk()
        );
        String rpcId = JacksonUtil.fromString(result, JsonNode.class)
                .get("rpcId")
                .asText();

        String url = "/api/rpc/persistent/device/" + savedDevice.getId().getId()
                + "?" + "page=0" + "&" +
                "pageSize=" + Integer.MAX_VALUE + "&" +
                "rpcStatus=" + RpcStatus.QUEUED;

        MvcResult byDeviceResult = doGetAsync(url).andReturn();

        List<Rpc> byDeviceRpcs = JacksonUtil.fromString(
                byDeviceResult
                        .getResponse()
                        .getContentAsString(),
                new TypeReference<PageData<Rpc>>() {}
        ).getData();


        boolean found = byDeviceRpcs.stream().anyMatch(r ->
                r.getUuidId().toString().equals(rpcId)
                        && r.getDeviceId().equals(savedDevice.getId())
        );

        Assert.assertTrue(found);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RpcControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
