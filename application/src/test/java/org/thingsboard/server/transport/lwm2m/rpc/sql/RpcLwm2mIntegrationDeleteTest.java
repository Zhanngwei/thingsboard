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
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_12;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;


/**
 * 中文说明：
 * 1. `RpcLwm2mIntegrationDeleteTest` 是 ThingsBoard Application 中验证 `RpcLwm2mIntegrationDelete` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRpcLwM2MIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class RpcLwm2mIntegrationDeleteTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * if there is such an instance
     * Delete {"id":"/3303/12"}
     * {"result":"DELETE"}
     */
    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteObjectInstanceIsSuchByIdKey_Result_DELETED() throws Exception {
        String expectedPath = objectIdVer_3303 + "/" + OBJECT_INSTANCE_ID_12;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.DELETED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * if there is no such instance
     * Delete {"id":"/19/12"}
     * {"result":"NOT_FOUND"}
     */
    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteObjectInstanceIsNotSuchByIdKey_Result_NOT_FOUND() throws Exception {
        String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_12;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * delete object
     * Delete {"id":"/19_1.1"}
     * {"result":"BAD_REQUEST","error":"Invalid path /19 : Only object instances can be delete"}
     */
    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteObjectByIdKey_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_19;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expected = "Invalid path " + pathIdVerToObjectId((String) expectedPath) + " : Only object instances can be delete";
        String actual = rpcActualResult.get("error").asText();
        assertTrue(actual.equals(expected));
    }


    /**
     * delete resource
     * Delete {"id":"/3/0/7"}
     * {"result":"METHOD_NOT_ALLOWED"}
     */
    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteResourceByIdKey_Result_METHOD_NOT_ALLOWED() throws Exception {
        String expectedPath = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + RESOURCE_ID_7;
        String actualResult = sendRPCDeleteById(expectedPath);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActualResult.get("result").asText());
    }


    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：文本结果。
     */
    private String sendRPCDeleteById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Delete\", \"params\": {\"id\": \"" + path  + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
