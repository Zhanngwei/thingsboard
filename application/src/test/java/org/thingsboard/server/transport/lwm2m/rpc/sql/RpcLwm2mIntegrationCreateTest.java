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
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;


/**
 * 中文说明：
 * 1. 类目的：`RpcLwm2mIntegrationCreateTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class RpcLwm2mIntegrationCreateTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Create  {"id":"/19_1.1","value":{"0":{"0":"00AC"}, "1":1}}
     *
     * create_2_instances_in_object
     * new ObjectInstance if Object is Multiple & Resource Single
     * Create  {"id":"/19_1.1/12","value":{"0":{"0":"00AC", "1":1}}}
     * {"{"result":"CREATED"}"}
     */
    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdByIdKey_Result_CREATED() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_12;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}, \"1\":1}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CREATED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Create  {"id":"/19_1.1","value":{"0":{"0":"00AC"}, "1":1}}
     *
     * create_2_instances_in_object
     * new ObjectInstance if Object is Multiple & Resource Single
     * Create  {"id":"/19_1.1/0","value":{"0":{"0":"00AC", "1":1}}}
     * {"result":"BAD_REQUEST","error":"instance 0 already exists"}
     */
    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdAlreadyExistsById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}, \"1\":1}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "instance " + OBJECT_INSTANCE_ID_0 + " already exists";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * failed: cannot_create_mandatory_single_object
     * Create  {"id":"/3/1,"value":{"0":"00AC"}}
     * {"result":"BAD_REQUEST","error":"Path /3/1. Object must be Multiple !"}
     */
    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdMandatorySingleObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_1;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Path " + expectedPath + ". Object must be Multiple !";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * failed:  cannot_create_instance_of_security_object
     * Create  {"id":"/0/2","value":{"2":4}}
     * {"result":"BAD_REQUEST","error":"Specified object id 0 absent in the list supported objects of the client or is security object!"}
     */
    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdSecurityObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_0 + "/" + OBJECT_INSTANCE_ID_1;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"2\":4}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId((String) expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * failed: cannot_create_instance_of_absent_object
     * Create  {"id":"/50/1","value":{"0":"00AC"}}
     * {"result":"BAD_REQUEST","error":"Specified object id 50 absent in the list supported objects of the client or is security object!"}
     */
    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdAbsentObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = OBJECT_ID_VER_50 + "/" + OBJECT_INSTANCE_ID_1;
        String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId((String) expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * 功能：发送或提交`RP Create By Id`。
     * 参数：
     * - `path`：文件或资源路径。
     * - `value`：值。
     * 返回：文本结果。
     */
    private String sendRPCreateById(String path, String value) throws Exception {
        String setRpcRequest = "{\"method\": \"Create\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RpcLwm2mIntegrationCreateTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
