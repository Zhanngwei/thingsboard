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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.AttributesResponse;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

/**
 * 中文说明：
 * 1. 类目的：`MqttClientTest` 是 ThingsBoard MSA 测试模块 中的协议连通性测试类型，用于验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Integration Test / Client Adapter。
 */
@DisableUIListeners
@Slf4j
public class MqttClientTest extends AbstractContainerTest {

    /**
     * 设备对象，用于描述当前业务场景。
     */
    private Device device;
    AbstractListeningExecutor handlerExecutor;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void setUp() throws Exception {
        this.handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 4;
            }
        };
        handlerExecutor.init();

        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
        if (handlerExecutor != null) {
            handlerExecutor.destroy();
        }
    }
    /**
     * 功能：执行 `telemetryUpload` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void telemetryUpload() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        MqttClient mqttClient = getMqttClient(deviceCredentials, null);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload().toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    /**
     * 功能：执行 `telemetryUploadWithTs` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void telemetryUploadWithTs() throws Exception {
        long ts = 1451649600512L;
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        MqttClient mqttClient = getMqttClient(deviceCredentials, null);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload(ts).toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(getExpectedLatestValues(ts)).isEqualTo(actualLatestTelemetry.getLatestValues());

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    /**
     * 功能：发送或提交属性。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void publishAttributeUpdateToServer() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        JsonObject clientAttributes = new JsonObject();
        clientAttributes.addProperty("attr1", "value1");
        clientAttributes.addProperty("attr2", true);
        clientAttributes.addProperty("attr3", 42.0);
        clientAttributes.addProperty("attr4", 73);
        mqttClient.publish("v1/devices/me/attributes", Unpooled.wrappedBuffer(clientAttributes.toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("attr1", "attr2", "attr3", "attr4"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("attr1").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr2").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr3").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr4").get(1)).isEqualTo(Long.toString(73));
    }

    /**
     * 功能：执行 `requestAttributeValuesFromServer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void requestAttributeValuesFromServer() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);

        // Add a new client attribute
        JsonObject clientAttributes = new JsonObject();
        String clientAttributeValue = StringUtils.randomAlphanumeric(8);
        clientAttributes.addProperty("clientAttr", clientAttributeValue);
        mqttClient.publish("v1/devices/me/attributes", Unpooled.wrappedBuffer(clientAttributes.toString().getBytes())).get();

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received ws telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(1);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnly("clientAttr");
        assertThat(actualLatestTelemetry.getDataValuesByKey("clientAttr").get(1)).isEqualTo(clientAttributeValue);

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty("sharedAttr", sharedAttributeValue);
        JsonNode sharedAttribute = mapper.readTree(sharedAttributes.toString());
        testRestClient.postTelemetryAttribute(DataConstants.DEVICE, device.getId(), SHARED_SCOPE, sharedAttribute);

        // Subscribe to attributes response
        mqttClient.on("v1/devices/me/attributes/response/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Request attributes
        JsonObject request = new JsonObject();
        request.addProperty("clientKeys", "clientAttr");
        request.addProperty("sharedKeys", "sharedAttr");
        mqttClient.publish("v1/devices/me/attributes/request/" + new Random().nextInt(100), Unpooled.wrappedBuffer(request.toString().getBytes())).get();
        MqttEvent event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        AttributesResponse attributes = mapper.readValue(Objects.requireNonNull(event).getMessage(), AttributesResponse.class);
        log.info("Received telemetry: {}", attributes);

        assertThat(attributes.getClient()).hasSize(1);
        assertThat(attributes.getClient().get("clientAttr")).isEqualTo(clientAttributeValue);

        assertThat(attributes.getShared()).hasSize(1);
        assertThat(attributes.getShared().get("sharedAttr")).isEqualTo(sharedAttributeValue);
    }

    /**
     * 功能：订阅属性。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void subscribeToAttributeUpdatesFromServer() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        String sharedAttributeName = "sharedAttr";

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty(sharedAttributeName, sharedAttributeValue);
        JsonNode sharedAttribute = mapper.readTree(sharedAttributes.toString());

        testRestClient.postTelemetryAttribute(DataConstants.DEVICE, device.getId(), SHARED_SCOPE, sharedAttribute);

        MqttEvent event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get(sharedAttributeName).asText())
                .isEqualTo(sharedAttributeValue);

        // Update the shared attribute value
        JsonObject updatedSharedAttributes = new JsonObject();
        String updatedSharedAttributeValue = StringUtils.randomAlphanumeric(8);
        updatedSharedAttributes.addProperty(sharedAttributeName, updatedSharedAttributeValue);
        testRestClient.postTelemetryAttribute(DEVICE, device.getId(), SHARED_SCOPE, mapper.readTree(updatedSharedAttributes.toString()));

        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get(sharedAttributeName).asText())
                .isEqualTo(updatedSharedAttributeValue);
    }

    /**
     * 功能：执行 `serverSideRpc` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void serverSideRpc() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/rpc/request/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Send an RPC from the server
        JsonObject serverRpcPayload = new JsonObject();
        serverRpcPayload.addProperty("method", "getValue");
        serverRpcPayload.addProperty("params", true);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName())));
        ListenableFuture<JsonNode> future = service.submit(() -> {
            try {
                return testRestClient.postServerSideRpc(device.getId(), mapper.readTree(serverRpcPayload.toString()));
            } catch (IOException e) {
                return null;
            }
        });

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(Objects.requireNonNull(requestFromServer).getMessage()).isEqualTo("{\"method\":\"getValue\",\"params\":true}");

        Integer requestId = Integer.valueOf(Objects.requireNonNull(requestFromServer).getTopic().substring("v1/devices/me/rpc/request/".length()));
        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        // Send a response to the server's RPC request
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, Unpooled.wrappedBuffer(clientResponse.toString().getBytes())).get();

        JsonNode serverResponse = future.get(5 * timeoutMultiplier, TimeUnit.SECONDS);
        service.shutdownNow();
        assertThat(serverResponse).isEqualTo(mapper.readTree(clientResponse.toString()));
    }

    /**
     * 功能：执行 `clientSideRpc` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void clientSideRpc() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/rpc/request/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Get the default rule chain id to make it root again after test finished
        RuleChainId defaultRuleChainId = getDefaultRuleChainId();

        // Create a new root rule chain
        RuleChainId ruleChainId = createRootRuleChainForRpcResponse();

        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);
        // Send the request to the server
        JsonObject clientRequest = new JsonObject();
        clientRequest.addProperty("method", "getResponse");
        clientRequest.addProperty("params", true);
        Integer requestId = 42;
        mqttClient.publish("v1/devices/me/rpc/request/" + requestId, Unpooled.wrappedBuffer(clientRequest.toString().getBytes())).get();

        // Check the response from the server
        TimeUnit.SECONDS.sleep(1 * timeoutMultiplier);
        MqttEvent responseFromServer = listener.getEvents().poll(1 * timeoutMultiplier, TimeUnit.SECONDS);
        Integer responseId = Integer.valueOf(Objects.requireNonNull(responseFromServer).getTopic().substring("v1/devices/me/rpc/response/".length()));
        assertThat(responseId).isEqualTo(requestId);
        assertThat(mapper.readTree(responseFromServer.getMessage()).get("response").asText()).isEqualTo("requestReceived");

        // Make the default rule chain a root again
        testRestClient.setRootRuleChain(defaultRuleChainId);

        // Delete the created rule chain
        testRestClient.deleteRuleChain(ruleChainId);
    }

    /**
     * 功能：执行 `deviceDeletedClosingSession` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void deviceDeletedClosingSession() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);

        testRestClient.deleteDeviceIfExists(device.getId());

        Awaitility
                .await()
                .alias("Check device connection.")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !mqttClient.isConnected());
    }

    /**
     * 功能：执行 `provisionRequestForDeviceWithPreProvisionedStrategy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionRequestForDeviceWithPreProvisionedStrategy() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient("provision", listener);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", device.getName());

        mqttClient.publish("/provision/request", Unpooled.wrappedBuffer(provisionRequest.toString().getBytes())).get();

        //Wait for response
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        MqttEvent provisionResponseMsg = listener.getEvents().poll(timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(provisionResponseMsg).isNotNull();

        JsonNode provisionResponse = mapper.readTree(provisionResponseMsg.getMessage());

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    /**
     * 功能：执行 `provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy() throws Exception {

        String testDeviceName = "test_provision_device";

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());

        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient("provision", listener);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", testDeviceName);

        mqttClient.publish("/provision/request", Unpooled.wrappedBuffer(provisionRequest.toString().getBytes())).get();

        //Wait for response
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        MqttEvent provisionResponseMsg = listener.getEvents().poll(timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(provisionResponseMsg).isNotNull();

        JsonNode provisionResponse = mapper.readTree(provisionResponseMsg.getMessage());

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    /**
     * 功能：执行 `provisionRequestForDeviceWithDisabledProvisioningStrategy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void provisionRequestForDeviceWithDisabledProvisioningStrategy() throws Exception {

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient("provision", listener);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);

        mqttClient.publish("/provision/request", Unpooled.wrappedBuffer(provisionRequest.toString().getBytes())).get();

        //Wait for response
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        MqttEvent provisionResponseMsg = listener.getEvents().poll(timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(provisionResponseMsg).isNotNull();

        JsonNode provisionResponse = mapper.readTree(provisionResponseMsg.getMessage());

        assertThat(provisionResponse.get("status").asText()).isEqualTo("NOT_FOUND");
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    private RuleChainId createRootRuleChainForRpcResponse() throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");

        RuleChain ruleChain = testRestClient.postRuleChain(newRuleChain);

        JsonNode configuration = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream("RpcResponseRuleChainMetadata.json"));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(mapper.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        ruleChainMetaData.setConnections(Arrays.asList(mapper.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));

        testRestClient.postRuleChainMetadata(ruleChainMetaData);

        // Set a new rule chain as root
        testRestClient.setRootRuleChain(ruleChain.getId());
        return ruleChain.getId();
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    private RuleChainId getDefaultRuleChainId() {
        PageData<RuleChain> ruleChains = testRestClient.getRuleChains(new PageLink(40, 0));

        Optional<RuleChain> defaultRuleChain = ruleChains.getData()
                .stream()
                .filter(RuleChain::isRoot)
                .findFirst();
        if (!defaultRuleChain.isPresent()) {
            fail("Root rule chain wasn't found");
        }
        return defaultRuleChain.get().getId();
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * - `listener`：数据列表。
     * 返回：处理结果。
     */
    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        return getMqttClient(deviceCredentials.getCredentialsId(), listener);
    }

    /**
     * 功能：获取`Owner Id`。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getOwnerId() {
        return "Tenant[" + device.getTenantId().getId() + "]MqttClientTestDevice[" + device.getId().getId() + "]";
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `username`：名称。
     * - `listener`：数据列表。
     * 返回：处理结果。
     */
    private MqttClient getMqttClient(String username, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId(getOwnerId());
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername(username);
        MqttClient mqttClient = MqttClient.create(clientConfig, listener, handlerExecutor);
        mqttClient.connect("localhost", 1883).get();
        return mqttClient;
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttMessageListener` 是 ThingsBoard MSA 测试模块 中的协议连通性测试类型，用于验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
     * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
     * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 Integration Test / Client Adapter。
     */
    @Data
    private class MqttMessageListener implements MqttHandler {
        /**
         * `events` 字段，保存当前对象的对应属性。
         */
        private final BlockingQueue<MqttEvent> events;

        /**
         * 功能：创建 `MqttClientTest` 实例，并初始化必要字段。
         * 参数：无。
         * 返回：新创建的对象实例。
         */
        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        /**
         * 功能：处理消息。
         * 参数：
         * - `topic`：主题名称或主题对象。
         * - `message`：待处理消息。
         * 返回：匹配的数据集合。
         */
        @Override
        public ListenableFuture<Void> onMessage(String topic, ByteBuf message) {
            log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
            events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
            return Futures.immediateVoidFuture();
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttEvent` 是 ThingsBoard MSA 测试模块 中的协议连通性测试类型，用于验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
     * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
     * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 Integration Test / Client Adapter。
     */
    @Data
    private class MqttEvent {
        /**
         * 主题，用于匹配或发送对应主题的数据。
         */
        private final String topic;
        private final String message;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttClientTest` 在 ThingsBoard MSA 测试模块 中承担协议连通性测试类型职责，核心目的是验证微服务环境下 MQTT、HTTP、CoAP、网关和设备上报链路。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
