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

/**
 * `TbRestApiCallNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TbRestApiCallNodeConfiguration implements NodeConfiguration<TbRestApiCallNodeConfiguration> {

    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    private String restEndpointUrlPattern;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private String requestMethod;
    /**
     * `headers`映射关系，用于按键查找对应值。
     */
    private Map<String, String> headers;
    /**
     * 是否使用客户端。
     */
    private boolean useSimpleClientHttpFactory;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private int readTimeoutMs;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private int maxParallelRequestsCount;
    /**
     * 是否使用队列。
     */
    private boolean useRedisQueueForMsgPersistence;
    /**
     * 是否满足`parseToPlainText`条件。
     */
    private boolean parseToPlainText;
    /**
     * 是否启用`proxy`。
     */
    private boolean enableProxy;
    /**
     * 是否使用Actor 系统。
     */
    private boolean useSystemProxyProperties;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    private String proxyHost;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    private int proxyPort;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    private String proxyUser;
    /**
     * 密码，用于认证或安全校验。
     */
    private String proxyPassword;
    /**
     * `proxyScheme` 字段，保存当前对象的对应属性。
     */
    private String proxyScheme;
    /**
     * 凭据，用于认证或安全校验。
     */
    private ClientCredentials credentials;
    /**
     * 是否忽略对应检查。
     */
    private boolean ignoreRequestBody;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
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
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
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
