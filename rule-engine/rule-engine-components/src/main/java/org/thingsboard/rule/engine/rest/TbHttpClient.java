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

@Data
@Slf4j
@SuppressWarnings("deprecation")
/**
 * REST API 节点的 HTTP 客户端封装，负责构造 AsyncRestTemplate、发起异步请求并转换响应。
 * 本类直接触达外部 HTTP 服务；本身不直接访问数据库或缓存，Rule Engine 后续路由通过回调传入。
 */
public class TbHttpClient {

    /**
     * HTTP 响应状态名称写入消息元数据时使用的键名。
     */
    private static final String STATUS = "status";
    /**
     * HTTP 响应状态码写入消息元数据时使用的键名。
     */
    private static final String STATUS_CODE = "statusCode";
    /**
     * HTTP 响应原因短语写入消息元数据时使用的键名。
     */
    private static final String STATUS_REASON = "statusReason";
    /**
     * HTTP 异常写入消息元数据时使用的键名。
     */
    private static final String ERROR = "error";
    /**
     * HTTP 非成功响应体或异常响应体写入消息元数据时使用的键名。
     */
    private static final String ERROR_BODY = "error_body";
    /**
     * 使用系统代理但缺少必要 JVM 属性时的错误说明。
     */
    private static final String ERROR_SYSTEM_PROPERTIES = "Didn't set any system proxy properties. Should be added next system proxy properties: \"http.proxyHost\" and \"http.proxyPort\" or  \"https.proxyHost\" and \"https.proxyPort\" or \"socksProxyHost\" and \"socksProxyPort\"";

    /**
     * REST 节点配置，包含 endpoint、方法、Header、代理、TLS 和并发限制。
     */
    private final TbRestApiCallNodeConfiguration config;

    /**
     * 本客户端独占创建的 Netty 事件循环；使用共享事件循环时为空。
     */
    private EventLoopGroup eventLoopGroup;
    /**
     * 实际执行异步 HTTP 请求的 Spring AsyncRestTemplate。
     */
    private AsyncRestTemplate httpClient;
    /**
     * 用于限制并发请求数量的未完成 future 队列。
     */
    private Deque<ListenableFuture<ResponseEntity<String>>> pendingFutures;

    /**
     * 根据配置创建 HTTP 客户端实现。
     * 代理模式使用 Apache async client，简单模式使用默认 AsyncRestTemplate，默认模式使用 Netty4 客户端和配置凭据。
     * 本构造方法不发起外部 HTTP 请求；数据库/缓存不在本类中直接涉及，凭据解析或系统属性读取可能由调用链间接完成。
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
                        // 系统代理认证使用 JVM 全局 Authenticator，影响范围由底层 JDK HTTP 客户端决定。
                        Authenticator.setDefault(new Authenticator() {
                            /**
                             * 为系统代理认证提供用户名和密码。
                             * 本方法由 JDK 认证流程回调，不直接访问数据库或缓存。
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
     * 选择共享 EventLoopGroup，或在没有共享对象时创建本客户端独占事件循环。
     * 本方法只管理 HTTP 客户端线程资源，不直接发起 REST 请求。
     */
    EventLoopGroup getSharedOrCreateEventLoopGroup(EventLoopGroup eventLoopGroupShared) {
        if (eventLoopGroupShared != null) {
            return eventLoopGroupShared;
        }
        return this.eventLoopGroup = new NioEventLoopGroup();
    }

    /**
     * 校验 JVM 系统代理属性是否足够构造代理连接。
     * 本方法只读取系统属性，不直接访问外部 HTTP 服务、数据库或缓存。
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
     * 判断代理认证用户名和密码是否同时存在。
     * 本方法是纯本地判断，不涉及外部调用。
     */
    private boolean useAuth(String proxyUser, String proxyPassword) {
        return !StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(proxyPassword);
    }

    /**
     * 关闭本客户端独占创建的 Netty 事件循环。
     * 使用共享事件循环时本方法不会关闭共享资源。
     */
    void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * 处理 Rule Engine 消息并发起异步 HTTP 请求。
     * endpoint、Header、HTTP 方法和请求体都由配置模板和当前 TbMsg 解析得到；外部调用边界是 httpClient.exchange。
     * AsyncRestTemplate 回调中 2xx 响应走成功，非 2xx 或异常走失败；本方法本身不直接访问数据库或缓存。
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
        // exchange 返回 ListenableFuture，HTTP 响应在线程池回调中映射到 Rule Engine 成功或失败路由。
        ListenableFuture<ResponseEntity<String>> future = httpClient.exchange(
                uri, method, entity, String.class);
        future.addCallback(new ListenableFutureCallback<>() {
            /**
             * HTTP 客户端异常回调，将异常转换为 Failure 路由消息。
             * 本方法运行在线程池回调中，不直接访问数据库或缓存。
             */
            @Override
            public void onFailure(Throwable throwable) {
                onFailure.accept(processException(msg, throwable), throwable);
            }

            /**
             * HTTP 客户端成功收到响应后的回调。
             * 2xx 响应走 Success，非 2xx 响应转换为 Failure 消息但 Throwable 为空。
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
     * 校验并编码 endpoint URL。
     * 本方法仅解析 URI，不直接发起 HTTP 请求；非法 URL 会同步抛出异常并由调用方失败路由。
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
     * 根据配置决定请求体内容，必要时把 JSON 字符串转换为纯文本。
     * 本方法只处理消息载荷，不涉及外部调用、数据库或缓存。
     */
    private String getData(TbMsg tbMsg, boolean ignoreBody, boolean parseToPlainText) {
        if (!ignoreBody && parseToPlainText) {
            return parseJsonStringToPlainText(tbMsg.getData());
        }
        return tbMsg.getData();
    }

    /**
     * 将 JSON 字符串形式的文本值去掉外层引号。
     * 本方法用于兼容旧 trimDoubleQuotes 行为，不直接触发外部调用。
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
     * 将 2xx HTTP 响应转换为新的 TbMsg。
     * 状态、状态码、原因短语和响应 Header 写入元数据，响应体写入消息体；本方法不直接访问外部系统。
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
     * 将 HTTP Header 列表写入消息元数据。
     * 多值 Header 会序列化为 JSON 字符串；本方法只处理本地数据。
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
     * 将非 2xx HTTP 响应转换为 Failure 路由使用的 TbMsg。
     * 状态和响应体写入元数据；Throwable 可能为空，因为 HTTP 调用本身已成功返回响应。
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
     * 将 HTTP 客户端异常转换为 Failure 路由使用的 TbMsg。
     * RestClientResponseException 会额外携带状态码和响应体；本方法不直接访问数据库或缓存。
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
     * 基于配置模板和消息内容准备 HTTP Header。
     * Basic 凭据会被编码为 Authorization Header；本方法不直接发起 HTTP 请求。
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
     * 对未完成 HTTP future 进行简单并发控制。
     * 当队列超过配置上限时等待并取消较早请求；队列使用 ConcurrentLinkedDeque，适配异步回调并发访问。
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
     * 校验代理主机配置。
     * 本方法只做本地参数校验，不直接访问代理服务器。
     */
    private static void checkProxyHost(String proxyHost) throws TbNodeException {
        if (StringUtils.isEmpty(proxyHost)) {
            throw new TbNodeException("Proxy host can't be empty");
        }
    }

    /**
     * 校验代理端口范围。
     * 本方法只做本地参数校验，不直接访问代理服务器。
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
