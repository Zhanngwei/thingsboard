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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * `TbHttpClient` 类，封装当前模块中的一组相关职责。
 */
@Data
@Slf4j
@SuppressWarnings("deprecation")
public class TbHttpClient {

    /**
     * 状态常量，用于统一引用固定值。
     */
    private static final String STATUS = "status";
    /**
     * 状态常量，用于统一引用固定值。
     */
    private static final String STATUS_CODE = "statusCode";
    /**
     * 状态常量，用于统一引用固定值。
     */
    private static final String STATUS_REASON = "statusReason";
    /**
     * 错误信息常量，用于统一引用固定值。
     */
    private static final String ERROR = "error";
    /**
     * 错误信息常量，用于统一引用固定值。
     */
    private static final String ERROR_BODY = "error_body";
    /**
     * 错误信息常量，用于统一引用固定值。
     */
    private static final String ERROR_SYSTEM_PROPERTIES = "Didn't set any system proxy properties. Should be added next system proxy properties: \"http.proxyHost\" and \"http.proxyPort\" or  \"https.proxyHost\" and \"https.proxyPort\" or \"socksProxyHost\" and \"socksProxyPort\"";

    /**
     * 配置，保存当前对象的配置选项。
     */
    private final TbRestApiCallNodeConfiguration config;

    /**
     * 事件循环，用于支撑当前网络或外部服务交互。
     */
    private EventLoopGroup eventLoopGroup;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private AsyncRestTemplate httpClient;
    /**
     * `pendingFutures`列表，用于保存一组待处理对象。
     */
    private Deque<ListenableFuture<ResponseEntity<String>>> pendingFutures;

