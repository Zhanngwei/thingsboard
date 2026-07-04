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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.thingsboard.common.util.AzureIotHubUtil;
import org.thingsboard.rule.engine.credentials.CertPemCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;

import java.security.Security;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Azure IoT Hub SAS 凭据模型，继承 PEM 证书凭据并补充 SAS Key。
 * 本类只负责凭据和 TLS 上下文构造，不直接发布 MQTT 消息、不管理 Rule Engine 消息确认。
 */
public class AzureIotHubSasCredentials extends CertPemCredentials {
    /**
     * 用于生成 Azure IoT Hub SAS Token 的共享访问密钥。
     */
    private String sasKey;

    /**
     * 初始化 Azure IoT Hub 所需的客户端 SSL 上下文。
     * 本方法本身不直接连接 MQTT Broker；调用方会把返回的 SslContext 写入 MQTT 客户端配置。
     * 证书默认值来自 Azure 工具类，不直接访问数据库或缓存。
     */
    @Override
    public SslContext initSslContext() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            if (caCert == null || caCert.isEmpty()) {
                caCert = AzureIotHubUtil.getDefaultCaCert();
            }
            return SslContextBuilder.forClient()
                    .trustManager(createAndInitTrustManagerFactory())
                    .clientAuth(ClientAuth.REQUIRE)
                    .build();
        } catch (Exception e) {
            log.error("[{}] Creating TLS factory failed!", caCert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    /**
     * 返回 SAS 凭据类型，供 MQTT/Azure 节点选择认证分支。
     * 本方法不直接涉及外部调用、线程调度或 Rule Engine 消息路由。
     */
    @Override
    public CredentialsType getType() {
        return CredentialsType.SAS;
    }

}

/*
 * 本类总结：
 * 本类描述 Azure IoT Hub 的 SAS 认证凭据，并可生成 MQTT TLS 连接所需的 SslContext。
 * 它不直接使用 MQTT 客户端，不参与连接生命周期、QoS、Topic 解析、消息确认、数据库或缓存操作。
 */
