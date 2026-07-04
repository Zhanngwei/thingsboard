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

import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_11;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;


/**
 * 中文说明：
 * 1. 类目的：`RpcLwm2mIntegrationReadTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class RpcLwm2mIntegrationReadTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Read {"id":"/3"}
     * Read {"id":"/6"}...
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadAllObjectsInClientById_Result_CONTENT_Value_IsLwM2mObject_IsInstances` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadAllObjectsInClientById_Result_CONTENT_Value_IsLwM2mObject_IsInstances() throws Exception {
                expectedObjectIdVers.forEach(expected -> {
            try {
                String actualResult  = sendRPCById((String) expected);
                String expectedObjectId = pathIdVerToObjectId((String) expected);
                LwM2mPath expectedPath = new LwM2mPath(expectedObjectId);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String expectedObjectInstances = "LwM2mObject [id=" + expectedPath.getObjectId() + ", instances={0=LwM2mObjectInstance [id=0, resources=";
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (expectedPath.getObjectId() == 2) {
                    expectedObjectInstances = "LwM2mObject [id=2, instances={}]";
                }
                assertTrue(rpcActualResult.get("value").asText().contains(expectedObjectInstances));
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Read {"id":"/5/0"}
     *
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadAllInstancesInClientById_Result_CONTENT_Value_IsInstances_IsResources` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadAllInstancesInClientById_Result_CONTENT_Value_IsInstances_IsResources() throws Exception{
        expectedObjectIdVerInstances.forEach(expected -> {
            try {
                String actualResult  = sendRPCById((String) expected);
                String expectedObjectId = pathIdVerToObjectId((String) expected);
                LwM2mPath expectedPath = new LwM2mPath(expectedObjectId);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String expectedObjectInstances = "LwM2mObjectInstance [id=" + expectedPath.getObjectInstanceId() + ", resources={";
                assertTrue(rpcActualResult.get("value").asText().contains(expectedObjectInstances));
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Read {"id":"/19/1/0"}
     *
     * @throws Exception
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadMultipleResourceById_Result_CONTENT_Value_IsLwM2mMultipleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadMultipleResourceById_Result_CONTENT_Value_IsLwM2mMultipleResource() throws Exception {
       String expectedIdVer = objectInstanceIdVer_3 +"/" + RESOURCE_ID_11;
        String actualResult = sendRPCById(expectedIdVer);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "LwM2mMultipleResource [id=" + RESOURCE_ID_11 + ", values={";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * Read {"id":"/3/0/14"}
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadSingleResourceById_Result_CONTENT_Value_IsLwM2mSingleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadSingleResourceById_Result_CONTENT_Value_IsLwM2mSingleResource() throws Exception {
         String expectedIdVer = objectInstanceIdVer_3 +"/" + RESOURCE_ID_14;
        String actualResult = sendRPCById(expectedIdVer);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * Read {"key":"UtfOffset"}
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadSingleResourceByKey_Result_CONTENT_Value_IsLwM2mSingleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadSingleResourceByKey_Result_CONTENT_Value_IsLwM2mSingleResource() throws Exception {
        String expectedKey = RESOURCE_ID_NAME_3_14;
        String actualResult = sendRPCByKey(expectedKey);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * ReadComposite {"ids":["/1_1.2", "/3_1.0/0/1", "/3_1.0/0/11"]}
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadCompositeSingleResourceByIds_Result_CONTENT_Value_IsObjectIsLwM2mSingleResourceIsLwM2mMultipleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadCompositeSingleResourceByIds_Result_CONTENT_Value_IsObjectIsLwM2mSingleResourceIsLwM2mMultipleResource() throws Exception {
        String expectedIdVer_1 = (String) expectedObjectIdVers.stream().filter(path -> (!((String)path).contains("/" + BINARY_APP_DATA_CONTAINER) && ((String)path).contains("/" + SERVER))).findFirst().get();
        String objectId_1 = pathIdVerToObjectId(expectedIdVer_1);
        String expectedIdVer3_0_1 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_1;
        String expectedIdVer3_0_11 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_11;
        String objectInstanceId_3 = pathIdVerToObjectId(objectInstanceIdVer_3);
        String expectedIds = "[\"" + expectedIdVer_1 + "\", \"" + expectedIdVer3_0_1 + "\", \"" + expectedIdVer3_0_11 + "\"]";
        String actualResult = sendCompositeRPCByIds(expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expected1 = objectId_1 + "=LwM2mObject [id=" + new LwM2mPath(objectId_1).getObjectId() + ", instances={";
        String expected3_0_1 = objectInstanceId_3 + "/" + RESOURCE_ID_1 + "=LwM2mSingleResource [id=" + RESOURCE_ID_1 + ", value=";
        String expected3_0_11 = objectInstanceId_3 + "/" + RESOURCE_ID_11 + "=LwM2mMultipleResource [id=" + RESOURCE_ID_11 + ", values={";
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected1));
        assertTrue(actualValues.contains(expected3_0_1));
        assertTrue(actualValues.contains(expected3_0_11));
    }

    /**
     * ReadComposite {"ids":["/1_1.2/0/1", "/1_1.2/0/2", "/3_1.0/0"]}
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadCompositeSingleResourceByIds_Result_CONTENT_Value_IsObjectInstanceIsLwM2mSingleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadCompositeSingleResourceByIds_Result_CONTENT_Value_IsObjectInstanceIsLwM2mSingleResource() throws Exception {
        String expectedIdVer3_0 = objectInstanceIdVer_3;
        String expectedIdVer1_0_1 = objectInstanceIdVer_1 + "/" + RESOURCE_ID_1;
        String expectedIdVer1_0_2 = objectInstanceIdVer_1 + "/" + RESOURCE_ID_2;
        String expectedIds = "[\"" + expectedIdVer1_0_1 + "\", \"" + expectedIdVer1_0_2 + "\", \"" + expectedIdVer3_0 + "\"]";
        String actualResult = sendCompositeRPCByIds(expectedIds);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String objectInstanceId_3 = pathIdVerToObjectId(objectInstanceIdVer_3);
        LwM2mPath path = new LwM2mPath(objectInstanceId_3);
        String expected3_0 = objectInstanceId_3 + "=LwM2mObjectInstance [id=" + path.getObjectInstanceId() + ", resources={";
        String objectInstanceId_1 = pathIdVerToObjectId(objectInstanceIdVer_1);
        String expected1_0_1 = objectInstanceId_1 + "/" + RESOURCE_ID_1 + "=LwM2mSingleResource [id=" + RESOURCE_ID_1 + ", value=";
        String expected1_0_2 = objectInstanceId_1 + "/" + RESOURCE_ID_2 + "=null";
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected3_0));
        assertTrue(actualValues.contains(expected1_0_1));
        assertTrue(actualValues.contains(expected1_0_2));
    }

    /**
     * ReadComposite {"keys":["batteryLevel", "UtfOffset", "dataRead", "dataWrite"]}
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadCompositeSingleResourceByKeys_Result_CONTENT_Value_3_0_IsLwM2mSingleResource_19_0_0_AND_19_0_1__IsLwM2mMultipleResource` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadCompositeSingleResourceByKeys_Result_CONTENT_Value_3_0_IsLwM2mSingleResource_19_0_0_AND_19_0_1__IsLwM2mMultipleResource() throws Exception {
        String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedKey19_0_0 = RESOURCE_ID_NAME_19_0_0;
        String expectedKey19_1_0 = RESOURCE_ID_NAME_19_1_0;
        String expectedKey19_X_0 = "=LwM2mMultipleResource [id=0, values={0=LwM2mResourceInstance [id=0, value=1Bytes, type=OPAQUE]";
        String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_0 + "\", \"" + expectedKey19_1_0 + "\"]";
        String actualResult = sendCompositeRPCByKeys(expectedKeys);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String objectInstanceId_3 = pathIdVerToObjectId(objectInstanceIdVer_3);
        String objectId_19 = pathIdVerToObjectId(objectIdVer_19);
        String expected3_0_9 = objectInstanceId_3 + "/" + RESOURCE_ID_9 + "=LwM2mSingleResource [id=" + RESOURCE_ID_9 + ", value=";
        String expected3_0_14 = objectInstanceId_3 + "/" + RESOURCE_ID_14 + "=LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=";
        String expected19_0_0 = objectId_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0 +  expectedKey19_X_0;
        String expected19_1_0 = objectId_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 +  expectedKey19_X_0;
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected3_0_9));
        assertTrue(actualValues.contains(expected3_0_14));
        assertTrue(actualValues.contains(expected19_0_0));
        assertTrue(actualValues.contains(expected19_1_0));
    }

    /**
     * ReadComposite {"keys":["batteryLevel", "UtfOffset", "dataDescription"]}
     */
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReadCompositeSingleResourceByKeys_Result_CONTENT_Value_3_0_IsLwM2mSingleResource_19_0_3_IsNotConfiguredInTheDeviceProfile` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReadCompositeSingleResourceByKeys_Result_CONTENT_Value_3_0_IsLwM2mSingleResource_19_0_3_IsNotConfiguredInTheDeviceProfile() throws Exception {
        String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        String expectedKey19_0_3 = RESOURCE_ID_NAME_19_0_3;
        String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_3 + "\"]";
        String actualResult = sendCompositeRPCByKeys(expectedKeys);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String actualValue = rpcActualResult.get("error").asText();
        String expectedValue = expectedKey19_0_3 + " is not configured in the device profile!";
        assertEquals(actualValue, expectedValue);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `sendRPCById` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String sendRPCById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendRPCByKey` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String sendRPCByKey(String key) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"key\": \"" + key + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendCompositeRPCByIds` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String sendCompositeRPCByIds(String paths) throws Exception {
        String setRpcRequest = "{\"method\": \"ReadComposite\", \"params\": {\"ids\":" + paths + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `sendCompositeRPCByKeys` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String sendCompositeRPCByKeys(String keys) throws Exception {
        String setRpcRequest = "{\"method\": \"ReadComposite\", \"params\": {\"keys\":" + keys + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RpcLwm2mIntegrationReadTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
