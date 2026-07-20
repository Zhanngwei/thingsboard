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
package org.thingsboard.server.transport.mqtt.sparkplug.rpc;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.exception.ThingsboardErrorCode.INVALID_ARGUMENTS;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DCMD;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NCMD;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.NAMESPACE;

/**
 * 中文说明：
 * 1. `AbstractMqttV5RpcSparkplugTest` 是 ThingsBoard Application 中验证 `AbstractMqttV5RpcSparkplug` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttV5ClientSparkplugTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractMqttV5RpcSparkplugTest  extends AbstractMqttV5ClientSparkplugTest {

    /**
     * 值常量，用于统一引用固定值。
     */
    private static final int metricBirthValue_Int32 = 123456;
    private static final String sparkplugRpcRequest = "{\"metricName\":\"" + metricBirthName_Int32 + "\",\"value\":" + metricBirthValue_Int32 + "}";

    /**
     * 功能：处理RPC。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.RPC, 1);
        String expected = "{\"result\":\"Success: " + SparkplugMessageType.NCMD.name() + "\"}";
        String actual = sendRPCSparkplug(NCMD.name(), sparkplugRpcRequest, savedGateway);
        await(alias + SparkplugMessageType.NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(metricBirthValue_Int32 == mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void processClientDeviceWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1, ts);
        String expected = "{\"result\":\"Success: " + DCMD.name() + "\"}";
        String actual = sendRPCSparkplug(DCMD.name() , sparkplugRpcRequest, devices.get(0));
        await(alias + NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(metricBirthValue_Int32 == mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    /**
     * 功能：处理RPC。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.RPC, 1);
        String invalidateTypeMessageName = "RCMD";
        String expected = "{\"result\":\"" + INVALID_ARGUMENTS + "\",\"error\":\"Failed to convert device RPC command to MQTT msg: " +
                invalidateTypeMessageName + "{\\\"metricName\\\":\\\"" + metricBirthName_Int32 + "\\\",\\\"value\\\":" + metricBirthValue_Int32 + "}\"}";
        String actual = sendRPCSparkplug(invalidateTypeMessageName, sparkplugRpcRequest, savedGateway);
        Assert.assertEquals(expected, actual);
    }

    /**
     * 功能：处理RPC。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InBirthNotHaveMetric_BAD_REQUEST_PARAMS() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.RPC, 1);
        String metricNameBad = metricBirthName_Int32 + "_Bad";
        String sparkplugRpcRequestBad = "{\"metricName\":\"" + metricNameBad + "\",\"value\":" + metricBirthValue_Int32 + "}";
        String expected = "{\"result\":\"BAD_REQUEST_PARAMS\",\"error\":\"Failed send To Node Rpc Request: " +
                DCMD.name() + ". This node does not have a metricName: [" + metricNameBad + "]\"}";
        String actual = sendRPCSparkplug(DCMD.name(), sparkplugRpcRequestBad, savedGateway);
        Assert.assertEquals(expected, actual);
     }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `nameTypeMessage`：待处理消息。
     * - `keyValue`：键。
     * - `device`：设备信息或设备标识。
     * 返回：文本结果。
     */
    private String sendRPCSparkplug(String nameTypeMessage, String keyValue, Device device) throws Exception {
        String setRpcRequest = "{\"method\": \"" + nameTypeMessage + "\", \"params\": " + keyValue + "}";
        return doPostAsync("/api/plugins/rpc/twoway/" + device.getId().getId(), setRpcRequest, String.class, status().isOk());
    }

}