    /**
     * 功能：创建 `TbHttpClient` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * - `eventLoopGroupShared`：`eventLoopGroupShared` 参数。
     * 返回：新创建的对象实例。
     */
    TbHttpClient(TbRestApiCallNodeConfiguration config, EventLoopGroup eventLoopGroupShared) throws TbNodeException {
        try {
            this.config = config;
            if (config.getMaxParallelRequestsCount() > 0) {
                pendingFutures = new ConcurrentLinkedDeque<>();
            }

            if (config.isEnableProxy()) {
                checkProxyHost(config.getProxyHost());
                checkProxyPort(config.getProxyPort());

                String proxyUser;
                String proxyPassword;

                CloseableHttpAsyncClient asyncClient;
                HttpComponentsAsyncClientHttpRequestFactory requestFactory = new HttpComponentsAsyncClientHttpRequestFactory();

                if (config.isUseSystemProxyProperties()) {
                    checkSystemProxyProperties();

                    asyncClient = HttpAsyncClients.createSystem();

                    proxyUser = System.getProperty("tb.proxy.user");
                    proxyPassword = System.getProperty("tb.proxy.password");

                    if (useAuth(proxyUser, proxyPassword)) {
                        Authenticator.setDefault(new Authenticator() {
                            /**
                             * 功能：获取密码。
                             * 参数：无。
                             * 返回：处理结果。
                             */
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                            }
                        });
                    }
                } else {
                    HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClientBuilder.create()
                            .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                            .setSSLContext(SSLContext.getDefault())
                            .setProxy(new HttpHost(config.getProxyHost(), config.getProxyPort(), config.getProxyScheme()));

                    proxyUser = config.getProxyUser();
                    proxyPassword = config.getProxyPassword();

                    if (useAuth(proxyUser, proxyPassword)) {
                        CredentialsProvider credsProvider = new BasicCredentialsProvider();
                        credsProvider.setCredentials(
                                new AuthScope(config.getProxyHost(), config.getProxyPort()),
                                new UsernamePasswordCredentials(proxyUser, proxyPassword)
                        );
                        httpAsyncClientBuilder.setDefaultCredentialsProvider(credsProvider);
                    }
                    asyncClient = httpAsyncClientBuilder.build();
                }

                requestFactory.setAsyncClient(asyncClient);
                requestFactory.setReadTimeout(config.getReadTimeoutMs());
                httpClient = new AsyncRestTemplate(requestFactory);
            } else if (config.isUseSimpleClientHttpFactory()) {
                if (CredentialsType.CERT_PEM == config.getCredentials().getType()) {
                    throw new TbNodeException("Simple HTTP Factory does not support CERT PEM credentials!");
                }
                httpClient = new AsyncRestTemplate();
            } else {
                Netty4ClientHttpRequestFactory nettyFactory = new Netty4ClientHttpRequestFactory(getSharedOrCreateEventLoopGroup(eventLoopGroupShared));
                nettyFactory.setSslContext(config.getCredentials().initSslContext());
                nettyFactory.setReadTimeout(config.getReadTimeoutMs());
                httpClient = new AsyncRestTemplate(nettyFactory);
            }
        } catch (SSLException | NoSuchAlgorithmException e) {
            throw new TbNodeException(e);
        }
    }

    /**
     * 功能：获取事件循环。
     * 参数：
     * - `eventLoopGroupShared`：`eventLoopGroupShared` 参数。
     * 返回：处理结果。
     */
    EventLoopGroup getSharedOrCreateEventLoopGroup(EventLoopGroup eventLoopGroupShared) {
        if (eventLoopGroupShared != null) {
            return eventLoopGroupShared;
        }
        return this.eventLoopGroup = new NioEventLoopGroup();
    }

    /**
     * 功能：校验Actor 系统。
     * 参数：无。
     * 返回：无。
     */
    private void checkSystemProxyProperties() throws TbNodeException {
        boolean useHttpProxy = !StringUtils.isEmpty(System.getProperty("http.proxyHost")) && !StringUtils.isEmpty(System.getProperty("http.proxyPort"));
        boolean useHttpsProxy = !StringUtils.isEmpty(System.getProperty("https.proxyHost")) && !StringUtils.isEmpty(System.getProperty("https.proxyPort"));
        boolean useSocksProxy = !StringUtils.isEmpty(System.getProperty("socksProxyHost")) && !StringUtils.isEmpty(System.getProperty("socksProxyPort"));
        if (!(useHttpProxy || useHttpsProxy || useSocksProxy)) {
            log.warn(ERROR_SYSTEM_PROPERTIES);
            throw new TbNodeException(ERROR_SYSTEM_PROPERTIES);
        }
    }

    /**
     * 功能：执行 `useAuth` 对应的处理。
     * 参数：
     * - `proxyUser`：`proxyUser` 参数。
     * - `proxyPassword`：`proxyPassword` 参数。
     * 返回：判断结果。
     */
    private boolean useAuth(String proxyUser, String proxyPassword) {
        return !StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(proxyPassword);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * 返回：无。
     */
    public void processMessage(TbContext ctx, TbMsg msg,
                               Consumer<TbMsg> onSuccess,
                               BiConsumer<TbMsg, Throwable> onFailure) {
        String endpointUrl = TbNodeUtils.processPattern(config.getRestEndpointUrlPattern(), msg);
        HttpHeaders headers = prepareHeaders(msg);
        HttpMethod method = HttpMethod.valueOf(config.getRequestMethod());
        HttpEntity<String> entity;
        if (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) ||
                HttpMethod.OPTIONS.equals(method) || HttpMethod.TRACE.equals(method) ||
                config.isIgnoreRequestBody()) {
            entity = new HttpEntity<>(headers);
        } else {
            entity = new HttpEntity<>(getData(msg, config.isIgnoreRequestBody(), config.isParseToPlainText()), headers);
        }

        URI uri = buildEncodedUri(endpointUrl);
        ListenableFuture<ResponseEntity<String>> future = httpClient.exchange(
                uri, method, entity, String.class);
        future.addCallback(new ListenableFutureCallback<>() {
            /**
             * 功能：处理失败信息。
             * 参数：
             * - `throwable`：`throwable` 参数。
             * 返回：无。
             */
            @Override
            public void onFailure(Throwable throwable) {
                onFailure.accept(processException(msg, throwable), throwable);
            }

            /**
             * 功能：处理`on Success`。
             * 参数：
             * - `responseEntity`：响应对象。
             * 返回：无。
             */
            @Override
            public void onSuccess(ResponseEntity<String> responseEntity) {
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    onSuccess.accept(processResponse(ctx, msg, responseEntity));
                } else {
                    onFailure.accept(processFailureResponse(msg, responseEntity), null);
                }
            }
        });
        if (pendingFutures != null) {
            processParallelRequests(future);
        }
    }

    /**
     * 功能：构建URI 地址。
     * 参数：
     * - `endpointUrl`：`endpointUrl` 参数。
     * 返回：处理结果。
     */
    public URI buildEncodedUri(String endpointUrl) {
        if (endpointUrl == null) {
            throw new RuntimeException("Url string cannot be null!");
        }
        if (endpointUrl.isEmpty()) {
            throw new RuntimeException("Url string cannot be empty!");
        }

        URI uri = UriComponentsBuilder.fromUriString(endpointUrl).build().encode().toUri();
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            throw new RuntimeException("Transport scheme(protocol) must be provided!");
        }

        boolean authorityNotValid = uri.getAuthority() == null || uri.getAuthority().isEmpty();
        boolean hostNotValid = uri.getHost() == null || uri.getHost().isEmpty();
        if (authorityNotValid || hostNotValid) {
            throw new RuntimeException("Url string is invalid!");
        }

        return uri;
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `ignoreBody`：`ignoreBody` 参数。
     * - `parseToPlainText`：`parseToPlainText` 参数。
     * 返回：文本结果。
     */
    private String getData(TbMsg tbMsg, boolean ignoreBody, boolean parseToPlainText) {
        if (!ignoreBody && parseToPlainText) {
            return parseJsonStringToPlainText(tbMsg.getData());
        }
        return tbMsg.getData();
    }

    /**
     * 功能：解析JSON。
     * 参数：
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    protected String parseJsonStringToPlainText(String data) {
        if (data.startsWith("\"") && data.endsWith("\"") && data.length() >= 2) {
            final String dataBefore = data;
            try {
                data = JacksonUtil.fromString(data, String.class);
            } catch (Exception ignored) {}
            log.trace("Trimming double quotes. Before trim: [{}], after trim: [{}]", dataBefore, data);
        }

        return data;
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `ctx`：处理上下文。
     * - `origMsg`：待处理消息。
     * - `response`：响应对象。
     * 返回：处理结果。
     */
    private TbMsg processResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        String body = response.getBody() == null ? TbMsg.EMPTY_JSON_OBJECT : response.getBody();
        return ctx.transformMsg(origMsg, metaData, body);
    }

    /**
     * 功能：执行 `headersToMetaData` 对应的处理。
     * 参数：
     * - `headers`：数据列表。
     * - `consumer`：`consumer` 参数。
     * 返回：无。
     */
    void headersToMetaData(Map<String, List<String>> headers, BiConsumer<String, String> consumer) {
        if (headers == null) {
            return;
        }
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                if (values.size() == 1) {
                    consumer.accept(key, values.get(0));
                } else {
                    consumer.accept(key, JacksonUtil.toString(values));
                }
            }
        });
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `response`：响应对象。
     * 返回：处理结果。
     */
    private TbMsg processFailureResponse(TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        metaData.putValue(ERROR_BODY, response.getBody());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 功能：处理`Exception`。
     * 参数：
     * - `origMsg`：待处理消息。
     * - `e`：`e` 参数。
     * 返回：处理结果。
     */
    private TbMsg processException(TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        if (e instanceof RestClientResponseException) {
            RestClientResponseException restClientResponseException = (RestClientResponseException) e;
            metaData.putValue(STATUS, restClientResponseException.getStatusText());
            metaData.putValue(STATUS_CODE, restClientResponseException.getRawStatusCode() + "");
            metaData.putValue(ERROR_BODY, restClientResponseException.getResponseBodyAsString());
        }
        return TbMsg.transformMsgMetadata(origMsg, metaData);
    }

    /**
     * 功能：执行 `prepareHeaders` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private HttpHeaders prepareHeaders(TbMsg msg) {
        HttpHeaders headers = new HttpHeaders();
        config.getHeaders().forEach((k, v) -> headers.add(TbNodeUtils.processPattern(k, msg), TbNodeUtils.processPattern(v, msg)));
        ClientCredentials credentials = config.getCredentials();
        if (CredentialsType.BASIC == credentials.getType()) {
            BasicCredentials basicCredentials = (BasicCredentials) credentials;
            String authString = basicCredentials.getUsername() + ":" + basicCredentials.getPassword();
            String encodedAuthString = new String(Base64.encodeBase64(authString.getBytes(StandardCharsets.UTF_8)));
            headers.add("Authorization", "Basic " + encodedAuthString);
        }
        return headers;
    }

    /**
     * 功能：处理`Parallel Requests`。
     * 参数：
     * - `future`：数据列表。
     * 返回：无。
     */
    private void processParallelRequests(ListenableFuture<ResponseEntity<String>> future) {
        pendingFutures.add(future);
        if (pendingFutures.size() > config.getMaxParallelRequestsCount()) {
            for (int i = 0; i < config.getMaxParallelRequestsCount(); i++) {
                try {
                    ListenableFuture<ResponseEntity<String>> pendingFuture = pendingFutures.removeFirst();
                    try {
                        pendingFuture.get(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.warn("Timeout during waiting for reply!", e);
                        pendingFuture.cancel(true);
                    }
                } catch (Exception e) {
                    log.warn("Failure during waiting for reply!", e);
                }
            }
        }
    }

    /**
     * 功能：校验主机地址。
     * 参数：
     * - `proxyHost`：`proxyHost` 参数。
     * 返回：无。
     */
    private static void checkProxyHost(String proxyHost) throws TbNodeException {
        if (StringUtils.isEmpty(proxyHost)) {
            throw new TbNodeException("Proxy host can't be empty");
        }
    }

    /**
     * 功能：校验端口号。
     * 参数：
     * - `proxyPort`：`proxyPort` 参数。
     * 返回：无。
     */
    private static void checkProxyPort(int proxyPort) throws TbNodeException {
        if (proxyPort < 0 || proxyPort > 65535) {
            throw new TbNodeException("Proxy port out of range:" + proxyPort);
        }
    }

}

/*
 * 本类总结：
 * 本类封装 REST 外部调用的 HTTP 客户端生命周期、请求构造、异步回调、失败元数据和并发限制。
 * 它直接调用外部 HTTP 服务，但不直接访问数据库或缓存；数据库/缓存只可能在 Rule Engine 上下文、凭据实现或调用链内部间接涉及。
 * 成功和失败路由由调用方传入的回调完成，线程安全依赖 AsyncRestTemplate、Netty/Apache 客户端和并发队列实现。
 */
