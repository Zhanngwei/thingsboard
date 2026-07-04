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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`RpcLwm2mIntegrationObserveTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class RpcLwm2mIntegrationObserveTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * ObserveReadAll&ObserveReadAll
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveReadAllNothingObservation_Result_CONTENT_Value_Count_0` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveReadAllNothingObservation_Result_CONTENT_Value_Count_0() throws Exception {
        String idVer_3_0_0 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_0;
        sendRpcObserve("Observe", fromVersionedIdToObjectId(idVer_3_0_0));
        String actualResultBefore = sendRpcObserve("ObserveReadAll", null);
        ObjectNode rpcActualResultBefore = JacksonUtil.fromString(actualResultBefore, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultBefore.get("result").asText());
        int cntObserveBefore = rpcActualResultBefore.get("value").asText().split(",").length;
        assertTrue(cntObserveBefore > 0);
        String actualResult = sendRpcObserve("ObserveCancelAll", null);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        int cntObserveCancelAll = Integer.parseInt(rpcActualResult.get("value").asText());
        assertTrue(cntObserveCancelAll > 0);
        String actualResultAfter = sendRpcObserve("ObserveReadAll", null);
        ObjectNode rpcActualResultAfter = JacksonUtil.fromString(actualResultAfter, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultAfter.get("result").asText());
        String expectResultAfter = "[]";
        assertEquals( expectResultAfter, rpcActualResultAfter.get("value").asText());
    }

    /**
     * Observe {"id":"/3/0/0"}
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveSingleResourceWithout_IdVer_1_0_Result_CONTENT_Value_SingleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveSingleResourceWithout_IdVer_1_0_Result_CONTENT_Value_SingleResource() throws Exception {
        String expectedId = objectInstanceIdVer_3 + "/" + RESOURCE_ID_0;
        String actualResult = sendRpcObserve("Observe", fromVersionedIdToObjectId(expectedId));
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
    }
    /**
     * Observe {"id":"/3_1.0/0/14"}
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveSingleResourceWith_IdVer_1_0_Result_CONTENT_Value_SingleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveSingleResourceWith_IdVer_1_0_Result_CONTENT_Value_SingleResource() throws Exception {
        String expectedId = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String actualResult = sendRpcObserve("Observe", expectedId);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertTrue(rpcActualResult.get("value").asText().contains("LwM2mSingleResource"));
    }

    /**
     * Observe {"id":"/3_1.1/0/13"}
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveWithBadVersion_Result_BadRequest_ErrorMsg_BadVersionMustBe1_0` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveWithBadVersion_Result_BadRequest_ErrorMsg_BadVersionMustBe1_0() throws Exception {
        String expectedInstance = (String) expectedInstances.stream().filter(path -> !((String)path).contains("_")).findFirst().get();
        LwM2mPath expectedPath = new LwM2mPath(expectedInstance);
        int expectedResource = lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnablers().get(expectedPath.getObjectId()).getObjectModel().resources.entrySet().stream().findAny().get().getKey();
        String expectedId = "/" + expectedPath.getObjectId() + "_1.2" + "/" + expectedPath.getObjectInstanceId() + "/" + expectedResource;
        String actualResult = sendRpcObserve("Observe", expectedId);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Specified resource id " + expectedId +" is not valid version! Must be version: 1.0";
        assertEquals(expected, rpcActualResult.get("error").asText());
    }

    /**
     * Not implemented Instance
     * Observe {"id":"/2/0"}
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveNoImplementedInstanceOnDevice_Result_NotFound` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveNoImplementedInstanceOnDevice_Result_NotFound() throws Exception {
        String objectInstanceIdVer = (String) expectedObjectIdVers.stream().filter(path -> ((String)path).contains("/" + ACCESS_CONTROL)).findFirst().get();
        String expected = objectInstanceIdVer + "/" + OBJECT_INSTANCE_ID_0;
        String actualResult = sendRpcObserve("Observe", expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Not implemented Resource
     * Observe {"id":"/19_1.1/0/0"}
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveNoImplementedResourceOnDeviceValueNull_Result_BadRequest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveNoImplementedResourceOnDeviceValueNull_Result_BadRequest() throws Exception {
        String expected = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        String actualResult = sendRpcObserve("Observe", expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        String expectedValue = "value MUST NOT be null";
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        assertEquals(expectedValue, rpcActualResult.get("error").asText());
    }

    /**
     * Repeated request on Observe
     * Observe {"id":"/5/0/0"}
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveRSourceNotRead_Result_METHOD_NOT_ALLOWED` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveRSourceNotRead_Result_METHOD_NOT_ALLOWED() throws Exception {
        String expectedId = objectInstanceIdVer_5 + "/" + RESOURCE_ID_0;
        sendRpcObserve("Observe", expectedId);
        String actualResult = sendRpcObserve("Observe", expectedId);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Repeated request on Observe
     * Observe {"id":"/3/0/9"}
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveRepeatedRequestObserveOnDevice_Result_BAD_REQUEST_ErrorMsg_AlreadyRegistered` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveRepeatedRequestObserveOnDevice_Result_BAD_REQUEST_ErrorMsg_AlreadyRegistered() throws Exception {
        String idVer_3_0_0 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_0;
        sendRpcObserve("Observe", fromVersionedIdToObjectId(idVer_3_0_0));
        sendRpcObserve("ObserveReadAll", null);
        String actualResult = sendRpcObserve("Observe", idVer_3_0_0);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Observation is already registered!";
        assertEquals(expected, rpcActualResult.get("error").asText());
    }

    /**
     * ObserveReadAll
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveReadAll_Result_CONTENT_Value_Contains_Paths_Count_ObserveReadAll` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveReadAll_Result_CONTENT_Value_Contains_Paths_Count_ObserveReadAll() throws Exception {
        String actualResultReadAll = sendRpcObserve("ObserveReadAll", null);
        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
        String actualValuesReadAll = rpcActualResultReadAll.get("value").asText();
        log.warn("ObserveReadAll:  [{}]", actualValuesReadAll);
        assertEquals(2, actualValuesReadAll.split(",").length);
    }


    /**
     * ObserveCancel {"id":"/3/0/3"}
     * ObserveCancel {"id":"/5/0/3"}
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testObserveCancelOneResource_Result_CONTENT_Value_Count_1` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testObserveCancelOneResource_Result_CONTENT_Value_Count_1() throws Exception {
        sendRpcObserve("ObserveCancelAll", null);
        String expectedId_3_0_3 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_3;
        String expectedId_5_0_3 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        sendRpcObserve("Observe", expectedId_3_0_3);
        sendRpcObserve("Observe", expectedId_5_0_3);
        String actualResult = sendRpcObserve("ObserveCancel", expectedId_3_0_3);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        assertEquals("1", rpcActualResult.get("value").asText());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendRpcObserve` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String sendRpcObserve(String method, String params) throws Exception {
        return sendObserve(method, params, deviceId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RpcLwm2mIntegrationObserveTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
