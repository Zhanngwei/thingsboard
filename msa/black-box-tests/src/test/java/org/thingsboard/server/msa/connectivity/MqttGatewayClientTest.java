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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultGatewayPrototype;

/**
 * 中文说明：
 * 1. `MqttGatewayClientTest` 是 ThingsBoard Microservices 中验证 `MqttGatewayClient` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractContainerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DisableUIListeners
@Slf4j
public class MqttGatewayClientTest extends AbstractContainerTest {
    /**
     * 设备对象，用于描述当前业务场景。
     */
    private Device gatewayDevice;
    private MqttClient mqttClient;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    private Device createdDevice;
    private MqttMessageListener listener;
    private JsonParser jsonParser = new JsonParser();

    /**
     * 处理器列表，用于保存一组待处理对象。
     */
    AbstractListeningExecutor handlerExecutor;

    /**
     * 功能：保存或创建`Gateway`。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void createGateway() throws Exception {
        this.handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 4;
            }
        };
        handlerExecutor.init();

        testRestClient.login("tenant@thingsboard.org", "tenant");
        gatewayDevice = testRestClient.postDevice("", defaultGatewayPrototype());
        DeviceCredentials gatewayDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(gatewayDevice.getId());

        this.listener = new MqttMessageListener();
        this.mqttClient = getMqttClient(gatewayDeviceCredentials, listener);
        this.createdDevice = createDeviceThroughGateway(mqttClient, gatewayDevice);
    }

    /**
     * 功能：删除或清理`Gateway`。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void removeGateway()  {
        testRestClient.deleteDeviceIfExists(this.gatewayDevice.getId());
        testRestClient.deleteDeviceIfExists(this.createdDevice.getId());
        this.listener = null;
        this.mqttClient = null;
        this.createdDevice = null;
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
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), -1).toString().getBytes())).get();
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

        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), ts).toString().getBytes())).get();
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
     * 功能：发送或提交属性。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void publishAttributeUpdateToServer() throws Exception {
        testRestClient.getDeviceCredentialsByDeviceId(createdDevice.getId());

        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        JsonObject clientAttributes = new JsonObject();
        clientAttributes.addProperty("attr1", "value1");
        clientAttributes.addProperty("attr2", true);
        clientAttributes.addProperty("attr3", 42.0);
        clientAttributes.addProperty("attr4", 73);
        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received attributes: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("attr1", "attr2", "attr3", "attr4"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("attr1").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr2").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr3").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr4").get(1)).isEqualTo(Long.toString(73));
    }

    /**
     * 功能：执行 `responseDataOnAttributesRequestCheck` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void responseDataOnAttributesRequestCheck() throws Exception {
        testRestClient.getDeviceCredentialsByDeviceId(createdDevice.getId());
        JsonObject sharedAttributes = new JsonObject();
        sharedAttributes.addProperty("attr1", "value1");
        sharedAttributes.addProperty("attr2", true);
        sharedAttributes.addProperty("attr3", 42.0);
        sharedAttributes.addProperty("attr4", 73);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();

        testRestClient.postTelemetryAttribute(DataConstants.DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));
        var event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        JsonObject requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        requestData.addProperty("key", "attr1");

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        JsonObject responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();
        assertThat(responseData.has("value")).isTrue();
        assertThat(responseData.get("value").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());

        requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        JsonArray keys = new JsonArray();
        keys.add("attr1");
        keys.add("attr2");
        requestData.add("keys", keys);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();

        assertThat(responseData.has("values")).isTrue();
        assertThat(responseData.get("values").getAsJsonObject().get("attr1").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());
        assertThat(responseData.get("values").getAsJsonObject().get("attr2").getAsString()).isEqualTo(sharedAttributes.get("attr2").getAsString());

        requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        keys = new JsonArray();
        keys.add("attr1");
        keys.add("undefined");
        requestData.add("keys", keys);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();

        assertThat(responseData.has("values")).isTrue();
        assertThat(responseData.get("values").getAsJsonObject().get("attr1").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());
        assertThat(responseData.get("values").getAsJsonObject().entrySet()).hasSize(1);
    }

    /**
     * 功能：执行 `requestAttributeValuesFromServer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void requestAttributeValuesFromServer() throws Exception {
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        // Add a new client attribute
        JsonObject clientAttributes = new JsonObject();
        String clientAttributeValue = StringUtils.randomAlphanumeric(8);
        clientAttributes.addProperty("clientAttr", clientAttributeValue);

        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();

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

        // Subscribe for attribute update event
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));
        MqttEvent sharedAttributeEvent = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        // Catch attribute update event
        assertThat(sharedAttributeEvent).isNotNull();
        assertThat(sharedAttributeEvent.getTopic()).isEqualTo("v1/gateway/attributes");

        // Subscribe to attributes response
        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        checkAttribute(true, clientAttributeValue);
        checkAttribute(false, sharedAttributeValue);
    }

    /**
     * 功能：订阅属性。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void subscribeToAttributeUpdatesFromServer() throws Exception {
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();
        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);
        String sharedAttributeName = "sharedAttr";
        // Add a new shared attribute

        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty(sharedAttributeName, sharedAttributeValue);

        JsonObject gatewaySharedAttributeValue = new JsonObject();
        gatewaySharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewaySharedAttributeValue.add("data", sharedAttributes);

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));

        MqttEvent event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText())
                .isEqualTo(sharedAttributeValue);

        // Update the shared attribute value
        JsonObject updatedSharedAttributes = new JsonObject();
        String updatedSharedAttributeValue = StringUtils.randomAlphanumeric(8);
        updatedSharedAttributes.addProperty(sharedAttributeName, updatedSharedAttributeValue);

        JsonObject gatewayUpdatedSharedAttributeValue = new JsonObject();
        gatewayUpdatedSharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewayUpdatedSharedAttributeValue.add("data", updatedSharedAttributes);

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(updatedSharedAttributes.toString()));
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText())
                .isEqualTo(updatedSharedAttributeValue);
    }

    /**
     * 功能：执行 `serverSideRpc` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void serverSideRpc() throws Exception {
        String gatewayRpcTopic = "v1/gateway/rpc";
        mqttClient.on(gatewayRpcTopic, listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Send an RPC from the server
        JsonObject serverRpcPayload = new JsonObject();
        serverRpcPayload.addProperty("method", "getValue");
        serverRpcPayload.addProperty("params", true);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName())));
        ListenableFuture<JsonNode> future = service.submit(() -> {
            try {
                return testRestClient.postServerSideRpc(createdDevice.getId(), mapper.readTree(serverRpcPayload.toString()));
            } catch (IOException e) {
                return null;
            }
        });

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        service.shutdownNow();

        assertThat(requestFromServer).isNotNull();
        assertThat(requestFromServer.getMessage()).isNotNull();
        JsonNode requestFromServerJson = JacksonUtil.toJsonNode(requestFromServer.getMessage());
        assertThat(requestFromServerJson.get("device").asText()).isEqualTo(createdDevice.getName());
        assertThat(requestFromServerJson.get("data").get("method").asText()).isEqualTo("getValue");
        assertThat(requestFromServerJson.get("data").get("params").asText()).isEqualTo("true");
        int requestId = requestFromServerJson.get("data").get("id").asInt();

        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        JsonObject gatewayResponse = new JsonObject();
        gatewayResponse.addProperty("device", createdDevice.getName());
        gatewayResponse.addProperty("id", requestId);
        gatewayResponse.add("data", clientResponse);
        // Send a response to the server's RPC request

        mqttClient.publish(gatewayRpcTopic, Unpooled.wrappedBuffer(gatewayResponse.toString().getBytes())).get();
        JsonNode serverResponse = future.get(5 * timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(serverResponse).isEqualTo(mapper.readTree(clientResponse.toString()));
    }

    /**
     * 功能：执行 `deviceCreationAfterDeleted` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void deviceCreationAfterDeleted() throws Exception {
        testRestClient.deleteDevice(this.createdDevice.getId());
        testRestClient.getDeviceById(this.createdDevice.getId(), HttpStatus.NOT_FOUND.value());
        this.createdDevice = createDeviceThroughGateway(mqttClient, gatewayDevice);
    }

    /**
     * 功能：校验属性。
     * 参数：
     * - `client`：客户端对象。
     * - `expectedValue`：值。
     * 返回：无。
     */
    private void checkAttribute(boolean client, String expectedValue) throws Exception {
        JsonObject gatewayAttributesRequest = new JsonObject();
        int messageId = new Random().nextInt(100);
        gatewayAttributesRequest.addProperty("id", messageId);
        gatewayAttributesRequest.addProperty("device", createdDevice.getName());
        gatewayAttributesRequest.addProperty("client", client);
        String attributeName;
        if (client)
            attributeName = "clientAttr";
        else
            attributeName = "sharedAttr";
        gatewayAttributesRequest.addProperty("key", attributeName);
        log.info(gatewayAttributesRequest.toString());
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(gatewayAttributesRequest.toString().getBytes())).get();
        MqttEvent clientAttributeEvent = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(clientAttributeEvent).isNotNull();
        JsonObject responseMessage = new JsonParser().parse(Objects.requireNonNull(clientAttributeEvent).getMessage()).getAsJsonObject();

        assertThat(responseMessage.get("id").getAsInt()).isEqualTo(messageId);
        assertThat(responseMessage.get("device").getAsString()).isEqualTo(createdDevice.getName());
        assertThat(responseMessage.entrySet()).hasSize(3);
        assertThat(responseMessage.get("value").getAsString()).isEqualTo(expectedValue);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `mqttClient`：客户端对象。
     * - `gatewayDevice`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private Device createDeviceThroughGateway(MqttClient mqttClient, Device gatewayDevice) throws Exception {
        if (timeoutMultiplier > 1) {
            TimeUnit.SECONDS.sleep(30);
        }

        String deviceName = "mqtt_device" + RandomStringUtils.randomAlphabetic(5);
        mqttClient.publish("v1/gateway/connect", Unpooled.wrappedBuffer(createGatewayConnectPayload(deviceName).toString().getBytes()), MqttQoS.AT_LEAST_ONCE).get();

        if (timeoutMultiplier > 1) {
            TimeUnit.SECONDS.sleep(30);
        }

        List<EntityRelation> relations = testRestClient.findRelationByFrom(gatewayDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).hasSize(1);

        EntityId createdEntityId = relations.get(0).getTo();
        DeviceId createdDeviceId = new DeviceId(createdEntityId.getId());
        return testRestClient.getDeviceById(createdDeviceId);
    }

    /**
     * 功能：获取`Owner Id`。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getOwnerId() {
        return "Tenant[" + gatewayDevice.getTenantId().getId() + "]MqttGatewayClientTestDevice[" + gatewayDevice.getId().getId() + "]";
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * - `listener`：数据列表。
     * 返回：处理结果。
     */
    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId(getOwnerId());
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername(deviceCredentials.getCredentialsId());
        MqttClient mqttClient = MqttClient.create(clientConfig, listener, handlerExecutor);
        mqttClient.connect("localhost", 1883).get();
        return mqttClient;
    }

    /**
     * 中文说明：
     * 1. `MqttMessageListener` 是 ThingsBoard Microservices 中处理消息的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `MqttHandler`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    @Data
    private class MqttMessageListener implements MqttHandler {
        /**
         * `events` 字段，保存当前对象的对应属性。
         */
        private final BlockingQueue<MqttEvent> events;

        /**
         * 功能：创建 `MqttGatewayClientTest` 实例，并初始化必要字段。
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
     * 1. `MqttEvent` 是 ThingsBoard Microservices 中承载事件信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
