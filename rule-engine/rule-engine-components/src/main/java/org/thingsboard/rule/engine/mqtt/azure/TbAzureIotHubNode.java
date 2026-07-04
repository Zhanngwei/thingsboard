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
package org.thingsboard.rule.engine.mqtt.azure;

import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.rule.engine.mqtt.TbMqttNode;
import org.thingsboard.rule.engine.mqtt.TbMqttNodeConfiguration;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentType;

import javax.net.ssl.SSLException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "azure iot hub",
        configClazz = TbAzureIotHubNodeConfiguration.class,
        clusteringMode = ComponentClusteringMode.SINGLETON,
        nodeDescription = "Publish messages to the Azure IoT Hub",
        nodeDetails = "Will publish message payload to the Azure IoT Hub with QoS <b>AT_LEAST_ONCE</b>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeAzureIotHubConfig"
)
/**
 * Azure IoT Hub MQTT 发布节点，复用 TbMqttNode 的 MQTT 客户端发布流程并补充 Azure 特定连接参数。
 * 本类直接影响 MQTT 客户端配置和连接生命周期；实际发布、QoS 和 Rule Engine 确认关系由父类实现。
 */
public class TbAzureIotHubNode extends TbMqttNode {
    /**
     * 初始化 Azure IoT Hub 节点并强制使用 8883、cleanSession 和 Azure 默认 CA 证书。
     * 本方法直接创建 MQTT 客户端连接；Topic 模板和凭据来自节点配置，QoS 固定逻辑继承自父类。
     * 本方法本身不直接访问数据库或缓存，Azure 证书/凭据解析由配置对象和工具类完成。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.mqttNodeConfiguration = TbNodeUtils.convert(configuration, TbMqttNodeConfiguration.class);
        try {
            mqttNodeConfiguration.setPort(8883);
            mqttNodeConfiguration.setCleanSession(true);
            ClientCredentials credentials = mqttNodeConfiguration.getCredentials();
            if (CredentialsType.CERT_PEM == credentials.getType()) {
                CertPemCredentials pemCredentials = (CertPemCredentials) credentials;
                if (pemCredentials.getCaCert() == null || pemCredentials.getCaCert().isEmpty()) {
                    pemCredentials.setCaCert(AzureIotHubUtil.getDefaultCaCert());
                }
            }
            this.mqttClient = initClient(ctx);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    /**
     * 准备 Azure IoT Hub 所需的 MQTT 协议版本、用户名和 SAS 密码。
     * 本方法只写入 MQTT 客户端配置，不直接发布消息；连接建立仍由父类 initClient 完成。
     */
    protected void prepareMqttClientConfig(MqttClientConfig config) throws SSLException {
        config.setProtocolVersion(MqttVersion.MQTT_3_1_1);
        config.setUsername(AzureIotHubUtil.buildUsername(mqttNodeConfiguration.getHost(), config.getClientId()));
        ClientCredentials credentials = mqttNodeConfiguration.getCredentials();
        if (CredentialsType.SAS == credentials.getType()) {
            config.setPassword(AzureIotHubUtil.buildSasToken(mqttNodeConfiguration.getHost(), ((AzureIotHubSasCredentials) credentials).getSasKey()));
        }
    }
}

/*
 * 本类总结：
 * 本类是 Azure IoT Hub 的 MQTT 特化节点，直接复用父类 MQTT 客户端连接与发布机制，并覆盖 Azure 所需的 TLS、用户名和 SAS Token 配置。
 * Rule Engine 消息确认、AT_LEAST_ONCE QoS、Topic 解析和成功/失败路由均沿用 TbMqttNode。
 * 本类本身不直接涉及数据库或缓存，相关依赖只可能通过 Rule Engine 上下文或凭据工具链间接涉及。
 */
