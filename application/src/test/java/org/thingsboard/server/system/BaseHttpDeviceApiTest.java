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
package org.thingsboard.server.system;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`BaseHttpDeviceApiTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@TestPropertySource(properties = {
        "transport.http.enabled=true",
})
public abstract class BaseHttpDeviceApiTest extends AbstractControllerTest {

    private static final AtomicInteger idSeq = new AtomicInteger(new Random(System.currentTimeMillis()).nextInt());

    /**
     * 设备对象，用于描述当前业务场景。
     */
    protected Device device;
    protected DeviceCredentials deviceCredentials;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() throws Exception {
        loginTenantAdmin();
        device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    /**
     * 功能：验证`Get Attributes`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetAttributes() throws Exception {
        doGetAsync("/api/v1/" + "WRONG_TOKEN" + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isUnauthorized());
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isOk());

        Map<String, String> attrMap = new HashMap<>();
        attrMap.put("keyA", "valueA");
        mockMvc.perform(
                asyncDispatch(doPost("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes", attrMap, new String[]{}).andReturn()))
                .andExpect(status().isOk());
        Thread.sleep(2000);
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isOk());
    }

    /**
     * 功能：执行 `doGetAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doGetAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest;
        getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

    /**
     * 功能：执行 `doPostAsync` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `urlVariables`：`urlVariables` 参数。
     * 返回：处理结果。
     */
    protected ResultActions doPostAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = post(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseHttpDeviceApiTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
