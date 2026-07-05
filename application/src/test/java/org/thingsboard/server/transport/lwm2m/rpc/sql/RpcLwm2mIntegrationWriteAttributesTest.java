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
package org.thingsboard.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;


/**
 * 中文说明：
 * 1. 类目的：`RpcLwm2mIntegrationWriteAttributesTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class RpcLwm2mIntegrationWriteAttributesTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * WriteAttributes {"id":"/3/0/14","attributes":{"pmax":100, "pmin":10}}
     * if not implemented:
     * {"result":"INTERNAL_SERVER_ERROR","error":"not implemented"}
     * if implemented:
     * {"result":"CHANGED"}
     */
    /**
     * 功能：验证错误信息相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWriteAttributesResourceWithParametersById_Result_INTERNAL_SERVER_ERROR() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String expectedValue = "{\"pmax\":100, \"pmin\":10}";
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.getName(), rpcActualResult.get("result").asText());
        String expected = "not implemented";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `path`：文件或资源路径。
     * - `value`：值。
     * 返回：文本结果。
     */
    private String sendRPCExecuteWithValueById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"WriteAttributes\", \"params\": {\"id\": \"" + path + "\", \"attributes\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RpcLwm2mIntegrationWriteAttributesTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
