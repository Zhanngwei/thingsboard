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
package org.thingsboard.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.thingsboard.rule.engine.mqtt.azure.AzureIotHubSasCredentials;

import javax.net.ssl.SSLException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnonymousCredentials.class, name = "anonymous"),
        @JsonSubTypes.Type(value = BasicCredentials.class, name = "basic"),
        @JsonSubTypes.Type(value = AzureIotHubSasCredentials.class, name = "sas"),
        @JsonSubTypes.Type(value = CertPemCredentials.class, name = "cert.PEM")})
/**
 * 中文说明：`ClientCredentials` 是客户端凭据接口，用于抽象描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料中的可替换行为。
 * 调用边界：接口本身不直接涉及数据库、缓存、Rule Engine、Actor、MQTT 或事务；具体实现或调用链可能涉及。
 */
public interface ClientCredentials {
    @JsonIgnore
    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `ClientCredentials` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    CredentialsType getType();

    @JsonIgnore
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态，供 `ClientCredentials` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    default SslContext initSslContext() throws SSLException{
        return SslContextBuilder.forClient().build();
    }
    /*
     * 本类总结：`ClientCredentials` 负责描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
