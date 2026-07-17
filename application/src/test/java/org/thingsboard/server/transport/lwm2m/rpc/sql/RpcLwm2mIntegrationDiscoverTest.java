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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;


/**
 * 中文说明：
 * 1. `RpcLwm2mIntegrationDiscoverTest` 是 ThingsBoard Application 中验证 `RpcLwm2mIntegrationDiscover` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractRpcLwM2MIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class RpcLwm2mIntegrationDiscoverTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * DiscoverAll
     *
     * @throws Exception
     */
    /**
     * 功能：验证客户端相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDiscoverAll_Return_CONTENT_LinksAllObjectsAllInstancesOfClient() throws Exception {
        String setRpcRequest = "{\"method\":\"DiscoverAll\"}";
        String actualResult = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        JsonNode rpcActualValue = JacksonUtil.toJsonNode(rpcActualResult.get("value").asText());
        Set actualObjects = ConcurrentHashMap.newKeySet();
        Set actualInstances = ConcurrentHashMap.newKeySet();
        rpcActualValue.forEach(node -> {
            if (!node.get("uriReference").asText().equals("/")) {
                LwM2mPath path = new LwM2mPath(node.get("uriReference").asText());
                actualObjects.add("/" + path.getObjectId());
                if (path.isObjectInstance()) {
                    actualInstances.add("/" + path.getObjectId() + "/" + path.getObjectInstanceId());
                }
            }
        });
        assertEquals(expectedInstances, actualInstances);
        assertEquals(expectedObjects, actualObjects);
    }

    /**
     * Discover {"id":"/3"}
     *
     * @throws Exception
     */
    /**
     * 功能：验证`Discover Object Return CONTENT Links Instances And Resources On Ly Expected Object`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDiscoverObject_Return_CONTENT_LinksInstancesAndResourcesOnLyExpectedObject() {
        expectedObjectIdVers.forEach(expected -> {
            try {
                String actualResult  = sendDiscover((String) expected);
                String expectedObjectId = pathIdVerToObjectId((String) expected);
                ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                String[] actualValues = rpcActualResult.get("value").asText().split(",");
                assertTrue(actualValues.length > 0);
                assertEquals(0, Arrays.stream(actualValues).filter(path -> !path.contains(expectedObjectId)).collect(Collectors.toList()).size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Discover {"id":"3/0"}
     * If WriteAttributes not implemented:
     * {"result":"CONTENT","value":"</3/0>,</3/0/0>,</3/0/1>,</3/0/2>,</3/0/3>,</3/0/4>,</3/0/5>,</3/0/6>,</3/0/7>,</3/0/8>,</3/0/9>,</3/0/10>,</3/0/11>,</3/0/12>,</3/0/13>,</3/0/14>,</3/0/15>,</3/0/16>,</3/0/1
     * 7>,</3/0/18>,</3/0/19>,</3/0/20>,</3/0/21>,</3/0/22>"}
     * If WriteAttributes implemented and WriteAttributes saved
     * Discover {"id":"19/0"}
     * {"result":"CONTENT","value":"[</19/0>;dim=2;pmin=10;pmax=60;gt=50;lt=42.2,</19/0/0>;pmax=120, </19/0/1>, </19/0/2>, </19/0/3>, </19/0/4>, </19/0/5>;lt=45]"}
     */
    /**
     * 功能：验证`Discover Instance Return CONTENT Links Resources On Ly Expected Instance`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDiscoverInstance_Return_CONTENT_LinksResourcesOnLyExpectedInstance() throws Exception {
        String expected = (String) expectedObjectIdVerInstances.stream().findAny().get();
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expectedObjectInstanceId = pathIdVerToObjectId(expected);
        String[] actualValues = rpcActualResult.get("value").asText().split(",");
        assertTrue(actualValues.length > 0);
        assertEquals(0, Arrays.stream(actualValues).filter(path -> !path.contains(expectedObjectInstanceId)).collect(Collectors.toList()).size());
    }

    /**
     * Discover {"id":"3/0/14"}
     * If WriteAttributes implemented:
     * {"result":"CONTENT","value":"</3/0/14>;pmax=100, "pmin":10, "ver"=1.0"}
     * If WriteAttributes not implemented:
     * {"result":"CONTENT","value":"</3/0/14>"}
     * Discover {"id":"19_1.1/0/0"}
     * If WriteAttributes implemented:
     * {"result":"CONTENT","value":"</19/0/0>;pmax=100, "pmin":10, "ver"=1.1"}
     * If WriteAttributes not implemented:
     * {"result":"CONTENT","value":"</19/0/0>"}
     */
    /**
     * 功能：验证`Discover Resource Return CONTENT Links Resource On Ly Expected Resource`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDiscoverResource_Return_CONTENT_LinksResourceOnLyExpectedResource() throws Exception {
        String expectedInstance = (String) expectedInstances.stream().findFirst().get();
        String expectedObjectInstanceId = pathIdVerToObjectId(expectedInstance);
        LwM2mPath expectedPath = new LwM2mPath(expectedObjectInstanceId);
        int expectedResource = lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnablers().get(expectedPath.getObjectId()).getObjectModel().resources.entrySet().stream().findAny().get().getKey();
        String expected = expectedInstance + "/" + expectedResource;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String expectedResourceId = "<" + expectedObjectInstanceId + "/" + expectedResource + ">";
        String actualValue = rpcActualResult.get("value").asText();
        assertEquals(expectedResourceId, actualValue );

    }

    /**
     * Discover {"id":"2/0"}
     *{"result":"NOT_FOUND"}
     */
    /**
     * 功能：验证`Discover Object Instance Absent In Object Return NOT FOUND`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDiscoverObjectInstanceAbsentInObject_Return_NOT_FOUND() throws Exception {
        String expected = objectIdVer_2 + "/" + OBJECT_INSTANCE_ID_0;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }
    /**
     * Discover {"id":"2/0/2"}
     * {"result":"NOT_FOUND"}
     */
    /**
     * 功能：验证`Discover Resource Absent In Object Return NOT FOUND`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDiscoverResourceAbsentInObject_Return_NOT_FOUND() throws Exception {
         String expected = objectIdVer_2 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2;
        String actualResult = sendDiscover(expected);
        ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * 功能：发送或提交`Discover`。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：文本结果。
     */
    private String sendDiscover(String path) throws Exception {
        String setRpcRequest = "{\"method\": \"Discover\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
