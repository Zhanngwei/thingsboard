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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
        "transport.mqtt.enabled=true",
})
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractMqttIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public abstract class AbstractMqttIntegrationTest extends AbstractTransportIntegrationTest {

    /**
     * 字段说明：
     * 1. 保存 `savedGateway` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected Device savedGateway;
    protected String gatewayAccessToken;

    /**
     * 方法说明：
     * 1. 职责：执行 `processBeforeTest` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processBeforeTest(MqttTestConfigProperties config) throws Exception {
        loginTenantAdmin();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        deviceProfile = createMqttDeviceProfile(config);
        assertNotNull(deviceProfile);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (config.getDeviceName() != null) {
            savedDevice = createDevice(config.getDeviceName(), deviceProfile.getName(), false);
            DeviceCredentials deviceCredentials =
                    doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
            assertNotNull(deviceCredentials);
            assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
            accessToken = deviceCredentials.getCredentialsId();
            assertNotNull(accessToken);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (config.getGatewayName() != null) {
            savedGateway = createDevice(config.getGatewayName(), deviceProfile.getName(), true);
            DeviceCredentials gatewayCredentials =
                    doGet("/api/device/" + savedGateway.getId().getId().toString() + "/credentials", DeviceCredentials.class);
            assertNotNull(gatewayCredentials);
            assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
            gatewayAccessToken = gatewayCredentials.getCredentialsId();
            assertNotNull(gatewayAccessToken);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createMqttDeviceProfile` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected DeviceProfile createMqttDeviceProfile(MqttTestConfigProperties config) throws Exception {
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        TransportPayloadType transportPayloadType = config.getTransportPayloadType();
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        if (transportPayloadType == null) {
            DeviceProfileInfo defaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
            return doGet("/api/deviceProfile/" + defaultDeviceProfileInfo.getId().getId(), DeviceProfile.class);
        } else {
            DeviceProfile deviceProfile = new DeviceProfile();
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            deviceProfile.setName(transportPayloadType.name());
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            deviceProfile.setTransportType(DeviceTransportType.MQTT);
            DeviceProfileProvisionType provisionType = config.getProvisionType() != null ?
                    config.getProvisionType() : DeviceProfileProvisionType.DISABLED;
            deviceProfile.setProvisionType(provisionType);
            deviceProfile.setProvisionDeviceKey(config.getProvisionKey());
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            deviceProfile.setDescription(transportPayloadType.name() + " Test");
            DeviceProfileData deviceProfileData = new DeviceProfileData();
            DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
            MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (StringUtils.hasLength(config.getTelemetryTopicFilter())) {
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(config.getTelemetryTopicFilter());
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (StringUtils.hasLength(config.getAttributesTopicFilter())) {
                mqttDeviceProfileTransportConfiguration.setDeviceAttributesTopic(config.getAttributesTopicFilter());
            }
            mqttDeviceProfileTransportConfiguration.setSparkplug(config.isSparkplug());
            mqttDeviceProfileTransportConfiguration.setSparkplugAttributesMetricNames(config.sparkplugAttributesMetricNames);
            mqttDeviceProfileTransportConfiguration.setSendAckOnValidationException(config.isSendAckOnValidationException());
            TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
            if (TransportPayloadType.JSON.equals(transportPayloadType)) {
                transportPayloadTypeConfiguration = new JsonTransportPayloadConfiguration();
            } else {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
                String telemetryProtoSchema = config.getTelemetryProtoSchema();
                String attributesProtoSchema = config.getAttributesProtoSchema();
                String rpcResponseProtoSchema = config.getRpcResponseProtoSchema();
                String rpcRequestProtoSchema = config.getRpcRequestProtoSchema();
                protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(
                        telemetryProtoSchema != null ? telemetryProtoSchema : DEVICE_TELEMETRY_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(
                        attributesProtoSchema != null ? attributesProtoSchema : DEVICE_ATTRIBUTES_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(
                        rpcResponseProtoSchema != null ? rpcResponseProtoSchema : DEVICE_RPC_RESPONSE_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(
                        rpcRequestProtoSchema != null ? rpcRequestProtoSchema : DEVICE_RPC_REQUEST_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setEnableCompatibilityWithJsonPayloadFormat(
                        config.isEnableCompatibilityWithJsonPayloadFormat()
                );
                protoTransportPayloadConfiguration.setUseJsonPayloadFormatForDefaultDownlinkTopics(
                        config.isEnableCompatibilityWithJsonPayloadFormat() &&
                                config.isUseJsonPayloadFormatForDefaultDownlinkTopics()
                );
                transportPayloadTypeConfiguration = protoTransportPayloadConfiguration;
            }
            mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
            deviceProfileData.setTransportConfiguration(mqttDeviceProfileTransportConfiguration);
            DeviceProfileProvisionConfiguration provisionConfiguration;
            switch (provisionType) {
                case ALLOW_CREATE_NEW_DEVICES:
                    provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case CHECK_PRE_PROVISIONED_DEVICES:
                    provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case DISABLED:
                default:
                    provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
            }
            deviceProfileData.setProvisionConfiguration(provisionConfiguration);
            deviceProfileData.setConfiguration(configuration);
            deviceProfile.setProfileData(deviceProfileData);
            deviceProfile.setDefault(false);
            deviceProfile.setDefaultRuleChainId(null);
            return doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDevice` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Device createDevice(String name, String type, boolean gateway) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        if (gateway) {
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("gateway", true);
            device.setAdditionalInfo(additionalInfo);
        }
        return doPost("/api/device", device, Device.class);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getPostAttributeMsg` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected TransportProtos.PostAttributeMsg getPostAttributeMsg(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
        builder.addAllKv(kvProtos);
        return builder.build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `subscribeAndWait` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void subscribeAndWait(MqttTestClient client, String attrSubTopic, DeviceId deviceId, FeatureType featureType) throws MqttException {
        subscribeAndWait(client, attrSubTopic, deviceId, featureType, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `subscribeAndWait` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void subscribeAndWait(MqttTestClient client, String attrSubTopic, DeviceId deviceId, FeatureType featureType, MqttQoS mqttQoS) throws MqttException {
        int subscriptionCount = getDeviceActorSubscriptionCount(deviceId, featureType);
        client.subscribeAndWait(attrSubTopic, mqttQoS);
        // TODO: This test awaits for the device actor to receive the subscription. Ideally it should not happen. See details below:
        // The transport layer acknowledge subscription request once the message about subscription is in the queue.
        // Test sends data immediately after acknowledgement.
        // But there is a time lag between push to the queue and read from the queue in the tb-core component.
        // Ideally, we should reply to device with SUBACK only when the device actor on the tb-core receives the message.
        awaitForDeviceActorToReceiveSubscription(deviceId, featureType, subscriptionCount + 1);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `subscribeAndCheckSubscription` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void subscribeAndCheckSubscription(MqttTestClient client, String attrSubTopic, DeviceId deviceId, FeatureType featureType) throws MqttException {
        client.subscribeAndWait(attrSubTopic, MqttQoS.AT_MOST_ONCE);
        // TODO: This test awaits for the device actor to receive the subscription. Ideally it should not happen. See details below:
        // The transport layer acknowledge subscription request once the message about subscription is in the queue.
        // Test sends data immediately after acknowledgement.
        // But there is a time lag between push to the queue and read from the queue in the tb-core component.
        // Ideally, we should reply to device with SUBACK only when the device actor on the tb-core receives the message.
        awaitForDeviceActorToReceiveSubscription(deviceId, featureType, 1);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractMqttIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
