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
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_3;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_4;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_8;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;


/**
 * 中文说明：
 * 1. `RpcLwm2mIntegrationExecuteTest` 是 ThingsBoard Application 中验证 `RpcLwm2mIntegrationExecute` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRpcLwM2MIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class RpcLwm2mIntegrationExecuteTest extends AbstractRpcLwM2MIntegrationTest {


    /**
     * Update FW
     * Execute {"id":"5/0/2"}
     * {"result":"CHANGED"}
     */
    /**
     * 功能：验证`Execute Update FW By Id Result CHANGED`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteUpdateFWById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Update SW
     * Execute {"id":"9/0/4"}
     * {"result":"CHANGED"}
     */
    /**
     * 功能：验证`Execute Update SW By Id Result CHANGED`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteUpdateSWById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_9 + "/" + RESOURCE_ID_4;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Reboot
     * Execute {"id":"3/0/4"}
     * {"result":"CHANGED"}
     */
    /**
     * 功能：验证`Execute Reboot By Id Result CHANGED`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteRebootById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_4;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Registration Update Trigger
     * Execute {"id":"1/0/8"}
     * {"result":"CHANGED"}
     */
    /**
     * 功能：验证`Execute Registration Update Trigger By Id Result CHANGED`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteRegistrationUpdateTriggerById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_1 + "/" + RESOURCE_ID_8;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }


    /**
     * execute_resource_with_parameters (execute reboot after 60 seconds on device)
     * Execute {"id":"3/0/4","value":60}
     * {"result":"CHANGED"}
     */
    /**
     * 功能：验证参数集合相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteResourceWithParametersById_Result_CHANGED() throws Exception {
        String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_4;
        Object expectedValue = 60;
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Bootstrap-Request Trigger
     * Execute {"id":"1/0/9"}
     * {"result":"BAD_REQUEST","error":"probably no bootstrap server configured"}
     */
    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteBootstrapRequestTriggerById_Result_BAD_REQUEST_Error_NoBootstrapServerConfigured() throws Exception {
        String expectedPath = objectInstanceIdVer_1 + "/" + RESOURCE_ID_9;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "probably no bootstrap server configured";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * bad: resource operation not "E"
     * Execute {"id":"5_1.0/0/3"}
     * {"result":"BAD_REQUEST","error":"Resource with /5_1.0/0/3 is not executable."}
     */
    /**
     * 功能：验证`Execute Resource With Operation Not Execute By Id Result METHOD NOT ALLOWED`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteResourceWithOperationNotExecuteById_Result_METHOD_NOT_ALLOWED() throws Exception {
        String expectedPath = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Resource with " + expectedPath + " is not executable.";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * bad: execute_non_existing_resource_on_non_existing_object
     * Execute {"id":"50/0/3"}
     * {"result":"BAD_REQUEST","error":"Specified object id 50 absent in the list supported objects of the client or is security object!"}
     */
    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteNonExistingResourceOnNonExistingObjectById_Result_BAD_REQUEST() throws Exception {
        String expectedPath = OBJECT_ID_VER_50 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId((String) expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }

    /**
     * bad: execute security object
     * Execute {"id":"0/0/3"}
     * {"result":"BAD_REQUEST","error":"Specified object id 0 absent in the list supported objects of the client or is security object!"}
     */
    /**
     * 功能：验证`Execute Security Object By Id Result NOT FOUND`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testExecuteSecurityObjectById_Result_NOT_FOUND() throws Exception {
        String expectedPath = objectIdVer_0 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId((String) expectedPath);
        LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }


    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：文本结果。
     */
    private String sendRPCExecuteById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Execute\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `path`：文件或资源路径。
     * - `value`：值。
     * 返回：文本结果。
     */
    private String sendRPCExecuteWithValueById(String path, Object value) throws Exception {
        String setRpcRequest = "{\"method\": \"Execute\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
