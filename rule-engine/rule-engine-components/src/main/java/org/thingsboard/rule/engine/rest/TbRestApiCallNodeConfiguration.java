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
package org.thingsboard.rule.engine.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;

import java.util.Collections;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
/**
 * REST API 调用节点配置模型，保存 endpoint、HTTP 方法、Header、客户端实现、代理、TLS 凭据和请求体策略。
 * 配置类本身不直接发起 HTTP 请求、不注册异步回调，也不涉及数据库或缓存。
 */
public class TbRestApiCallNodeConfiguration implements NodeConfiguration<TbRestApiCallNodeConfiguration> {

    /**
     * REST endpoint URL 模板，运行时结合 TbMsg 解析。
     */
    private String restEndpointUrlPattern;
    /**
     * HTTP 请求方法名称。
     */
    private String requestMethod;
    /**
     * HTTP Header 模板集合，键和值都可基于消息解析。
     */
    private Map<String, String> headers;
    /**
     * 是否使用简单 AsyncRestTemplate 工厂；该模式不支持 CERT PEM 凭据。
     */
    private boolean useSimpleClientHttpFactory;
    /**
     * HTTP 读取超时时间，单位毫秒。
     */
    private int readTimeoutMs;
    /**
     * 最大并发请求数，0 表示不启用本地限制。
     */
    private int maxParallelRequestsCount;
    /**
     * 已废弃的 Redis 持久化配置开关，当前节点初始化仅记录警告。
     */
    private boolean useRedisQueueForMsgPersistence;
    /**
     * 是否将 JSON 字符串请求体解析成纯文本。
     */
    private boolean parseToPlainText;
    /**
     * 是否启用 HTTP 代理。
     */
    private boolean enableProxy;
    /**
     * 是否使用 JVM 系统代理属性。
     */
    private boolean useSystemProxyProperties;
    /**
     * 手动代理主机。
     */
    private String proxyHost;
    /**
     * 手动代理端口。
     */
    private int proxyPort;
    /**
     * 手动代理用户名。
     */
    private String proxyUser;
    /**
     * 手动代理密码。
     */
    private String proxyPassword;
    /**
     * 手动代理协议，例如 http 或 https。
     */
    private String proxyScheme;
    /**
     * REST 客户端认证/TLS 凭据。
     */
    private ClientCredentials credentials;
    /**
     * 是否忽略请求体，即使 HTTP 方法通常允许请求体。
     */
    private boolean ignoreRequestBody;

    /**
     * 构造 REST 节点默认配置。
     * 本方法只设置默认值，不直接调用外部 REST 服务，也不处理 Rule Engine 消息确认。
     */
    @Override
    public TbRestApiCallNodeConfiguration defaultConfiguration() {
        TbRestApiCallNodeConfiguration configuration = new TbRestApiCallNodeConfiguration();
        configuration.setRestEndpointUrlPattern("http://localhost/api");
        configuration.setRequestMethod("POST");
        configuration.setHeaders(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        configuration.setUseSimpleClientHttpFactory(false);
        configuration.setReadTimeoutMs(0);
        configuration.setMaxParallelRequestsCount(0);
        configuration.setUseRedisQueueForMsgPersistence(false);
        configuration.setParseToPlainText(false);
        configuration.setEnableProxy(false);
        configuration.setCredentials(new AnonymousCredentials());
        configuration.setIgnoreRequestBody(false);
        return configuration;
    }

    /**
     * 返回配置凭据，兼容旧配置中 credentials 为空的情况。
     * 本方法只提供默认匿名凭据，不直接建立 HTTP 连接或访问数据库/缓存。
     */
    public ClientCredentials getCredentials() {
        if (this.credentials == null) {
            return new AnonymousCredentials();
        } else {
            return this.credentials;
        }
    }
}

/*
 * 本类总结：
 * 本类描述 REST API 外部调用节点的全部可配置项，包括 URL、方法、Header、代理、TLS 凭据、请求体和并发限制。
 * 实际 HTTP 客户端创建、异步调用、失败路由和资源释放由 TbHttpClient/TbRestApiCallNode 完成。
 * 配置类本身不直接涉及外部调用、数据库、缓存或 Rule Engine Actor 调度。
 */
