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
 * 1. `RpcLwm2mIntegrationReadTest` 是 ThingsBoard Application 中验证 `RpcLwm2mIntegrationRead` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRpcLwM2MIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class RpcLwm2mIntegrationReadTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Read {"id":"/3"}
     * Read {"id":"/6"}...
     */
    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testReadAllObjectsInClientById_Result_CONTENT_Value_IsLwM2mObject_IsInstances() throws Exception {
                expectedObjectIdVers.forEach(expected -> {
            try {
                String actualResult  = sendRPCById((String) expected);
                String expectedObjectId = pathIdVerToObjectId((String) expected);
                LwM2mPath expectedPath = new LwM2mPath(expectedObjectId);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String expectedObjectInstances = "LwM2mObject [id=" + expectedPath.getObjectId() + ", instances={0=LwM2mObjectInstance [id=0, resources=";
                if (expectedPath.getObjectId() == 2) {
                    expectedObjectInstances = "LwM2mObject [id=2, instances={}]";
                }
                assertTrue(rpcActualResult.get("value").asText().contains(expectedObjectInstances));
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
    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
    /**
     * 功能：验证设备配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
     * 功能：发送或提交RPC。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：文本结果。
     */
    private String sendRPCById(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `key`：键。
     * 返回：文本结果。
     */
    private String sendRPCByKey(String key) throws Exception {
        String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"key\": \"" + key + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `paths`：文件或资源路径。
     * 返回：文本结果。
     */
    private String sendCompositeRPCByIds(String paths) throws Exception {
        String setRpcRequest = "{\"method\": \"ReadComposite\", \"params\": {\"ids\":" + paths + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `keys`：键。
     * 返回：文本结果。
     */
    private String sendCompositeRPCByKeys(String keys) throws Exception {
        String setRpcRequest = "{\"method\": \"ReadComposite\", \"params\": {\"keys\":" + keys + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
