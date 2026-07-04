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
package org.thingsboard.rule.engine.mqtt;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttConnectResult;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.net.ssl.SSLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "mqtt",
        configClazz = TbMqttNodeConfiguration.class,
        clusteringMode = ComponentClusteringMode.USER_PREFERENCE,
        nodeDescription = "Publish messages to the MQTT broker",
        nodeDetails = "Will publish message payload to the MQTT broker with QoS <b>AT_LEAST_ONCE</b>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeMqttConfig",
        icon = "call_split"
)
/**
 * MQTT 外部发布节点，直接持有并使用 ThingsBoard MQTT 客户端把 Rule Engine 消息载荷发布到外部 Broker。
 * 本类不直接访问数据库或缓存；Rule Engine 确认、失败路由和线程执行由 {@link TbAbstractExternalNode} 与 {@link TbContext} 调用链处理。
 */
public class TbMqttNode extends TbAbstractExternalNode {

    /**
     * MQTT 发布载荷使用的字符集，确保消息字符串按 UTF-8 转换为 Netty ByteBuf。
     */
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * 发布异常写入消息元数据时使用的键名。
     */
    private static final String ERROR = "error";

    /**
     * 当前节点配置，Topic、QoS 以外的连接参数、保留消息标志和凭据均来源于该配置。
     */
    protected TbMqttNodeConfiguration mqttNodeConfiguration;

    /**
     * 连接生命周期由本节点直接管理的 MQTT 客户端，init 建立连接，destroy 断开连接。
     */
    protected MqttClient mqttClient;

    /**
     * 初始化 MQTT 节点配置并立即创建 MQTT 客户端连接。
     * 本方法直接触达 MQTT 客户端连接生命周期；Topic、host、port、clientId、cleanSession、SSL 等均来自节点配置。
     * 本方法本身不直接访问数据库或缓存，Rule Engine/Actor 调用链可能在调度和上下文获取时涉及。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.mqttNodeConfiguration = TbNodeUtils.convert(configuration, TbMqttNodeConfiguration.class);
        try {
            this.mqttClient = initClient(ctx);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    /**
     * 处理 Rule Engine 消息并异步发布到 MQTT Broker。
     * Topic 从配置的 topicPattern 和消息内容解析，QoS 固定为 AT_LEAST_ONCE，retained 标志来自配置。
     * 调用 ackIfNeeded 后再发布，发布完成监听器根据 MQTT 客户端回调路由到 Success 或 Failure。
     * 本方法直接使用 MQTT 客户端；线程安全依赖 MQTT 客户端实现和 Rule Engine 对节点实例的调用约束。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String topic = TbNodeUtils.processPattern(this.mqttNodeConfiguration.getTopicPattern(), msg);
        var tbMsg = ackIfNeeded(ctx, msg);
        // MQTT publish 返回 Netty future，Rule Engine 消息的后续路由在该异步回调中完成。
        this.mqttClient.publish(topic, Unpooled.wrappedBuffer(tbMsg.getData().getBytes(UTF8)), MqttQoS.AT_LEAST_ONCE, mqttNodeConfiguration.isRetainedMessage())
                .addListener(future -> {
                            if (future.isSuccess()) {
                                tellSuccess(ctx, tbMsg);
                            } else {
                                tellFailure(ctx, processException(tbMsg, future.cause()), future.cause());
                            }
                        }
                );
    }

    /**
     * 将 MQTT 发布异常转写到消息元数据，供 Failure 路由上的后续节点读取。
     * 本方法本身不直接访问外部系统、数据库或缓存。
     */
    private TbMsg processException(TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 销毁节点时断开 MQTT 客户端连接。
     * 本方法直接管理 MQTT 连接生命周期，不负责 Rule Engine 消息确认。
     */
    @Override
    public void destroy() {
        if (this.mqttClient != null) {
            this.mqttClient.disconnect();
        }
    }

    /**
     * 构造 MQTT 客户端 ownerId，便于客户端日志和资源归属定位。
     * 本方法本身不直接访问 MQTT、数据库或缓存。
     */
    String getOwnerId(TbContext ctx) {
        return "Tenant[" + ctx.getTenantId().getId() + "]RuleNode[" + ctx.getSelf().getId().getId() + "]";
    }

    /**
     * 根据节点配置创建、配置并同步等待 MQTT 连接完成。
     * 本方法直接使用 MQTT 客户端；host、port、clientId、cleanSession、SSL 和认证均来自配置。
     * 连接超时或失败时会主动断开客户端并抛出异常，外层 init 将其转换为 TbNodeException。
     */
    protected MqttClient initClient(TbContext ctx) throws Exception {
        MqttClientConfig config = new MqttClientConfig(getSslContext());
        config.setOwnerId(getOwnerId(ctx));
        if (!StringUtils.isEmpty(this.mqttNodeConfiguration.getClientId())) {
            config.setClientId(this.mqttNodeConfiguration.isAppendClientIdSuffix() ?
                    this.mqttNodeConfiguration.getClientId() + "_" + ctx.getServiceId() : this.mqttNodeConfiguration.getClientId());
        }
        config.setCleanSession(this.mqttNodeConfiguration.isCleanSession());

        prepareMqttClientConfig(config);
        MqttClient client = MqttClient.create(config, null, ctx.getExternalCallExecutor());
        client.setEventLoop(ctx.getSharedEventLoop());
        Promise<MqttConnectResult> connectFuture = client.connect(this.mqttNodeConfiguration.getHost(), this.mqttNodeConfiguration.getPort());
        MqttConnectResult result;
        try {
            // 连接建立阶段阻塞等待配置的超时时间，避免节点初始化后留下未连接客户端。
            result = connectFuture.get(this.mqttNodeConfiguration.getConnectTimeoutSec(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = this.mqttNodeConfiguration.getHost() + ":" + this.mqttNodeConfiguration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s.", hostPort));
        }
        if (!result.isSuccess()) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = this.mqttNodeConfiguration.getHost() + ":" + this.mqttNodeConfiguration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s. Result code is: %s", hostPort, result.getReturnCode()));
        }
        return client;
    }

    /**
     * 将基础用户名密码凭据写入 MQTT 客户端配置。
     * 本方法只准备客户端参数，不直接建立连接；证书、SAS 等特殊认证可由子类覆盖。
     */
    protected void prepareMqttClientConfig(MqttClientConfig config) throws SSLException {
        ClientCredentials credentials = this.mqttNodeConfiguration.getCredentials();
        if (credentials.getType() == CredentialsType.BASIC) {
            BasicCredentials basicCredentials = (BasicCredentials) credentials;
            config.setUsername(basicCredentials.getUsername());
            config.setPassword(basicCredentials.getPassword());
        }
    }

    /**
     * 按配置决定是否初始化 SSL 上下文。
     * 本方法本身不直接访问数据库或缓存；证书内容来自节点凭据配置，具体解析由凭据实现完成。
     */
    private SslContext getSslContext() throws SSLException {
        return this.mqttNodeConfiguration.isSsl() ? this.mqttNodeConfiguration.getCredentials().initSslContext() : null;
    }

}

/*
 * 本类总结：
 * 本类是 MQTT 外部集成节点，直接管理 MQTT 客户端连接并以 AT_LEAST_ONCE QoS 发布消息。
 * Topic、连接参数、保留消息和认证来自节点配置；Rule Engine 消息确认在发布前通过基类处理，发布回调决定成功或失败路由。
 * 本类本身不直接涉及数据库或缓存，相关能力只可能通过 Rule Engine 上下文、执行器或调用链间接涉及。
 */
