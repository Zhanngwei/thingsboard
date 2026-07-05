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
package org.thingsboard.server.transport.mqtt.sparkplug.attributes;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NCMD;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.NAMESPACE;

/**
 * Created by nickAS21 on 12.01.23
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttV5ClientSparkplugAttributesTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugAttributesTest extends AbstractMqttV5ClientSparkplugTest {

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPublishNCMDReBirth() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        // Shared attribute "Node Control/Rebirth" = true. type = NCMD.
        boolean value = true;
        Assert.assertTrue(listKeys.contains(keyNodeRebirth));
        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + keyNodeRebirth + "\":" + value + "}";
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.ATTRIBUTES, 1);
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(keyNodeRebirth, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
    }

    /**
     * If boolean - send long 0 or 1
     * If String - try to parse long
     * If double - cast long
     * If we can't parse, cast, or JSON there - debug the message with the id of the devise/node, tenant,
     * the name and type of the attribute into an error and don't send anything.
     */
    /**
     * 功能：处理值。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPublishNCMD_BooleanType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        MetricDataType metricDataType = MetricDataType.Boolean;
        String metricKey = "MyBoolean";
        Object metricValue = nextBoolean();
        connectionWithNBirth(metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.ATTRIBUTES, 1);

        // Boolean <-> String
        boolean expectedValue = true;
        String valueStr = "123";
        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        expectedValue = false;
        valueStr = "0";
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Boolean <-> Integer
        expectedValue = true;
        Integer valueInt = nextInt32();
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueInt + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        expectedValue = false;
        valueInt = 0;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueInt + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
    }

    /**
     * 功能：处理值。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPublishNCMD_LongType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        MetricDataType metricDataType = UInt32;
        String metricKey = "MyLong";
        Object metricValue = nextUInt32();
        connectionWithNBirth(metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.ATTRIBUTES, 1);

        // Long <-> String
        String valueStr = "123";
        long expectedValue = Long.valueOf(valueStr);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getLongValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Long <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = 1L;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getLongValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = 0L;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getLongValue());
    }

    /**
     * 功能：处理值。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPublishNCMD_FloatType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        MetricDataType metricDataType = MetricDataType.Float;
        String metricKey = "MyFloat";
        Object metricValue = nextFloat(30, 400);
        connectionWithNBirth(metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.ATTRIBUTES, 1);

        // Float <-> String
        String valueStr = "123.345";
        float expectedValue = Float.valueOf(valueStr);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getFloatValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Float <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = 1f;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getFloatValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = 0f;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getFloatValue());
    }

    /**
     * 功能：处理值。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPublishNCMD_DoubleType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        MetricDataType metricDataType = MetricDataType.Double;
        String metricKey = "MyDouble";
        Object metricValue = nextDouble();
        connectionWithNBirth(metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.ATTRIBUTES, 1);

        // Double <-> String
        String valueStr = "123345456";
        double expectedValue = Double.valueOf(valueStr);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getDoubleValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Double <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = 1d;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getDoubleValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = 0d;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getDoubleValue());
    }

    /**
     * 功能：处理值。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenPublishNCMD_StringType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        MetricDataType metricDataType = MetricDataType.String;
        String metricKey = "MyString";
        Object metricValue = nextString();
        connectionWithNBirth(metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/" + NCMD.name() + "/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.ATTRIBUTES, 1);

        // String <-> Long
        long valueLong = 123345456L;
        String expectedValue = String.valueOf(valueLong);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueLong + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getStringValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // String <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = "true";
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getStringValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = "false";
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getStringValue());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttribute() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1, ts);

        // Integer <-> Integer
        int expectedValueInt = 123456;

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricBirthName_Int32 + "\":" + expectedValueInt + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + devices.get(0).getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.DBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValueInt, mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    /**
     * 功能：处理设备。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttributes_LongType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1, ts);

        // Int <-> String
        String valueStr = "123";
        long expectedValue = Long.valueOf(valueStr);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricBirthName_Int32 + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + devices.get(0).getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.DBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Int <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = 1;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricBirthName_Int32 + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + devices.get(0).getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = 0;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricBirthName_Int32 + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + devices.get(0).getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    /**
     * 功能：处理配置。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientNodeWithCorrectAccessTokenPublish_AttributesInProfileContainsKeyAttributes() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        String urlTemplate = "/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/keys/attributes/" + SHARED_SCOPE;
        AtomicReference<List<String>> actualKeys = new AtomicReference<>();
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    actualKeys.set(doGetAsyncTyped(urlTemplate, new TypeReference<>() {
                    }));
                    return actualKeys.get().size() == 1;
                });
        Assert.assertEquals(metricBirthName_Int32, actualKeys.get().get(0));
    }

    /**
     * 功能：处理设备配置。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientDeviceWithCorrectAccessTokenPublish_AttributesInProfileContainsKeyAttributes() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1, ts);
        String urlTemplate = "/api/plugins/telemetry/DEVICE/" + devices.get(0).getId().getId() + "/keys/attributes/" + SHARED_SCOPE;
        AtomicReference<List<String>> actualKeys = new AtomicReference<>();
        await(alias + SparkplugMessageType.DBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    actualKeys.set(doGetAsyncTyped(urlTemplate, new TypeReference<>() {
                    }));
                    return actualKeys.get().size() == 1;
                });
        Assert.assertEquals(metricBirthName_Int32, actualKeys.get().get(0));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttV5ClientSparkplugAttributesTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
