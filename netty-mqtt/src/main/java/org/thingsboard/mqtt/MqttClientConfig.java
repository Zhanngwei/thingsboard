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
package org.thingsboard.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslContext;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * 中文说明：
 * 1. `MqttClientConfig` 是 ThingsBoard Netty MQTT Client 中描述 MQTT 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MqttClientConfig {
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final SslContext sslContext;
    private final String randomClientId;

    /**
     * `ownerId`ID，用于定位对应业务对象。
     */
    @Getter
    @Setter
    private String ownerId; // [TenantId][IntegrationId] or [TenantId][RuleNodeId] for exceptions logging purposes
    private String clientId;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private int timeoutSeconds = 60;
    private MqttVersion protocolVersion = MqttVersion.MQTT_3_1;
    /**
     * 是否满足会话条件。
     */
    @Nullable private String username = null;
    @Nullable private String password = null;
    private boolean cleanSession = true;
    /**
     * Netty Channel 类型，表示当前网络连接使用的通道。
     */
    @Nullable private MqttLastWill lastWill;
    private Class<? extends Channel> channelClass = NioSocketChannel.class;

    /**
     * 是否满足`reconnect`条件。
     */
    private boolean reconnect = true;
    private long reconnectDelay = 1L;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private int maxBytesInMessage = 8092;

    /**
     * 功能：创建 `MqttClientConfig` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public MqttClientConfig() {
        this(null);
    }

    /**
     * 功能：创建 `MqttClientConfig` 实例，并初始化必要字段。
     * 参数：
     * - `sslContext`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public MqttClientConfig(SslContext sslContext) {
        this.sslContext = sslContext;
        Random random = new Random();
        String id = "netty-mqtt/";
        String[] options = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split("");
        for(int i = 0; i < 8; i++){
            id += options[random.nextInt(options.length)];
        }
        this.clientId = id;
        this.randomClientId = id;
    }

    /**
     * 功能：获取客户端。
     * 参数：无。
     * 返回：文本结果。
     */
    @Nonnull
    public String getClientId() {
        return clientId;
    }

    /**
     * 功能：更新客户端。
     * 参数：
     * - `clientId`：客户端ID。
     * 返回：无。
     */
    public void setClientId(@Nullable String clientId) {
        if(clientId == null){
            this.clientId = randomClientId;
        }else{
            this.clientId = clientId;
        }
    }

    /**
     * 功能：获取超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 功能：更新超时时间。
     * 参数：
     * - `timeoutSeconds`：`timeoutSeconds` 参数。
     * 返回：无。
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        if(timeoutSeconds != -1 && timeoutSeconds <= 0){
            throw new IllegalArgumentException("timeoutSeconds must be > 0 or -1");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 功能：获取版本号。
     * 参数：无。
     * 返回：处理结果。
     */
    public MqttVersion getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * 功能：更新版本号。
     * 参数：
     * - `protocolVersion`：`protocolVersion` 参数。
     * 返回：无。
     */
    public void setProtocolVersion(MqttVersion protocolVersion) {
        if(protocolVersion == null){
            throw new NullPointerException("protocolVersion");
        }
        this.protocolVersion = protocolVersion;
    }

    /**
     * 功能：获取用户名。
     * 参数：无。
     * 返回：文本结果。
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * 功能：更新用户名。
     * 参数：
     * - `username`：名称。
     * 返回：无。
     */
    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    /**
     * 功能：获取密码。
     * 参数：无。
     * 返回：文本结果。
     */
    @Nullable
    public String getPassword() {
        return password;
    }

    /**
     * 功能：更新密码。
     * 参数：
     * - `password`：`password` 参数。
     * 返回：无。
     */
    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    /**
     * 功能：判断会话。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isCleanSession() {
        return cleanSession;
    }

    /**
     * 功能：更新会话。
     * 参数：
     * - `cleanSession`：会话对象。
     * 返回：无。
     */
    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    /**
     * 功能：获取MQTT 遗嘱消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Nullable
    public MqttLastWill getLastWill() {
        return lastWill;
    }

    /**
     * 功能：更新MQTT 遗嘱消息。
     * 参数：
     * - `lastWill`：`lastWill` 参数。
     * 返回：无。
     */
    public void setLastWill(@Nullable MqttLastWill lastWill) {
        this.lastWill = lastWill;
    }

    /**
     * 功能：获取Netty Channel 类型。
     * 参数：无。
     * 返回：处理结果。
     */
    public Class<? extends Channel> getChannelClass() {
        return channelClass;
    }

    /**
     * 功能：更新Netty Channel 类型。
     * 参数：
     * - `channelClass`：网络通道。
     * 返回：无。
     */
    public void setChannelClass(Class<? extends Channel> channelClass) {
        this.channelClass = channelClass;
    }

    /**
     * 功能：获取上下文。
     * 参数：无。
     * 返回：处理结果。
     */
    public SslContext getSslContext() {
        return sslContext;
    }

    /**
     * 功能：判断`Reconnect`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isReconnect() {
        return reconnect;
    }

    /**
     * 功能：更新`Reconnect`。
     * 参数：
     * - `reconnect`：`reconnect` 参数。
     * 返回：无。
     */
    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    /**
     * 功能：获取延迟时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getReconnectDelay() {
        return reconnectDelay;
    }

    /**
     * Sets the reconnect delay in seconds. Defaults to 1 second.
     * @param reconnectDelay
     * @throws IllegalArgumentException if reconnectDelay is smaller than 1.
     */
    /**
     * 功能：更新延迟时间。
     * 参数：
     * - `reconnectDelay`：`reconnectDelay` 参数。
     * 返回：无。
     */
    public void setReconnectDelay(long reconnectDelay) {
        if (reconnectDelay <= 0) {
            throw new IllegalArgumentException("reconnectDelay must be > 0");
        }
        this.reconnectDelay = reconnectDelay;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getMaxBytesInMessage() {
        return maxBytesInMessage;
    }

    /**
     * Sets the maximum number of bytes in the message for the {@link io.netty.handler.codec.mqtt.MqttDecoder}.
     * Default value is 8092 as specified by Netty. The absolute maximum size is 256MB as set by the MQTT spec.
     *
     * @param maxBytesInMessage
     * @throws IllegalArgumentException if maxBytesInMessage is smaller than 1 or greater than 256_000_000.
     */
    /**
     * 功能：更新消息。
     * 参数：
     * - `maxBytesInMessage`：待处理消息。
     * 返回：无。
     */
    public void setMaxBytesInMessage(int maxBytesInMessage) {
        if (maxBytesInMessage <= 0 || maxBytesInMessage > 256_000_000) {
            throw new IllegalArgumentException("maxBytesInMessage must be > 0 or < 256_000_000");
        }
        this.maxBytesInMessage = maxBytesInMessage;
    }
}
