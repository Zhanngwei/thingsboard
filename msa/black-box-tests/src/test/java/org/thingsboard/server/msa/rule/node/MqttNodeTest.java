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
package org.thingsboard.server.msa.rule.node;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.TestProperties;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

/**
 * 中文说明：
 * 1. `MqttNodeTest` 是 ThingsBoard Microservices 中验证 `MqttNode` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractContainerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DisableUIListeners
@Slf4j
public class MqttNodeTest extends AbstractContainerTest {

    /**
     * 主题常量，用于统一引用固定值。
     */
    private static final String TOPIC = "tb/mqtt/device";

    /**
     * 设备对象，用于描述当前业务场景。
     */
    private Device device;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeMethod
    public void setUp() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("mqtt_"));
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    /**
     * 功能：执行 `telemetryUpload` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void telemetryUpload() throws Exception {
        RuleChainId defaultRuleChainId = getDefaultRuleChainId();

        createRootRuleChainWithTestNode("MqttRuleNodeTestMetadata.json", "org.thingsboard.rule.engine.mqtt.TbMqttNode", 2);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        MqttMessageListener messageListener = new MqttMessageListener();
        MqttClient responseClient = new MqttClient(TestProperties.getMqttBrokerUrl(), StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        responseClient.connect();
        responseClient.subscribe(TOPIC, messageListener);

        MqttClient mqttClient = new MqttClient("tcp://localhost:1883", StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(deviceCredentials.getCredentialsId());
        mqttClient.connect(mqttConnectOptions);
        mqttClient.publish("v1/devices/me/telemetry", new MqttMessage(createPayload().toString().getBytes()));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));

        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> messageListener.getEvents().size() > 0);

        BlockingQueue<MqttEvent> events = messageListener.getEvents();
        JsonNode actual = JacksonUtil.toJsonNode(Objects.requireNonNull(events.poll()).message);

        assertThat(actual.get("stringKey").asText()).isEqualTo("value1");
        assertThat(actual.get("booleanKey").asBoolean()).isEqualTo(Boolean.TRUE);
        assertThat(actual.get("doubleKey").asDouble()).isEqualTo(42.0);
        assertThat(actual.get("longKey").asLong()).isEqualTo(73);

        testRestClient.setRootRuleChain(defaultRuleChainId);
    }

    /**
     * 中文说明：
     * 1. `MqttMessageListener` 是 ThingsBoard Microservices 中处理消息的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `IMqttMessageListener`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    @Data
    private class MqttMessageListener implements IMqttMessageListener {
        /**
         * `events` 字段，保存当前对象的对应属性。
         */
        private final BlockingQueue<MqttEvent> events;

        /**
         * 功能：创建 `MqttNodeTest` 实例，并初始化必要字段。
         * 参数：无。
         * 返回：新创建的对象实例。
         */
        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        /**
         * 功能：执行 `messageArrived` 对应的处理。
         * 参数：
         * - `s`：`s` 参数。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) {
            log.info("MQTT message [{}], topic [{}]", mqttMessage.toString(), s);
            events.add(new MqttEvent(s, mqttMessage.toString()));
        }

        /**
         * 功能：获取`Events`。
         * 参数：无。
         * 返回：处理结果。
         */
        public BlockingQueue<MqttEvent> getEvents() {
            return events;
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
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChainMetadataFile`：待处理数据。
     * - `ruleNodeType`：类型。
     * - `eventsCount`：`eventsCount` 参数。
     * 返回：处理结果。
     */
    protected RuleChainId createRootRuleChainWithTestNode(String ruleChainMetadataFile, String ruleNodeType, int eventsCount) throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");
        RuleChain ruleChain = testRestClient.postRuleChain(newRuleChain);

        JsonNode configuration = JacksonUtil.OBJECT_MAPPER.readTree(this.getClass().getClassLoader().getResourceAsStream(ruleChainMetadataFile));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        ruleChainMetaData.setConnections(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));

        ruleChainMetaData = testRestClient.postRuleChainMetadata(ruleChainMetaData);

        testRestClient.setRootRuleChain(ruleChain.getId());

        RuleNode node = ruleChainMetaData.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals(ruleNodeType)).findFirst().get();

        Awaitility
                .await()
                .alias("Get events from rule chain")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(node.getId(), EventType.LC_EVENT, ruleChain.getTenantId(), new TimePageLink(1024));
                    List<EventInfo> eventInfos = events.getData().stream().filter(eventInfo ->
                                    "STARTED".equals(eventInfo.getBody().get("event").asText()) &&
                                            "true".equals(eventInfo.getBody().get("success").asText()))
                            .collect(Collectors.toList());

                    return eventInfos.size() == eventsCount;
                });

        return ruleChain.getId();
    }
}
