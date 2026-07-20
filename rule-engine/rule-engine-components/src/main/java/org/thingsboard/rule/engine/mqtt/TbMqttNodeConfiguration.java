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

/**
 * `TbMqttNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbMqttNodeConfiguration implements NodeConfiguration<TbMqttNodeConfiguration> {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private String topicPattern;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    private String host;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    private int port;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private int connectTimeoutSec;
    /**
     * 客户端ID，用于定位对应业务对象。
     */
    private String clientId;
    /**
     * 是否满足客户端条件。
     */
    private boolean appendClientIdSuffix;
    /**
     * 是否满足消息条件。
     */
    private boolean retainedMessage;

    /**
     * 是否满足会话条件。
     */
    private boolean cleanSession;
    /**
     * 是否启用 SSL。
     */
    private boolean ssl;
    /**
     * 凭据，用于认证或安全校验。
     */
    private ClientCredentials credentials;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
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
