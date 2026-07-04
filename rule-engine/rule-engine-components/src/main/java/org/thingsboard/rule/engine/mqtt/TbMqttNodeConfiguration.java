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

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;

@Data
/**
 * MQTT 节点配置模型，保存 Broker 地址、Topic 模板、客户端会话、SSL 和认证信息。
 * 配置类本身不直接使用 MQTT 客户端，也不直接涉及数据库、缓存、Actor 或 Rule Engine 消息确认。
 */
public class TbMqttNodeConfiguration implements NodeConfiguration<TbMqttNodeConfiguration> {

    /**
     * MQTT Topic 模板，运行时由节点结合 TbMsg 元数据/数据解析得到最终 Topic。
     */
    private String topicPattern;
    /**
     * MQTT Broker 主机名或地址。
     */
    private String host;
    /**
     * MQTT Broker 端口。
     */
    private int port;
    /**
     * MQTT 连接初始化时等待 CONNACK 的超时时间，单位为秒。
     */
    private int connectTimeoutSec;
    /**
     * MQTT clientId，可为空以使用客户端默认值。
     */
    private String clientId;
    /**
     * 是否在 clientId 后追加服务实例后缀，避免集群中客户端 ID 冲突。
     */
    private boolean appendClientIdSuffix;
    /**
     * MQTT retained 标志，发布时由节点直接传给 MQTT 客户端。
     */
    private boolean retainedMessage;

    /**
     * MQTT cleanSession 标志，控制连接会话生命周期。
     */
    private boolean cleanSession;
    /**
     * 是否启用 SSL/TLS 连接。
     */
    private boolean ssl;
    /**
     * MQTT 客户端凭据，可能是匿名、基础用户名密码或证书类凭据。
     */
    private ClientCredentials credentials;

    /**
     * 构造 MQTT 节点的默认配置。
     * 本方法只填充配置默认值，不直接创建 MQTT 客户端、不访问外部 Broker，也不处理 Rule Engine 消息确认。
     */
    @Override
    public TbMqttNodeConfiguration defaultConfiguration() {
        TbMqttNodeConfiguration configuration = new TbMqttNodeConfiguration();
        configuration.setTopicPattern("my-topic");
        configuration.setPort(1883);
        configuration.setConnectTimeoutSec(10);
        configuration.setCleanSession(true);
        configuration.setSsl(false);
        configuration.setRetainedMessage(false);
        configuration.setCredentials(new AnonymousCredentials());
        return configuration;
    }

}

/*
 * 本类总结：
 * 本类集中描述 MQTT 节点连接和发布所需的可配置项，Topic、retained、连接超时、会话和凭据都会被 TbMqttNode 消费。
 * 配置类本身没有外部调用、异步回调、数据库或缓存逻辑。
 */
