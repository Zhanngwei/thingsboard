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
 * 1. `RpcLwm2mIntegrationCreateTest` 是 ThingsBoard Application 中验证 `RpcLwm2mIntegrationCreate` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRpcLwM2MIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
